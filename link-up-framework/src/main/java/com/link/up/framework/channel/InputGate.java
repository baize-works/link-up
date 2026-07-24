package com.link.up.framework.channel;

import java.util.Objects;

/**
 * Task 输入端。
 *
 * <p>当前一个 SinkTask 对应一个 ChannelReader。
 * 后续可扩展为多 Channel 合并读取。
 */
public final class InputGate<T> {

    private final ChannelReader<T> reader;

    public InputGate(ChannelReader<T> reader) {
        this.reader =
                Objects.requireNonNull(
                        reader,
                        "reader must not be null");
    }

    /**
     * @return 下一条数据；输入结束时返回 null
     */
    public T read() throws Exception {
        return reader.read();
    }
}