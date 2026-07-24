package com.link.up.framework.channel;

import com.link.up.framework.routing.Partitioner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Task 输出端。
 *
 * <p>根据 Partitioner 将数据发送到指定 Channel。
 */
public final class OutputGate<T>
        implements AutoCloseable {

    private final List<ChannelWriter<T>> writers;

    private final Partitioner<T> partitioner;

    private final AtomicBoolean closed =
            new AtomicBoolean(false);

    public OutputGate(
            List<ChannelWriter<T>> writers,
            Partitioner<T> partitioner) {

        if (writers == null || writers.isEmpty()) {
            throw new IllegalArgumentException(
                    "writers must not be empty");
        }

        this.writers =
                Collections.unmodifiableList(
                        new ArrayList<ChannelWriter<T>>(writers));

        this.partitioner =
                Objects.requireNonNull(
                        partitioner,
                        "partitioner must not be null");
    }

    public void write(T value) throws Exception {
        if (closed.get()) {
            throw new IllegalStateException(
                    "OutputGate has already been closed");
        }

        int channelIndex =
                partitioner.selectChannel(
                        value,
                        writers.size());

        if (channelIndex < 0
                || channelIndex >= writers.size()) {

            throw new IllegalStateException(
                    "Partitioner returned invalid channel index: "
                            + channelIndex);
        }

        writers.get(channelIndex).write(value);
    }

    public void fail(Throwable cause) {
        for (ChannelWriter<T> writer : writers) {
            writer.fail(cause);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        for (ChannelWriter<T> writer : writers) {
            writer.close();
        }
    }
}