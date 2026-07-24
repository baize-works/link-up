package com.link.up.connector.jdbc.sink.savemode;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TableSchema;
import com.link.up.api.table.catalog.WritableCatalog;
import com.link.up.connector.jdbc.sink.DataSaveMode;
import com.link.up.connector.jdbc.sink.SchemaSaveMode;

/**
 * JDBC-specific save-mode handler that controls whether copied primary keys are created.
 */
public final class JdbcSaveModeHandler extends DefaultSaveModeHandler {
    private final boolean createPrimaryKey;

    public JdbcSaveModeHandler(
            SchemaSaveMode schemaSaveMode,
            DataSaveMode dataSaveMode,
            WritableCatalog catalog,
            CatalogTable table,
            boolean createPrimaryKey) {
        super(schemaSaveMode, dataSaveMode, catalog, table);
        this.createPrimaryKey = createPrimaryKey;
    }

    @Override
    protected void createTable() {
        catalog.createTable(createTableDefinition(), false);
    }

    private CatalogTable createTableDefinition() {
        if (createPrimaryKey) {
            return table;
        }
        TableSchema schema =
                TableSchema.builder()
                        .columns(table.getTableSchema().getColumns())
                        .build();
        return table.withSchema(schema);
    }
}
