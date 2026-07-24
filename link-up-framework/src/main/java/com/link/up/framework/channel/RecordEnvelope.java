package com.link.up.framework.channel;

import com.link.up.api.source.RecordBatch;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;

import java.util.Objects;

/**
 * Source 和 Sink 之间传输的数据对象。
 *
 * <p>除了数据批次，还携带结构化表路径和表结构，
 * 避免 Sink 再通过字符串解析数据集标识。
 */
public final class RecordEnvelope<T> {

    private final TablePath tablePath;

    private final CatalogTable catalogTable;

    private final RecordBatch<T> batch;

    public RecordEnvelope(
            TablePath tablePath,
            CatalogTable catalogTable,
            RecordBatch<T> batch) {

        this.tablePath =
                Objects.requireNonNull(
                        tablePath,
                        "tablePath must not be null");

        this.catalogTable =
                Objects.requireNonNull(
                        catalogTable,
                        "catalogTable must not be null");

        this.batch =
                Objects.requireNonNull(
                        batch,
                        "batch must not be null");
    }

    public TablePath getTablePath() {
        return tablePath;
    }

    public CatalogTable getCatalogTable() {
        return catalogTable;
    }

    public RecordBatch<T> getBatch() {
        return batch;
    }
}