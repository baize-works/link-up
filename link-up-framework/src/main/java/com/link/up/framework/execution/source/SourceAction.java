package com.link.up.framework.execution.source;

import com.link.up.api.source.SourceSplit;
import com.link.up.framework.factory.PreparedSource;

import java.util.Objects;

/**
 * Source 执行动作。
 * <p>
 * Action 只描述要执行什么，不持有已打开的数据库资源。
 */
public final class SourceAction<SplitT extends SourceSplit> {

    private final String name;

    private final PreparedSource<SplitT> preparedSource;

    private final int batchSize;

    public SourceAction(
            String name,
            PreparedSource<SplitT> preparedSource,
            int batchSize) {

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than 0");
        }

        this.name =
                Objects.requireNonNull(
                        name,
                        "name must not be null");

        this.preparedSource =
                Objects.requireNonNull(
                        preparedSource,
                        "preparedSource must not be null");

        this.batchSize = batchSize;
    }

    public String getName() {
        return name;
    }

    public PreparedSource<SplitT> getPreparedSource() {
        return preparedSource;
    }

    public int getBatchSize() {
        return batchSize;
    }
}