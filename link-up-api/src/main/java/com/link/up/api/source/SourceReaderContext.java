package com.link.up.api.source;

import java.io.Serializable;

/**
 * Source Reader 运行上下文。
 */
public final class SourceReaderContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前 Reader 的下标。
     */
    private final int subtaskIndex;

    /**
     * Source 总并行度。
     */
    private final int parallelism;

    /**
     * 每次向 Channel 输出的数据量。
     */
    private final int batchSize;

    public SourceReaderContext(
            int subtaskIndex,
            int parallelism,
            int batchSize) {

        if (subtaskIndex < 0) {
            throw new IllegalArgumentException(
                    "subtaskIndex must not be less than 0");
        }

        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }

        if (subtaskIndex >= parallelism) {
            throw new IllegalArgumentException(
                    "subtaskIndex must be less than parallelism");
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than 0");
        }

        this.subtaskIndex = subtaskIndex;
        this.parallelism = parallelism;
        this.batchSize = batchSize;
    }

    public int getSubtaskIndex() {
        return subtaskIndex;
    }

    public int getParallelism() {
        return parallelism;
    }

    public int getBatchSize() {
        return batchSize;
    }
}