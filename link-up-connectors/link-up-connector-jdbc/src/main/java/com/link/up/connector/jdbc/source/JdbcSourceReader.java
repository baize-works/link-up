package com.link.up.connector.jdbc.source;

import com.link.up.api.source.RecordBatch;
import com.link.up.api.source.SourceReader;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.type.FluxRow;
import com.link.up.connector.jdbc.config.JdbcSourceConfig;
import com.link.up.connector.jdbc.internal.JdbcInputFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JDBC 离线数据读取器。
 * <p>
 * Reader 依次读取分配给自己的分片，并按批次输出 FluxRow。
 */
public final class JdbcSourceReader
        implements SourceReader<FluxRow, JdbcSourceSplit> {

    private final JdbcInputFormat inputFormat;

    private final int batchSize;

    private List<JdbcSourceSplit> splits =
            Collections.emptyList();

    private int splitIndex;

    private JdbcSourceSplit currentSplit;

    private boolean opened;

    private boolean finished;

    public JdbcSourceReader(
            JdbcSourceConfig config,
            Map<TablePath, CatalogTable> tables,
            int batchSize) {

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than 0");
        }

        this.inputFormat =
                new JdbcInputFormat(config, tables);

        this.batchSize = batchSize;
    }

    @Override
    public void open(List<JdbcSourceSplit> splits)
            throws Exception {

        if (opened) {
            throw new IllegalStateException(
                    "JdbcSourceReader has already been opened");
        }

        if (splits == null) {
            throw new IllegalArgumentException(
                    "splits must not be null");
        }

        this.splits =
                Collections.unmodifiableList(
                        new ArrayList<>(splits));

        this.splitIndex = 0;
        this.currentSplit = null;
        this.finished = false;

        try {
            inputFormat.openInputFormat();
            opened = true;
        } catch (Exception e) {
            try {
                inputFormat.closeInputFormat();
            } catch (Exception closeException) {
                e.addSuppressed(closeException);
            }

            this.splits = Collections.emptyList();
            this.splitIndex = 0;
            this.currentSplit = null;
            this.finished = false;

            throw e;
        }
    }

    @Override
    public void open() throws Exception {
        open(Collections.<JdbcSourceSplit>emptyList());
    }

    @Override
    public void openSplit(JdbcSourceSplit split) throws Exception {
        checkOpened();
        if (currentSplit != null) throw new IllegalStateException("A split is already open");
        currentSplit = java.util.Objects.requireNonNull(split, "split must not be null");
        inputFormat.open(currentSplit);
    }

    @Override
    public void closeSplit() throws Exception {
        closeCurrentSplit();
    }

    @Override
    public RecordBatch<FluxRow> readBatch()
            throws Exception {

        checkOpened();

        if (finished) {
            return RecordBatch.endOfInput();
        }

        while (true) {
            if (currentSplit == null) {
                if (!openNextSplit()) {
                    finished = true;
                    return RecordBatch.endOfInput();
                }
            }

            List<FluxRow> rows =
                    new ArrayList<>(batchSize);

            JdbcSourceSplit batchSplit = currentSplit;

            while (rows.size() < batchSize
                    && !inputFormat.reachedEnd()) {

                FluxRow row = inputFormat.nextRecord();

                if (row != null) {
                    rows.add(row);
                }
            }

            if (inputFormat.reachedEnd()) {
                closeCurrentSplit();
            }

            /*
             * 一个批次只属于一个分片。
             *
             * 这样一个批次不会混入不同表的数据，
             * dataSetId 也始终保持明确。
             */
            if (!rows.isEmpty()) {
                return RecordBatch.of(
                        batchSplit,
                        rows);
            }
        }
    }

    /**
     * 打开下一个待读取分片。
     */
    private boolean openNextSplit()
            throws Exception {

        if (splitIndex >= splits.size()) {
            return false;
        }

        currentSplit = splits.get(splitIndex++);
        inputFormat.open(currentSplit);

        return true;
    }

    /**
     * 关闭当前分片。
     */
    private void closeCurrentSplit()
            throws Exception {

        if (currentSplit == null) {
            return;
        }

        try {
            inputFormat.close();
        } finally {
            currentSplit = null;
        }
    }

    private void checkOpened() {
        if (!opened) {
            throw new IllegalStateException(
                    "JdbcSourceReader has not been opened");
        }
    }

    @Override
    public void close() throws Exception {
        if (!opened) {
            return;
        }

        try {
            closeCurrentSplit();
        } finally {
            inputFormat.closeInputFormat();
            opened = false;
        }
    }
}
