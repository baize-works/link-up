package com.link.up.framework.execution.source;

import com.link.up.api.source.RecordBatch;
import com.link.up.api.source.SourceReader;
import com.link.up.api.source.SourceSplit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 单个 Source Reader 执行任务。
 */
public final class SourceTask<T, SplitT extends SourceSplit> {

    private final SourceReader<T, SplitT> reader;

    private final List<SplitT> splits;

    private final RecordBatchConsumer<T> consumer;

    public SourceTask(
            SourceReader<T, SplitT> reader,
            List<SplitT> splits,
            RecordBatchConsumer<T> consumer) {

        this.reader =
                Objects.requireNonNull(
                        reader,
                        "reader must not be null");

        this.splits =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                Objects.requireNonNull(
                                        splits,
                                        "splits must not be null")));

        this.consumer =
                Objects.requireNonNull(
                        consumer,
                        "consumer must not be null");
    }

    /**
     * 执行 Source 读取任务。
     */
    public void execute() throws Exception {
        Throwable failure = null;

        try {
            reader.open(splits);

            while (true) {
                RecordBatch<T> batch =
                        reader.readBatch();

                if (batch == null) {
                    throw new IllegalStateException(
                            "SourceReader returned a null RecordBatch");
                }

                if (batch.isEndOfInput()) {
                    break;
                }

                consumer.accept(batch);
            }

        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;

        } finally {
            try {
                reader.close();
            } catch (Exception closeException) {
                if (failure != null) {
                    failure.addSuppressed(closeException);
                } else {
                    throw closeException;
                }
            }
        }
    }
}