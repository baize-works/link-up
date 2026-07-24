package com.link.up.framework.execution;

import java.util.Objects;

/**
 * Task 执行结果。
 */
public final class TaskResult {

    private final TaskId taskId;

    private final TaskState state;

    private final Throwable failure;

    private TaskResult(
            TaskId taskId,
            TaskState state,
            Throwable failure) {

        this.taskId =
                Objects.requireNonNull(
                        taskId,
                        "taskId must not be null");

        this.state =
                Objects.requireNonNull(
                        state,
                        "state must not be null");

        this.failure = failure;
    }

    public static TaskResult finished(
            TaskId taskId) {

        return new TaskResult(
                taskId,
                TaskState.FINISHED,
                null);
    }

    public static TaskResult failed(
            TaskId taskId,
            Throwable failure) {

        return new TaskResult(
                taskId,
                TaskState.FAILED,
                Objects.requireNonNull(
                        failure,
                        "failure must not be null"));
    }

    public static TaskResult canceled(
            TaskId taskId,
            Throwable cause) {

        return new TaskResult(
                taskId,
                TaskState.CANCELED,
                cause);
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public TaskState getState() {
        return state;
    }

    public Throwable getFailure() {
        return failure;
    }

    public boolean isFailed() {
        return state == TaskState.FAILED;
    }
}