package com.link.up.framework.routing;

/**
 * 固定转发分区器。
 *
 * <p>通常用于上游和下游并行度一致的场景。
 */
public final class ForwardPartitioner<T>
        implements Partitioner<T> {

    private final int preferredChannel;

    public ForwardPartitioner(int preferredChannel) {
        if (preferredChannel < 0) {
            throw new IllegalArgumentException(
                    "preferredChannel must not be negative");
        }

        this.preferredChannel = preferredChannel;
    }

    @Override
    public int selectChannel(
            T value,
            int numberOfChannels) {

        if (numberOfChannels <= 0) {
            throw new IllegalArgumentException(
                    "numberOfChannels must be greater than 0");
        }

        return preferredChannel % numberOfChannels;
    }
}