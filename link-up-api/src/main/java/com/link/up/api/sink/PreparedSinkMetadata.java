package com.link.up.api.sink;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, connector-neutral result of sink preparation.
 */
public final class PreparedSinkMetadata {
    private final Map<TablePath, CatalogTable> targetTables;
    private final Map<TablePath, Object> attributes;
    private final Map<TablePath, TableDdl> tableDdls;

    public PreparedSinkMetadata(Map<TablePath, CatalogTable> targetTables) {
        this(
                targetTables,
                Collections.<TablePath, Object>emptyMap(),
                Collections.<TablePath, TableDdl>emptyMap());
    }

    public PreparedSinkMetadata(
            Map<TablePath, CatalogTable> targetTables,
            Map<TablePath, Object> attributes) {
        this(
                targetTables,
                attributes,
                Collections.<TablePath, TableDdl>emptyMap());
    }

    public PreparedSinkMetadata(
            Map<TablePath, CatalogTable> targetTables,
            Map<TablePath, Object> attributes,
            Map<TablePath, TableDdl> tableDdls) {
        this.targetTables = immutable(targetTables, "targetTables");
        this.attributes = immutable(attributes, "attributes");
        this.tableDdls = immutable(tableDdls, "tableDdls");
    }

    private static <T> Map<TablePath, T> immutable(
            Map<TablePath, T> input,
            String name) {
        Objects.requireNonNull(input, name + " must not be null");
        return Collections.unmodifiableMap(
                new LinkedHashMap<TablePath, T>(input));
    }

    public Map<TablePath, CatalogTable> getTargetTables() {
        return targetTables;
    }

    public CatalogTable getTargetTable(TablePath sourceTable) {
        return targetTables.get(sourceTable);
    }

    public Map<TablePath, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(TablePath sourceTable) {
        return attributes.get(sourceTable);
    }

    public Map<TablePath, TableDdl> getTableDdls() {
        return tableDdls;
    }

    public TableDdl getTableDdl(TablePath sourceTable) {
        return tableDdls.get(sourceTable);
    }
}
