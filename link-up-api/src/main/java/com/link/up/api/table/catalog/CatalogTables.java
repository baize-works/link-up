package com.link.up.api.table.catalog;

import java.io.Serializable;
import java.util.*;

/**
 * 单表或多表 Catalog 元数据集合。
 * <p>
 * 单表场景 size() == 1；
 * 多表场景 size() > 1。
 */
public final class CatalogTables
        implements Iterable<CatalogTable>, Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<TablePath, CatalogTable> tables;

    private CatalogTables(
            Collection<CatalogTable> catalogTables) {

        if (catalogTables == null
                || catalogTables.isEmpty()) {

            throw new IllegalArgumentException(
                    "catalogTables must not be empty");
        }

        Map<TablePath, CatalogTable> safeTables =
                new LinkedHashMap<>();

        for (CatalogTable table : catalogTables) {
            Objects.requireNonNull(
                    table,
                    "catalogTable must not be null");

            CatalogTable oldTable =
                    safeTables.put(
                            table.getTablePath(),
                            table);

            if (oldTable != null) {
                throw new IllegalArgumentException(
                        "Duplicate table path: "
                                + table.getTablePath());
            }
        }

        this.tables =
                Collections.unmodifiableMap(safeTables);
    }

    public static CatalogTables of(
            Collection<CatalogTable> tables) {

        return new CatalogTables(tables);
    }

    public static CatalogTables single(
            CatalogTable table) {

        return new CatalogTables(
                Collections.singletonList(table));
    }

    public int size() {
        return tables.size();
    }

    public boolean isSingleTable() {
        return tables.size() == 1;
    }

    public boolean contains(TablePath tablePath) {
        return tables.containsKey(tablePath);
    }

    public CatalogTable get(TablePath tablePath) {
        CatalogTable table = tables.get(tablePath);

        if (table == null) {
            throw new IllegalArgumentException(
                    "Cannot find table: " + tablePath);
        }

        return table;
    }

    /**
     * 单表任务获取唯一表。
     */
    public CatalogTable getSingleTable() {
        if (!isSingleTable()) {
            throw new IllegalStateException(
                    "Expected single table, actual table count: "
                            + tables.size());
        }

        return tables.values()
                .iterator()
                .next();
    }

    public List<TablePath> getTablePaths() {
        return Collections.unmodifiableList(
                new ArrayList<>(tables.keySet()));
    }

    public List<CatalogTable> getTables() {
        return Collections.unmodifiableList(
                new ArrayList<>(tables.values()));
    }

    @Override
    public Iterator<CatalogTable> iterator() {
        return tables.values().iterator();
    }

    @Override
    public String toString() {
        return "CatalogTables{"
                + "tables="
                + tables.keySet()
                + '}';
    }
}