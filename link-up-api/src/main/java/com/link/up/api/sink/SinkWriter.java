package com.link.up.api.sink;

import com.link.up.api.source.RecordBatch;
import com.link.up.api.table.catalog.CatalogTable;

/**
 * 一个 SinkTask 对应一个 SinkWriter。
 *
 * <p>事务生命周期：
 *
 * <pre>
 * open
 *   -> write
 *   -> write
 *   -> prepareCommit
 *   -> commit / abort
 *   -> close
 * </pre>
 */
public interface SinkWriter<T> extends AutoCloseable {

    /**
     * 初始化 Writer。
     *
     * <p>事务型 Sink 可在这里初始化事务上下文；
     * 非事务型 Sink 可以使用默认实现。
     */
    default void open() throws Exception {
    }

    /**
     * 写入一批数据。
     *
     * <p>调用 write 成功不代表数据已经最终提交。
     */
    void write(
            RecordBatch<T> batch,
            CatalogTable sourceTable)
            throws Exception;

    /**
     * Validates that this task is ready to commit; no coordinator is implied.
     */
    default void prepareCommit() throws Exception {
    }

    /**
     * Commits this SinkTask only.
     */
    default void commit() throws Exception {
    }

    /**
     * Aborts uncommitted work for this SinkTask only.
     */
    default void abort() throws Exception {
        rollback();
    }

    /**
     * @deprecated Implement {@link #abort()} instead.
     */
    @Deprecated
    default void rollback() throws Exception {
    }

    /**
     * The durability boundary supplied by this writer.
     *
     * <p>Ordinary JDBC writers are {@link CommitScope#TASK_LOCAL}; they never
     * claim Job-level atomicity.
     */
    default CommitScope getCommitScope() {
        return CommitScope.TASK_LOCAL;
    }

    /**
     * Human-readable retry guidance to surface when this task has committed.
     */
    default String getRetryAdvice() {
        return "This sink commits per task; verify already committed targets before retrying.";
    }

    /**
     * 释放 Writer 持有的资源。
     *
     * <p>close 不负责提交事务。
     */
    @Override
    void close() throws Exception;
}
