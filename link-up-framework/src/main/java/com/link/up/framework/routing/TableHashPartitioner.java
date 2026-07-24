package com.link.up.framework.routing;

import com.link.up.framework.channel.RecordEnvelope;

/**
 * 按源表路径进行 Hash 分区。
 *
 * <p>同一张源表的数据始终进入同一个 SinkTask，
 * 避免相同目标表被多个 Writer 并发初始化和写入。
 */
public final class TableHashPartitioner<T>
        implements Partitioner<RecordEnvelope<T>> {

    @Override
    public int selectChannel(
            RecordEnvelope<T> value,
            int numberOfChannels) {

        if (numberOfChannels <= 0) {
            throw new IllegalArgumentException(
                    "numberOfChannels must be greater than 0");
        }

        return Math.floorMod(
                value.getTablePath().hashCode(),
                numberOfChannels);
    }
}