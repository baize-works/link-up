package com.baize.flux.framework.execution;

import com.baize.flux.api.dirtydata.DirtyDataSummary;
import com.baize.flux.api.sink.CommitScope;
import com.baize.flux.framework.job.CommitSummary;
import com.baize.flux.framework.job.JobResult;
import com.baize.flux.framework.job.JobStatus;
import com.baize.flux.framework.job.PipelineResult;
import com.baize.flux.framework.metrics.JobMetrics;
import com.baize.flux.framework.planner.ExecutionPlan;
import com.baize.flux.framework.planner.PipelinePlan;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Coordinates pipelines by completion order and retains every submitted outcome.
 */
public final class JobExecution {

    private static final Logger LOG =
            LogManager.getLogger(
                    JobExecution.class);

    private final ExecutionPlan executionPlan;
    private final ClassLoader classLoader;
    private final CancellationToken cancellationToken =
            new CancellationToken();
    private final JobMetrics jobMetrics =
            new JobMetrics();

    public JobExecution(
            ExecutionPlan executionPlan,
            ClassLoader classLoader) {

        this.executionPlan =
                Objects.requireNonNull(
                        executionPlan,
                        "executionPlan");

        this.classLoader =
                Objects.requireNonNull(
                        classLoader,
                        "classLoader");
    }

    public JobResult execute() {
        final long start =
                System.currentTimeMillis();

        final String jobName =
                executionPlan.getJobName();

        final String jobId =
                JobLogFileName.createJobId(
                        jobName,
                        start);

        final String jobLogFile =
                JobLogFileName.create(
                        jobName,
                        start);

        try (CloseableThreadContext.Instance ignored =
                     openJobLogContext(
                             jobId,
                             jobName,
                             jobLogFile)) {

            LOG.info(
                    "Job started: jobName={}, jobLogFile={}",
                    jobName,
                    jobLogFile);

            JobResult result =
                    executeInternal(
                            start,
                            jobId,
                            jobLogFile);

            if (result.getStatus()
                    == JobStatus.SUCCEEDED) {

                LOG.info(
                        "Job finished: status={}, durationMillis={}",
                        result.getStatus(),
                        result.getDurationMillis());
            } else if (result.getFailure() != null) {
                LOG.error(
                        "Job finished: status={}, durationMillis={}",
                        result.getStatus(),
                        result.getDurationMillis(),
                        result.getFailure());
            } else {
                LOG.warn(
                        "Job finished: status={}, durationMillis={}",
                        result.getStatus(),
                        result.getDurationMillis());
            }

            return result;
        }
    }

    private JobResult executeInternal(
            long start,
            final String jobId,
            final String jobLogFile) {

        List<PipelineResult> results =
                new ArrayList<PipelineResult>();

        Throwable first = null;

        if (!executionPlan.isEmpty()) {
            int threadCount =
                    Math.min(
                            executionPlan
                                    .getExecutionConfig()
                                    .getPipelineParallelism(),
                            executionPlan
                                    .getPipelinePlans()
                                    .size());

            ExecutorService pool =
                    Executors.newFixedThreadPool(
                            threadCount);

            CompletionService<PipelineResult> completionService =
                    new ExecutorCompletionService<PipelineResult>(
                            pool);

            List<Future<PipelineResult>> submitted =
                    new ArrayList<Future<PipelineResult>>();

            try {
                for (final PipelinePlan pipelinePlan :
                        executionPlan.getPipelinePlans()) {

                    submitted.add(
                            completionService.submit(
                                    new Callable<PipelineResult>() {
                                        public PipelineResult call() {
                                            try (CloseableThreadContext.Instance ignored =
                                                         openJobLogContext(
                                                                 jobId,
                                                                 executionPlan.getJobName(),
                                                                 jobLogFile)) {

                                                return new PipelineExecution(
                                                        pipelinePlan,
                                                        executionPlan
                                                                .getExecutionConfig(),
                                                        cancellationToken,
                                                        jobMetrics,
                                                        classLoader,
                                                        executionPlan
                                                                .getJobName(),
                                                        start)
                                                        .execute();
                                            }
                                        }
                                    }));
                }

                for (int index = 0;
                     index < submitted.size();
                     index++) {

                    try {
                        PipelineResult result =
                                completionService
                                        .take()
                                        .get();

                        results.add(result);

                        if (result.getFailure() != null
                                && first == null) {

                            first = result.getFailure();
                            cancellationToken.cancel(first);
                        }
                    } catch (Exception exception) {
                        Throwable failure =
                                exception instanceof ExecutionException
                                        && exception.getCause() != null
                                        ? exception.getCause()
                                        : exception;

                        if (first == null) {
                            first = failure;
                            cancellationToken.cancel(failure);
                        }
                    }
                }
            } finally {
                pool.shutdownNow();
            }
        }

        CommitSummary summary =
                merge(results);

        JobStatus status =
                first != null
                        ? JobStatus.FAILED
                        : cancellationToken.isCancelled()
                        ? JobStatus.CANCELED
                        : JobStatus.SUCCEEDED;

        return new JobResult(
                executionPlan.getJobName(),
                status,
                start,
                System.currentTimeMillis(),
                jobMetrics,
                first,
                summary,
                DirtyDataSummary.empty(),
                results);
    }

    private static CloseableThreadContext.Instance
    openJobLogContext(
            String jobId,
            String jobName,
            String jobLogFile) {

        return CloseableThreadContext
                .put(
                        "jobId",
                        jobId)
                .put(
                        "jobName",
                        jobName)
                .put(
                        "jobLogFile",
                        jobLogFile);
    }

    private CommitSummary merge(
            List<PipelineResult> results) {

        int total = 0;
        int finished = 0;
        int committed = 0;
        int empty = 0;
        int failed = 0;
        long attempted = 0L;
        long written = 0L;
        long committedRows = 0L;
        long failedRows = 0L;
        long unknown = 0L;

        for (PipelineResult result : results) {
            CommitSummary summary =
                    result.getCommitSummary();

            total += summary.getTotalTaskCount();
            finished += summary.getFinishedTaskCount();
            committed += summary.getCommittedTaskCount();
            empty += summary.getEmptyCommittedTaskCount();
            failed += summary.getFailedOrUncommittedTaskCount();
            attempted += summary.getAttemptedRecordCount();
            written += summary.getSuccessfullyWrittenRecordCount();
            committedRows += summary.getSuccessfullyCommittedRecordCount();
            failedRows += summary.getFailedRecordCount();
            unknown += summary.getUnknownStateRecordCount();
        }

        return new CommitSummary(
                total,
                finished,
                committed,
                empty,
                failed,
                attempted,
                written,
                committedRows,
                failedRows,
                unknown,
                CommitScope.TASK_LOCAL,
                "This sink commits per task; inspect unknown batch states before retrying.");
    }

    public void cancel() {
        cancellationToken.cancel(
                new java.util.concurrent.CancellationException(
                        "Job was cancelled by caller"));
    }

    /**
     * 返回当前 Job 的实时指标。
     *
     * <p>JobMetrics 本身是线程安全的，调用方应将其转换为只读快照，
     * 不应直接暴露给 HTTP 客户端。
     */
    public JobMetrics getMetrics() {
        return jobMetrics;
    }

    /**
     * 当前 Job 是否已经收到取消请求。
     */
    public boolean isCancellationRequested() {
        return cancellationToken.isCancelled();
    }
}
