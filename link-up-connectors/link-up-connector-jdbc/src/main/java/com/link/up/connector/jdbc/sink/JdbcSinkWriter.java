package com.link.up.connector.jdbc.sink;

import com.link.up.api.dirtydata.*;
import com.link.up.api.sink.CommitScope;
import com.link.up.api.sink.DirtyDataAwareSinkWriter;
import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.sink.SinkWriter;
import com.link.up.api.source.RecordBatch;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.type.FluxRow;
import com.link.up.connector.jdbc.config.JdbcSinkConfig;

import java.nio.file.Paths;
import java.util.Objects;

/**
 * JDBC SinkWriter。
 *
 * <p>一个 SinkTask 使用一个 JdbcSinkWriter，
 * 对应一条独立 JDBC 事务。
 */
public final class JdbcSinkWriter
        implements SinkWriter<FluxRow>, DirtyDataAwareSinkWriter {

    private final JdbcOutputFormat outputFormat;

    private final JdbcSinkConfig config;
    private DirtyDataContext dirtyDataContext;

    public JdbcSinkWriter(
            JdbcSinkConfig config,
            PreparedSinkMetadata metadata) {

        this.config = Objects.requireNonNull(config, "config must not be null");
        this.outputFormat =
                new JdbcOutputFormatBuilder()
                        .withMetadata(Objects.requireNonNull(metadata, "metadata must not be null"))
                        .withConfig(
                                this.config)
                        .build();
    }

    @Override
    public void configureDirtyData(DirtyDataContext context) {
        DirtyDataCollector collector;
        switch (config.getDirtyDataOutputType()) {
            case LOGGING:
                collector = new LoggingDirtyDataCollector(context.getTaskId(), config.getDirtyDataMaxSamples(), config.getDirtyDataMaxCount(), config.getDirtyDataMaxPercentage());
                break;
            case JSONL:
                collector = new JsonLinesDirtyDataCollector(context.getTaskId(), config.getDirtyDataMaxSamples(), config.getDirtyDataMaxCount(), config.getDirtyDataMaxPercentage(), Paths.get(config.getDirtyDataOutputPath()));
                break;
            default:
                collector = new BoundedMemoryDirtyDataCollector(context.getTaskId(), config.getDirtyDataMaxSamples(), config.getDirtyDataMaxCount(), config.getDirtyDataMaxPercentage());
        }
        dirtyDataContext = context;
        outputFormat.setDirtyDataCollector(collector, context);
    }

    @Override
    public DirtyDataSummary getDirtyDataSummary() {
        return outputFormat.getDirtyDataSummary();
    }

    @Override
    public void open() throws Exception {
        outputFormat.open();
    }

    @Override
    public void write(
            RecordBatch<FluxRow> batch,
            CatalogTable sourceTable)
            throws Exception {

        if (batch == null
                || batch.isEndOfInput()
                || batch.getRecords().isEmpty()) {
            return;
        }

        if (dirtyDataContext != null)
            outputFormat.updateDirtyDataContext(dirtyDataContext.withDataSet(batch.getDataSetId(), batch.getSplitId()));

        outputFormat.write(
                batch.getRecords(),
                sourceTable);
    }

    @Override
    public void commit() throws Exception {
        outputFormat.commit();
    }

    @Override
    public void abort() throws Exception {
        outputFormat.rollback();
    }

    @Override
    public CommitScope getCommitScope() {
        return CommitScope.TASK_LOCAL;
    }

    @Override
    public String getRetryAdvice() {
        if (config.getWriteMode() == com.link.up.connector.jdbc.config.JdbcWriteMode.UPSERT) {
            return "JDBC UPSERT is usually safe to rerun when primary keys are stable, but safety depends on dialect semantics.";
        }
        switch (config.getDataSaveMode()) {
            case APPEND_DATA:
                return "JDBC APPEND may duplicate rows on retry; verify committed targets before rerunning.";
            case DROP_DATA:
                return "JDBC DROP_DATA may have cleared target data before failure; inspect and restore/reload as needed before rerunning.";
            case CUSTOM_PROCESSING:
                return "JDBC CUSTOM_PROCESSING retry safety is determined by the configured SQL; review its effects before rerunning.";
            default:
                return "JDBC retry safety depends on the configured save and write modes; verify committed targets before rerunning.";
        }
    }

    @Override
    public void close() throws Exception {
        outputFormat.close();
    }


}
