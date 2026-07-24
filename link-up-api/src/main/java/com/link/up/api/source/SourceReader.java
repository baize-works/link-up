package com.link.up.api.source;

import java.util.List;

/**
 * 离线 Source 数据读取器。
 *
 * @param <T>      输出数据类型
 * @param <SplitT> Source 分片类型
 */
public interface SourceReader<T, SplitT extends SourceSplit>
        extends AutoCloseable {

    /**
     * 打开 Reader，并设置当前 Reader 负责的分片。
     */
    void open(List<SplitT> splits) throws Exception;

    /**
     * Opens task-local resources for single-split processing.
     */
    default void open() throws Exception {
        open(java.util.Collections.<SplitT>emptyList());
    }

    /**
     * Opens one split. Implementations supporting dynamic assignment must override this method.
     */
    default void openSplit(SplitT split) throws Exception {
        throw new UnsupportedOperationException("Single-split lifecycle is not supported");
    }

    /**
     * Closes the currently opened split while keeping task-local resources open.
     */
    default void closeSplit() throws Exception {
    }

    /**
     * 读取下一批数据。
     * <p>
     * 当全部分片读取完成时，返回 RecordBatch.endOfInput()。
     */
    RecordBatch<T> readBatch() throws Exception;

    @Override
    void close() throws Exception;
}