package com.link.up.framework.execution.task;

import com.link.up.framework.execution.TaskContext;
import com.link.up.framework.execution.TaskId;

/**
 * Framework 最小执行单元。
 */
public interface ExecutionTask {

    TaskId getTaskId();

    void execute(TaskContext context)
            throws Exception;

    /**
     * 主动取消 Task。
     *
     * <p>默认依靠线程中断和 CancellationToken。
     */
    default void cancel() {
        // Default no-op.
    }
}