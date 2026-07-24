package com.link.up.framework.execution;

import com.link.up.framework.execution.task.ExecutionTask;
import com.link.up.framework.execution.task.SinkTask;
import com.link.up.framework.job.CommitSummary;
import com.link.up.framework.metrics.JobMetrics;
import com.link.up.framework.metrics.TaskMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Job Task 协调器。
 *
 * <p>任意一个 Task 失败时：
 *
 * <ol>
 *     <li>设置全局取消标记</li>
 *     <li>关闭或失败所有 Channel</li>
 *     <li>取消其他 Task</li>
 *     <li>中断执行线程</li>
 * </ol>
 */
public final class ExecutionCoordinator {

    private final TaskExecutor taskExecutor;

    private final CancellationToken cancellationToken;

    private final JobMetrics jobMetrics;

    private final ClassLoader classLoader;

    private final Runnable cancellationHook;

    public ExecutionCoordinator(
            TaskExecutor taskExecutor,
            CancellationToken cancellationToken,
            JobMetrics jobMetrics,
            ClassLoader classLoader,
            Runnable cancellationHook) {

        this.taskExecutor =
                Objects.requireNonNull(
                        taskExecutor,
                        "taskExecutor must not be null");

        this.cancellationToken =
                Objects.requireNonNull(
                        cancellationToken,
                        "cancellationToken must not be null");

        this.jobMetrics =
                Objects.requireNonNull(
                        jobMetrics,
                        "jobMetrics must not be null");

        this.classLoader =
                Objects.requireNonNull(
                        classLoader,
                        "classLoader must not be null");

        this.cancellationHook =
                Objects.requireNonNull(
                        cancellationHook,
                        "cancellationHook must not be null");
    }

    public ExecutionOutcome execute(
            List<ExecutionTask> sinkTasks,
            List<ExecutionTask> sourceTasks) {

        Objects.requireNonNull(
                sinkTasks,
                "sinkTasks must not be null");

        Objects.requireNonNull(
                sourceTasks,
                "sourceTasks must not be null");

        List<ExecutionTask> allTasks =
                new ArrayList<ExecutionTask>();

        /*
         * 先启动 Sink，让消费者优先进入等待状态。
         */
        allTasks.addAll(sinkTasks);
        allTasks.addAll(sourceTasks);

        if (allTasks.isEmpty()) {
            return new ExecutionOutcome(null, CommitSummary.empty());
        }

        List<Future<TaskResult>> futures =
                new ArrayList<Future<TaskResult>>(
                        allTasks.size());

        Throwable firstFailure = null;

        try {
            for (ExecutionTask task : allTasks) {
                TaskMetrics metrics =
                        jobMetrics.registerTask(
                                task.getTaskId());

                TaskContext context =
                        new TaskContext(
                                task.getTaskId(),
                                cancellationToken,
                                metrics,
                                classLoader);

                futures.add(
                        taskExecutor.submit(
                                task,
                                context));
            }

            for (int i = 0; i < allTasks.size(); i++) {
                Future<TaskResult> completed =
                        taskExecutor.takeCompleted();

                try {
                    TaskResult result =
                            completed.get();

                    if (result.isFailed()
                            && firstFailure == null) {

                        firstFailure =
                                result.getFailure();

                        cancelAll(
                                allTasks,
                                futures,
                                firstFailure);
                    }

                } catch (CancellationException ignored) {
                    // 由其他 Task 失败触发的取消。

                } catch (ExecutionException exception) {
                    if (firstFailure == null) {
                        firstFailure =
                                exception.getCause() == null
                                        ? exception
                                        : exception.getCause();

                        cancelAll(
                                allTasks,
                                futures,
                                firstFailure);
                    }
                }
            }

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();

            firstFailure = interrupted;

            cancelAll(
                    allTasks,
                    futures,
                    interrupted);

        } catch (Throwable throwable) {
            firstFailure = throwable;

            cancelAll(
                    allTasks,
                    futures,
                    throwable);
        }

        return new ExecutionOutcome(firstFailure, summarizeCommits(sinkTasks));
    }

    private CommitSummary summarizeCommits(List<ExecutionTask> sinkTasks) {
        int committed = 0, empty = 0, finished = 0;
        long attempted = 0, written = 0, unknown = 0, failed = 0;
        String retryAdvice = "This sink commits per task; verify already committed targets before retrying.";
        com.link.up.api.sink.CommitScope scope = com.link.up.api.sink.CommitScope.TASK_LOCAL;
        for (ExecutionTask task : sinkTasks)
            if (task instanceof SinkTask) {
                SinkTask sink = (SinkTask) task;
                TaskMetrics m = jobMetrics.getTaskMetrics().get(sink.getTaskId());
                long successful = m == null ? 0 : m.getSinkWriteSuccessRecordCount();
                attempted += m == null ? 0 : m.getAttemptedRecordCount();
                written += successful;
                unknown += m == null ? 0 : m.getUnknownStateRecordCount();
                failed += m == null ? 0 : m.getFailedRecordCount();
                if (m != null && m.getState() == TaskState.FINISHED) finished++;
                if (sink.isCommitted()) {
                    committed++;
                    if (successful == 0) empty++;
                }
                scope = sink.getCommitScope();
                retryAdvice = sink.getRetryAdvice();
            }
        return new CommitSummary(sinkTasks.size(), finished, committed, empty, sinkTasks.size() - committed,
                attempted, written, committed == 0 ? 0 : written, failed, unknown, scope, retryAdvice);
    }

    private void cancelAll(
            List<ExecutionTask> tasks,
            List<Future<TaskResult>> futures,
            Throwable cause) {

        cancellationToken.cancel(cause);

        try {
            cancellationHook.run();
        } catch (Throwable hookFailure) {
            cause.addSuppressed(
                    hookFailure);
        }

        for (ExecutionTask task : tasks) {
            try {
                task.cancel();
            } catch (Throwable cancelFailure) {
                cause.addSuppressed(
                        cancelFailure);
            }
        }

        for (Future<TaskResult> future : futures) {
            future.cancel(true);
        }
    }

    /**
     * Result of task coordination, including local sink commit observations.
     */
    public static final class ExecutionOutcome {
        private final Throwable failure;
        private final CommitSummary commitSummary;

        private ExecutionOutcome(Throwable failure, CommitSummary commitSummary) {
            this.failure = failure;
            this.commitSummary = commitSummary;
        }

        public Throwable getFailure() {
            return failure;
        }

        public CommitSummary getCommitSummary() {
            return commitSummary;
        }
    }
}
