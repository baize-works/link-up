package com.link.up.connector.jdbc.sink;

import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.sink.SinkPrepareContext;
import com.link.up.api.sink.SinkPreparer;
import com.link.up.api.table.catalog.*;
import com.link.up.connector.jdbc.config.JdbcSinkConfig;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.core.dialect.JdbcDialectLoader;
import com.link.up.connector.jdbc.sink.savemode.JdbcSaveModeHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes JDBC target mapping, validation and DDL once, before any task starts.
 */
final class JdbcSinkPreparer implements SinkPreparer {
    private final JdbcSinkConfig config;
    private final JdbcDialect dialect;

    JdbcSinkPreparer(JdbcSinkConfig config) {
        this.config = config;
        this.dialect = JdbcDialectLoader.load(config.getConnectionConfig());
    }

    private static List<String> columnNames(CatalogTable table) {
        List<String> names = new ArrayList<String>();
        table.getTableSchema().getColumns().forEach(column -> names.add(column.getName()));
        return names;
    }

    @Override
    public PreparedSinkMetadata prepare(SinkPrepareContext context) throws Exception {
        Map<TablePath, CatalogTable> targets = new LinkedHashMap<TablePath, CatalogTable>();
        Map<TablePath, Object> keys = new LinkedHashMap<TablePath, Object>();
        Catalog catalog = dialect.createCatalog(config.getConnectionConfig());
        try {
            if (!(catalog instanceof WritableCatalog)) {
                throw new IllegalStateException("JDBC catalog does not support DDL: " + dialect.name());
            }
            for (Map.Entry<TablePath, CatalogTable> entry : context.getSourceTables().entrySet()) {
                CatalogTable target = resolveTargetTable(entry.getValue());
                List<String> primaryKeys = resolvePrimaryKeys(target);
                if (config.isUpsert() && !dialect.buildUpsertSql(target.getTablePath(), columnNames(target), primaryKeys).isPresent())
                    throw new IllegalArgumentException("Dialect does not support UPSERT: " + dialect.name());
                JdbcSaveModeHandler handler = new JdbcSaveModeHandler(config.getSchemaSaveMode(), config.getDataSaveMode(), (WritableCatalog) catalog, target, config.isCreatePrimaryKey());
                try {
                    handler.open();
                    handler.handleSaveMode();
                } finally {
                    handler.close();
                }
                targets.put(entry.getKey(), target);
                keys.put(entry.getKey(), new ArrayList<String>(primaryKeys));
            }
            return new PreparedSinkMetadata(targets, keys);
        } finally {
            catalog.close();
        }
    }

    private CatalogTable resolveTargetTable(CatalogTable source) {
        String path = config.resolveTargetTablePath(source.getTablePath());
        return path == null ? source : source.withPath(dialect.parseTablePath(path));
    }

    private List<String> resolvePrimaryKeys(CatalogTable table) {
        if (config.hasConfiguredPrimaryKeys()) return config.getPrimaryKeys();
        PrimaryKey key = table.getTableSchema().getPrimaryKey();
        return key == null ? new ArrayList<String>() : key.getColumnNames();
    }
}
