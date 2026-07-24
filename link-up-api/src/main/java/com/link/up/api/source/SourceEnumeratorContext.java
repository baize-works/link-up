package com.link.up.api.source;

import java.io.Serializable;

/**
 * Source 分片生成上下文。
 */
public final class SourceEnumeratorContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前 Source 配置的并行度。
     */
    private final int parallelism;

    public SourceEnumeratorContext(int parallelism) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }

        this.parallelism = parallelism;
    }

    public int getParallelism() {
        return parallelism;
    }
}