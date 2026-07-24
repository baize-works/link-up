package com.link.up.framework.channel;

/**
 * Channel 数据读取端。
 */
public interface ChannelReader<T> {

    /**
     * 读取下一条数据。
     *
     * @return 下一条数据；所有生产者结束后返回 null
     */
    T read() throws Exception;
}