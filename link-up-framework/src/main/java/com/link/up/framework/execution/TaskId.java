package com.link.up.framework.execution;

import java.util.Objects;

/**
 * Immutable semantic task identity.
 */
public final class TaskId {
    private final String pipelineId;
    private final TaskType taskType;
    private final int subtaskIndex;
    private final int parallelism;

    public TaskId(String pipelineId, TaskType taskType, int subtaskIndex, int parallelism) {
        this.pipelineId = Objects.requireNonNull(pipelineId, "pipelineId must not be null");
        this.taskType = Objects.requireNonNull(taskType, "taskType must not be null");
        if (subtaskIndex < 0 || parallelism <= 0 || subtaskIndex >= parallelism)
            throw new IllegalArgumentException("invalid subtask index or parallelism");
        this.subtaskIndex = subtaskIndex;
        this.parallelism = parallelism;
    }

    /**
     * @deprecated Use the typed constructor. Retained only for source compatibility.
     */
    @Deprecated
    public TaskId(String stageName, int subtaskIndex, int parallelism) {
        this(stageName.endsWith("/sink") || "sink".equals(stageName) ? stageName.replaceAll("/sink$", "") : stageName.replaceAll("/source$", ""), stageName.endsWith("/sink") || "sink".equals(stageName) ? TaskType.SINK : TaskType.SOURCE, subtaskIndex, parallelism);
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    /**
     * Display-only legacy name.
     */
    public String getStageName() {
        return pipelineId + "/" + taskType.name().toLowerCase();
    }

    public int getSubtaskIndex() {
        return subtaskIndex;
    }

    public int getParallelism() {
        return parallelism;
    }

    @Override
    public String toString() {
        return getStageName() + "-" + subtaskIndex + "/" + parallelism;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskId)) return false;
        TaskId x = (TaskId) o;
        return subtaskIndex == x.subtaskIndex && parallelism == x.parallelism && pipelineId.equals(x.pipelineId) && taskType == x.taskType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineId, taskType, subtaskIndex, parallelism);
    }
}
