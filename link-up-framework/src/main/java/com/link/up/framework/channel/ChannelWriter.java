package com.link.up.framework.channel;

/**
 * Channel 数据写入端。
 *
 * <p>每个生产任务必须持有独立的 Writer，并在任务结束后关闭。
 */
public interface ChannelWriter<T> extends AutoCloseable {

    /**
     * 写入一条数据。
     */
    void write(T value) throws Exception;

    /**
     * 标记 Channel 执行失败。
     */
    void fail(Throwable cause);

    /**
     * 标记当前生产者已经完成。
     */
    @Override
    void close();
}