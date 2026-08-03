package com.link.up.connector.jdbc.source;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.catalog.TableSchema;
import com.link.up.api.table.type.BasicType;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JdbcSourceSplitGeneratorTest {

    @Test
    public void reconcilesUnqualifiedPathWithPreparedCatalogMetadata() {
        TablePath configuredPath =
                TablePath.of("sink_user_info");
        TablePath normalizedPath =
                TablePath.of("test1", "sink_user_info");

        TableSchema schema =
                TableSchema.builder()
                        .column(
                                Column.builder(
                                                "id",
                                                BasicType.LONG_TYPE)
                                        .nullable(false)
                                        .build())
                        .build();

        CatalogTable discoveredCatalogTable =
                CatalogTable.builder(
                                normalizedPath,
                                schema)
                        .comment("second discovery")
                        .build();

        CatalogTable preparedCatalogTable =
                CatalogTable.builder(
                                normalizedPath,
                                schema)
                        .comment("prepared metadata")
                        .build();

        JdbcSourceTable discoveredTable =
                JdbcSourceTable.builder()
                        .tablePath(configuredPath)
                        .catalogTable(discoveredCatalogTable)
                        .build();

        Map<TablePath, JdbcSourceTable> discoveredTables =
                new LinkedHashMap<TablePath, JdbcSourceTable>();
        discoveredTables.put(
                configuredPath,
                discoveredTable);

        Map<TablePath, CatalogTable> preparedTables =
                new LinkedHashMap<TablePath, CatalogTable>();
        preparedTables.put(
                normalizedPath,
                preparedCatalogTable);

        Map<TablePath, JdbcSourceTable> result =
                JdbcSourceSplitGenerator.reconcilePreparedTables(
                        discoveredTables,
                        preparedTables);

        assertFalse(result.containsKey(configuredPath));
        assertTrue(result.containsKey(normalizedPath));
        assertEquals(
                normalizedPath,
                result.get(normalizedPath).getTablePath());
        assertSame(
                preparedCatalogTable,
                result.get(normalizedPath).getCatalogTable());
    }
}
