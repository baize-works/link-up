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
    private final CatalogTable createTableDefinition;
    private boolean tableCreated;

    public JdbcSaveModeHandler(
            SchemaSaveMode schemaSaveMode,
            DataSaveMode dataSaveMode,
            WritableCatalog catalog,
            CatalogTable table,
            boolean createPrimaryKey) {
        super(schemaSaveMode, dataSaveMode, catalog, table);
        this.createPrimaryKey = createPrimaryKey;
        this.createTableDefinition = createTableDefinition(table);
    }

    @Override
    protected void createTable() {
        catalog.createTable(createTableDefinition, false);
        tableCreated = true;
    }

    public CatalogTable getCreateTableDefinition() {
        return createTableDefinition;
    }

    public boolean isTableCreated() {
        return tableCreated;
    }

    private CatalogTable createTableDefinition(CatalogTable sourceTable) {
        if (createPrimaryKey) {
            return sourceTable;
        }
        TableSchema schema =
                TableSchema.builder()
                        .columns(sourceTable.getTableSchema().getColumns())
                        .build();
        return sourceTable.withSchema(schema);
    }
}
