package com.link.up.server.runtime;

import com.link.up.api.sink.TableDdl;
import com.link.up.framework.execution.JobExecution;
import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.JobResult;
import com.link.up.framework.job.PipelineResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * Server 内部使用的可变作业执行句柄。
 *
 * <p>所有状态转换均通过 synchronized 方法完成，
 * 避免提交、入队、开始执行、取消和完成之间发生状态覆盖。
 */
final class JobExecutionHandle {

    enum CancelResult {
        REQUESTED,
        ALREADY_REQUESTED,
        CANCELLED_BEFORE_START
    }

    private final String jobId;
    private final JobSubmission submission;
    private final long createTimeMillis;
    private final List<JobStateTransition> transitions =
            new ArrayList<JobStateTransition>();

    private volatile long submittedTimeMillis;
    private volatile long queuedTimeMillis;
    private volatile long startTimeMillis;
    private volatile long endTimeMillis;
    private volatile long stateVersion;

    private volatile ServerJobStatus status =
            ServerJobStatus.CREATED;

    private volatile boolean cancellationRequested;
    private volatile JobExecution execution;
    private volatile Future<?> future;
    private volatile JobResult result;
    private volatile Throwable failure;
    private volatile String runId;
    private volatile String jobLogFile;

    JobExecutionHandle(
            String jobId,
            JobSubmission submission) {

        this.jobId =
                Objects.requireNonNull(
                        jobId,
                        "jobId must not be null");

        this.submission =
                Objects.requireNonNull(
                        submission,
                        "submission must not be null");

        this.createTimeMillis =
                System.currentTimeMillis();

        transitions.add(
                new JobStateTransition(
                        stateVersion,
                        null,
                        ServerJobStatus.CREATED,
                        createTimeMillis,
                        "job-created"));
    }

    synchronized void markSubmitted() {
        submittedTimeMillis =
                System.currentTimeMillis();

        transition(
                ServerJobStatus.SUBMITTED,
                "submission-accepted");
    }

    synchronized void markQueued() {
        queuedTimeMillis =
                System.currentTimeMillis();

        transition(
                ServerJobStatus.QUEUED,
                "worker-queue-accepted");
    }

    synchronized void bindFuture(Future<?> future) {
        this.future =
                Objects.requireNonNull(
                        future,
                        "future must not be null");

        if (cancellationRequested) {
            future.cancel(true);
        }
    }

    synchronized boolean markRunning() {
        if (status != ServerJobStatus.QUEUED
                || cancellationRequested) {
            return false;
        }

        startTimeMillis =
                System.currentTimeMillis();

        transition(
                ServerJobStatus.RUNNING,
                "execution-started");

        return true;
    }

    synchronized void bindExecution(
            JobExecution execution) {

        this.execution =
                Objects.requireNonNull(
                        execution,
                        "execution must not be null");

        this.runId = execution.getRunId();
        this.jobLogFile =
                execution.getJobLogFile();

        if (cancellationRequested) {
            execution.cancel();
        }
    }

    synchronized CancelResult requestCancel() {
        if (status.isTerminal()) {
            throw new JobStateConflictException(
                    jobId,
                    status);
        }

        if (cancellationRequested) {
            return CancelResult.ALREADY_REQUESTED;
        }

        cancellationRequested = true;

        boolean beforeStart =
                status != ServerJobStatus.RUNNING;

        JobExecution currentExecution =
                execution;

        if (currentExecution != null) {
            currentExecution.cancel();
        }

        Future<?> currentFuture = future;

        boolean futureCancelled =
                currentFuture != null
                        && currentFuture.cancel(true);

        if (beforeStart && futureCancelled) {
            return CancelResult.CANCELLED_BEFORE_START;
        }

        return CancelResult.REQUESTED;
    }

    synchronized boolean complete(
            ServerJobStatus finalStatus,
            JobResult result,
            Throwable failure) {

        if (!finalStatus.isTerminal()) {
            throw new IllegalArgumentException(
                    "finalStatus must be terminal");
        }

        if (status.isTerminal()) {
            return false;
        }

        this.result = result;
        this.failure = failure;
        this.endTimeMillis =
                System.currentTimeMillis();

        transition(
                finalStatus,
                terminalReason(
                        finalStatus,
                        failure));

        execution = null;
        future = null;

        return true;
    }

    private void transition(
            ServerJobStatus target,
            String reason) {

        ServerJobStatus previous = status;

        JobStateMachine.requireTransition(
                previous,
                target);

        stateVersion++;
        status = target;

        transitions.add(
                new JobStateTransition(
                        stateVersion,
                        previous,
                        target,
                        System.currentTimeMillis(),
                        reason));
    }

    private static String terminalReason(
            ServerJobStatus status,
            Throwable failure) {

        if (status == ServerJobStatus.SUCCEEDED) {
            return "execution-succeeded";
        }

        if (status == ServerJobStatus.CANCELED) {
            return "cancellation-completed";
        }

        if (status == ServerJobStatus.LOST) {
            return "worker-lost-execution";
        }

        return failure == null
                ? "execution-failed"
                : "execution-failed:"
                + failure.getClass()
                .getSimpleName();
    }

    synchronized JobExecutionMetadata metadata() {
        Map<String, TableDdl> tableDdls =
                new LinkedHashMap<String, TableDdl>();

        JobResult currentResult = result;
        if (currentResult != null) {
            for (PipelineResult pipelineResult :
                    currentResult.getPipelineResults()) {
                if (pipelineResult.getTableDdl() != null) {
                    tableDdls.put(
                            pipelineResult.getPipelineId(),
                            pipelineResult.getTableDdl());
                }
            }
        }

        return new JobExecutionMetadata(
                submission.getExternalExecutionId(),
                submission.getIdempotencyKey(),
                submission.getDefinitionVersion(),
                submission.getConfigDigest(),
                submittedTimeMillis,
                queuedTimeMillis,
                stateVersion,
                cancellationRequested,
                transitions,
                tableDdls,
                runId,
                jobLogFile);
    }

    boolean isCancellationRequested() {
        return cancellationRequested;
    }

    String getJobId() {
        return jobId;
    }

    String getJobName() {
        return submission.getDefinition()
                .getName();
    }

    JobDefinition getDefinition() {
        return submission.getDefinition();
    }

    long getCreateTimeMillis() {
        return createTimeMillis;
    }

    long getStartTimeMillis() {
        return startTimeMillis;
    }

    long getEndTimeMillis() {
        return endTimeMillis;
    }

    ServerJobStatus getStatus() {
        return status;
    }

    JobExecution getExecution() {
        return execution;
    }

    JobResult getResult() {
        return result;
    }

    Throwable getFailure() {
        return failure;
    }
}
