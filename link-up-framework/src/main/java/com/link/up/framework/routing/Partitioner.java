package com.link.up.framework.routing;

/**
 * 数据分区器。
 */
public interface Partitioner<T> {

    /**
     * 选择目标 Channel。
     */
    int selectChannel(
            T value,
            int numberOfChannels);
}