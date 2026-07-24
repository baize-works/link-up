package com.link.up.framework.execution.source;

import com.link.up.api.source.RecordBatch;

/**
 * Source 批次数据消费者。
 */
@FunctionalInterface
public interface RecordBatchConsumer<T> {

    void accept(RecordBatch<T> batch)
            throws Exception;
}