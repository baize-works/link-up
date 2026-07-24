package com.link.up.framework.channel;

import com.link.up.framework.metrics.ChannelMetrics;

/**
 * Source 和 Sink 之间的数据通道。
 */
public interface DataChannel<T> extends AutoCloseable {

    /**
     * 为一个生产任务创建 Writer。
     */
    ChannelWriter<T> openWriter();

    /**
     * 创建 Channel 唯一的 Reader。
     */
    ChannelReader<T> openReader();

    /**
     * 标记 Channel 失败。
     */
    void fail(Throwable cause);

    /**
     * 主动取消 Channel。
     */
    void cancel();

    /**
     * 获取 Channel 指标。
     */
    ChannelMetrics getMetrics();

    @Override
    void close();
}