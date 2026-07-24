package com.link.up.connector.jdbc.sink.savemode;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.SchemaCompatibilityReport;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.catalog.WritableCatalog;
import com.link.up.api.table.catalog.exception.TableNotFoundException;
import com.link.up.connector.jdbc.sink.DataSaveMode;
import com.link.up.connector.jdbc.sink.SchemaSaveMode;

import java.util.Objects;

/**
 * Default implementation of the JDBC table save-mode lifecycle.
 */
public class DefaultSaveModeHandler implements SaveModeHandler {
    protected final SchemaSaveMode schemaSaveMode;
    protected final DataSaveMode dataSaveMode;
    protected final WritableCatalog catalog;
    protected final CatalogTable table;
    protected final TablePath tablePath;

    public DefaultSaveModeHandler(
            SchemaSaveMode schemaSaveMode,
            DataSaveMode dataSaveMode,
            WritableCatalog catalog,
            CatalogTable table) {
        this.schemaSaveMode =
                Objects.requireNonNull(schemaSaveMode, "schemaSaveMode must not be null");
        this.dataSaveMode =
                Objects.requireNonNull(dataSaveMode, "dataSaveMode must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.table = Objects.requireNonNull(table, "table must not be null");
        this.tablePath = table.getTablePath();
    }

    @Override
    public void open() {
        catalog.open();
    }

    @Override
    public void handleSchemaSaveMode() {
        boolean exists = catalog.tableExists(tablePath);
        switch (schemaSaveMode) {
            case RECREATE_SCHEMA:
                if (exists) {
                    catalog.dropTable(tablePath, false);
                }
                createTable();
                return;
            case CREATE_SCHEMA_WHEN_NOT_EXIST:
                if (!exists) {
                    createTable();
                }
                if (exists) {
                    validateExistingSchema(false);
                }
                return;
            case CREATE_OR_ADD_COLUMNS:
                if (!exists) {
                    createTable();
                    return;
                }
                SchemaCompatibilityReport report = validateExistingSchema(true);
                for (com.link.up.api.table.catalog.Column column : report.getMissingTargetColumns()) {
                    catalog.addColumn(tablePath, column);
                }
                return;
            case ERROR_WHEN_SCHEMA_NOT_EXIST:
                if (!exists) {
                    throw new TableNotFoundException(catalog.name(), tablePath);
                }
                validateExistingSchema(false);
                return;
            case IGNORE:
                if (!exists) {
                    throw new TableNotFoundException(catalog.name(), tablePath);
                }
                validateExistingSchema(false);
                return;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported schema save mode: " + schemaSaveMode);
        }
    }

    private SchemaCompatibilityReport validateExistingSchema(boolean allowMissingColumns) {
        SchemaCompatibilityReport report = SchemaCompatibilityReport.compare(
                table.getTableSchema(), catalog.getTable(tablePath).getTableSchema());
        if (!report.getIncompatibleColumns().isEmpty()
                || (!allowMissingColumns && !report.getMissingTargetColumns().isEmpty())) {
            throw new IllegalArgumentException("Incompatible target schema: "
                    + report.getIncompatibleColumns() + "; missing target columns: "
                    + report.getMissingTargetColumns());
        }
        return report;
    }

    @Override
    public void handleDataSaveMode() {
        switch (dataSaveMode) {
            case DROP_DATA:
                catalog.truncateTable(tablePath, false);
                return;
            case APPEND_DATA:
            case CUSTOM_PROCESSING:
            case ERROR_WHEN_DATA_EXISTS:
                // Row writing (or the database constraint) owns these modes for JDBC.
                return;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported data save mode: " + dataSaveMode);
        }
    }

    protected void createTable() {
        catalog.createTable(table, false);
    }

    @Override
    public void close() {
        catalog.close();
    }
}
