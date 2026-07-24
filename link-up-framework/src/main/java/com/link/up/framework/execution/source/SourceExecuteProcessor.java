package com.link.up.framework.execution.source;

import com.link.up.api.source.Source;
import com.link.up.api.source.SourceReader;
import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.type.FluxRow;
import com.link.up.framework.factory.PreparedSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 离线 Source 执行处理器。
 * <p>
 * A bounded source executor which assigns every split to exactly one reader.
 * Readers own their connections and may run concurrently; emitted batches are
 * handed to the caller's thread-safe consumer.
 */
public final class SourceExecuteProcessor {

    /**
     * Round-robin assignment balances table and range splits without duplication.
     */
    static <SplitT extends SourceSplit> List<List<SplitT>> assignSplits(
            List<SplitT> splits, int parallelism) {
        Objects.requireNonNull(splits, "splits must not be null");
        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }
        int taskCount = Math.min(splits.size(), parallelism);
        List<List<SplitT>> assignments = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            assignments.add(new ArrayList<SplitT>());
        }
        for (int i = 0; i < splits.size(); i++) {
            assignments.get(i % taskCount).add(splits.get(i));
        }
        List<List<SplitT>> immutable = new ArrayList<>(taskCount);
        for (List<SplitT> assignment : assignments) {
            immutable.add(Collections.unmodifiableList(assignment));
        }
        return Collections.unmodifiableList(immutable);
    }

    /**
     * Executes with one reader for callers that do not configure an environment.
     */
    public <SplitT extends SourceSplit> void execute(
            SourceAction<SplitT> action,
            RecordBatchConsumer<FluxRow> consumer)
            throws Exception {

        execute(action, 1, consumer);
    }

    public <SplitT extends SourceSplit> void execute(
            SourceAction<SplitT> action,
            int parallelism,
            RecordBatchConsumer<FluxRow> consumer)
            throws Exception {

        Objects.requireNonNull(
                action,
                "action must not be null");

        Objects.requireNonNull(
                consumer,
                "consumer must not be null");

        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }

        PreparedSource<SplitT> preparedSource =
                action.getPreparedSource();

        Source<SplitT> source =
                preparedSource.getSource();

        List<SplitT> splits =
                source.createSplits(
                        preparedSource.getTables(),
                        parallelism);

        if (splits == null) {
            throw new IllegalStateException(
                    "Source returned null splits");
        }

        if (splits.isEmpty()) {
            return;
        }

        List<List<SplitT>> assignments = assignSplits(splits, parallelism);
        ExecutorService executor = Executors.newFixedThreadPool(
                assignments.size());
        List<Future<?>> futures = new ArrayList<>(assignments.size());
        try {
            for (List<SplitT> assignment : assignments) {
                futures.add(executor.submit(() -> {
                    SourceReader<FluxRow, SplitT> reader = source.createReader(
                            preparedSource.getTables(), action.getBatchSize());
                    new SourceTask<FluxRow, SplitT>(reader, assignment, consumer).execute();
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (Exception failure) {
            for (Future<?> future : futures) {
                future.cancel(true);
            }
            Throwable cause = failure instanceof java.util.concurrent.ExecutionException
                    ? failure.getCause() : failure;
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } finally {
            executor.shutdownNow();
        }
    }
}
