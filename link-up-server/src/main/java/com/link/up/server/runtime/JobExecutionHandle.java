package com.link.up.server.runtime;

import com.link.up.framework.execution.JobExecution;
import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.JobResult;

import java.util.Objects;
import java.util.concurrent.Future;

/**
 * Server 内部使用的可变作业执行句柄。
 *
 * <p>所有状态转换均通过 synchronized 方法完成，
 * 避免提交、开始执行、取消和完成之间发生状态覆盖。
 */
final class JobExecutionHandle {

    enum CancelResult {
        REQUESTED,
        ALREADY_REQUESTED,
        CANCELLED_BEFORE_START
    }

    private final String jobId;
    private final JobDefinition definition;
    private final long createTimeMillis;

    private volatile long startTimeMillis;
    private volatile long endTimeMillis;

    private volatile ServerJobStatus status =
            ServerJobStatus.SUBMITTED;

    private volatile JobExecution execution;
    private volatile Future<?> future;
    private volatile JobResult result;
    private volatile Throwable failure;

    JobExecutionHandle(
            String jobId,
            JobDefinition definition) {

        this.jobId =
                Objects.requireNonNull(
                        jobId,
                        "jobId must not be null");

        this.definition =
                Objects.requireNonNull(
                        definition,
                        "definition must not be null");

        this.createTimeMillis =
                System.currentTimeMillis();
    }

    synchronized void bindFuture(Future<?> future) {
        this.future =
                Objects.requireNonNull(
                        future,
                        "future must not be null");

        if (status == ServerJobStatus.CANCELLING
                || status == ServerJobStatus.CANCELED) {
            future.cancel(true);
        }
    }

    synchronized boolean markRunning() {
        if (status != ServerJobStatus.SUBMITTED) {
            return false;
        }

        startTimeMillis =
                System.currentTimeMillis();

        status = ServerJobStatus.RUNNING;
        return true;
    }

    synchronized void bindExecution(
            JobExecution execution) {

        this.execution =
                Objects.requireNonNull(
                        execution,
                        "execution must not be null");

        if (status == ServerJobStatus.CANCELLING
                || status == ServerJobStatus.CANCELED) {
            execution.cancel();
        }
    }

    synchronized CancelResult requestCancel() {
        if (status.isTerminal()) {
            throw new JobStateConflictException(
                    jobId,
                    status);
        }

        if (status == ServerJobStatus.CANCELLING) {
            return CancelResult.ALREADY_REQUESTED;
        }

        boolean beforeStart =
                startTimeMillis == 0L;

        status = ServerJobStatus.CANCELLING;

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
        this.status = finalStatus;

        execution = null;
        future = null;

        return true;
    }

    boolean isCancellationRequested() {
        return status == ServerJobStatus.CANCELLING
                || status == ServerJobStatus.CANCELED;
    }

    String getJobId() {
        return jobId;
    }

    String getJobName() {
        return definition.getName();
    }

    JobDefinition getDefinition() {
        return definition;
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