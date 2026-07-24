package com.baize.flux.framework.execution;

import com.baize.flux.framework.execution.task.ExecutionTask;
import com.baize.flux.framework.metrics.TaskMetrics;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地 Task 线程执行器。
 */
public final class TaskExecutor
        implements AutoCloseable {

    private static final Logger LOG =
            LogManager.getLogger(
                    TaskExecutor.class);

    private final ExecutorService executorService;

    private final ExecutorCompletionService<TaskResult>
            completionService;

    private final String jobName;

    private final long runId;

    public TaskExecutor(
            int threadCount,
            final String threadPrefix) {

        this(
                threadCount,
                threadPrefix,
                "unnamed",
                System.currentTimeMillis());
    }

    public TaskExecutor(
            int threadCount,
            final String threadPrefix,
            String jobName,
            long runId) {

        if (threadCount <= 0) {
            throw new IllegalArgumentException(
                    "threadCount must be greater than 0");
        }

        this.jobName =
                Objects.requireNonNull(
                        jobName,
                        "jobName must not be null");

        if (runId < 0L) {
            throw new IllegalArgumentException(
                    "runId must not be negative");
        }

        this.runId = runId;

        final AtomicInteger sequence =
                new AtomicInteger();

        ThreadFactory threadFactory =
                new ThreadFactory() {
                    @Override
                    public Thread newThread(
                            Runnable runnable) {

                        Thread thread =
                                new Thread(
                                        runnable,
                                        threadPrefix
                                                + "-"
                                                + sequence
                                                .getAndIncrement());

                        thread.setDaemon(false);

                        return thread;
                    }
                };

        this.executorService =
                Executors.newFixedThreadPool(
                        threadCount,
                        threadFactory);

        this.completionService =
                new ExecutorCompletionService<TaskResult>(
                        executorService);
    }

    public Future<TaskResult> submit(
            final ExecutionTask task,
            final TaskContext context) {

        Objects.requireNonNull(
                task,
                "task must not be null");

        Objects.requireNonNull(
                context,
                "context must not be null");

        final String jobId =
                JobLogFileName.createJobId(
                        jobName,
                        runId);

        final String jobLogFile =
                JobLogFileName.create(
                        jobName,
                        runId);

        return completionService.submit(
                new Callable<TaskResult>() {
                    @Override
                    public TaskResult call() {
                        try (CloseableThreadContext.Instance ignored =
                                     CloseableThreadContext
                                             .put(
                                                     "jobId",
                                                     jobId)
                                             .put(
                                                     "jobName",
                                                     jobName)
                                             .put(
                                                     "jobLogFile",
                                                     jobLogFile)) {

                            return executeTask(
                                    task,
                                    context);
                        }
                    }
                });
    }

    private TaskResult executeTask(
            ExecutionTask task,
            TaskContext context) {

        TaskMetrics metrics =
                context.getMetrics();

        if (context
                .getCancellationToken()
                .isCancelled()) {

            metrics.markFinished(
                    TaskState.CANCELED);

            LOG.info(
                    "Task cancelled before execution: {}",
                    task.getTaskId());

            return TaskResult.canceled(
                    task.getTaskId(),
                    context
                            .getCancellationToken()
                            .getCause());
        }

        metrics.markStarted();

        LOG.info(
                "Task started: {}",
                task.getTaskId());

        try {
            task.execute(context);

            if (context
                    .getCancellationToken()
                    .isCancelled()) {

                metrics.markFinished(
                        TaskState.CANCELED);

                LOG.info(
                        "Task cancelled: {}",
                        task.getTaskId());

                return TaskResult.canceled(
                        task.getTaskId(),
                        context
                                .getCancellationToken()
                                .getCause());
            }

            metrics.markFinished(
                    TaskState.FINISHED);

            LOG.info(
                    "Task finished: {}",
                    task.getTaskId());

            return TaskResult.finished(
                    task.getTaskId());

        } catch (Throwable throwable) {
            if (context
                    .getCancellationToken()
                    .isCancelled()
                    && throwable
                    instanceof InterruptedException) {

                Thread.currentThread()
                        .interrupt();

                metrics.markFinished(
                        TaskState.CANCELED);

                LOG.info(
                        "Task cancelled: {}",
                        task.getTaskId());

                return TaskResult.canceled(
                        task.getTaskId(),
                        throwable);
            }

            metrics.markFinished(
                    TaskState.FAILED);

            LOG.error(
                    "Task failed: "
                            + task.getTaskId(),
                    throwable);

            return TaskResult.failed(
                    task.getTaskId(),
                    throwable);
        }
    }

    public Future<TaskResult> takeCompleted()
            throws InterruptedException {

        return completionService.take();
    }

    @Override
    public void close() {
        executorService.shutdownNow();

        try {
            if (!executorService.awaitTermination(
                    5L,
                    TimeUnit.SECONDS)) {

                executorService.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread()
                    .interrupt();

            executorService.shutdownNow();
        }
    }
}
