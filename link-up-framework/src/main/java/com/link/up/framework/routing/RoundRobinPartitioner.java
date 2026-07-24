package com.link.up.framework.routing;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询分区器。
 */
public final class RoundRobinPartitioner<T>
        implements Partitioner<T> {

    private final AtomicInteger sequence =
            new AtomicInteger();

    @Override
    public int selectChannel(
            T value,
            int numberOfChannels) {

        if (numberOfChannels <= 0) {
            throw new IllegalArgumentException(
                    "numberOfChannels must be greater than 0");
        }

        return Math.floorMod(
                sequence.getAndIncrement(),
                numberOfChannels);
    }
}