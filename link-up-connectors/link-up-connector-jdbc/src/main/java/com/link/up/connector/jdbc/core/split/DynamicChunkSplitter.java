package com.link.up.connector.jdbc.core.split;

import java.util.List;
import java.util.Objects;

/**
 * 对底层分片器增加并行度限制。
 *
 * <p>该类不会查询数据库，也不会分析数据分布，只负责确保单表计划出的
 * 分片数不超过 reader parallelism。
 */
public final class DynamicChunkSplitter<T>
        implements ChunkSplitter<T> {

    private final ChunkSplitter<T> delegate;
    private final int maxChunks;

    public DynamicChunkSplitter(
            ChunkSplitter<T> delegate,
            int maxChunks) {

        this.delegate = Objects.requireNonNull(
                delegate,
                "delegate must not be null");

        if (maxChunks <= 0) {
            throw new IllegalArgumentException(
                    "maxChunks must be greater than 0");
        }

        this.maxChunks = maxChunks;
    }

    static int effectiveChunkCount(
            int requestedChunks,
            int parallelism) {

        if (requestedChunks <= 0) {
            throw new IllegalArgumentException(
                    "requestedChunks must be greater than 0");
        }

        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }

        return Math.min(requestedChunks, parallelism);
    }

    @Override
    public List<Chunk<T>> split(
            T lower,
            T upper,
            int requestedChunks) {

        return delegate.split(
                lower,
                upper,
                effectiveChunkCount(requestedChunks, maxChunks));
    }
}