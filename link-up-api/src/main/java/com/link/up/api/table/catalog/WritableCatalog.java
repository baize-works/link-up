package com.link.up.api.table.catalog;

import com.link.up.api.table.catalog.exception.*;

/**
 * 支持 DDL 操作的 Catalog。
 * <p>
 * Source 只依赖 Catalog；
 * 需要自动建表的 Sink 才依赖 WritableCatalog。
 */
public interface WritableCatalog extends Catalog {

    /**
     * 创建数据库。
     */
    void createDatabase(
            String databaseName,
            boolean ignoreIfExists)
            throws CatalogException,
            DatabaseAlreadyExistsException;

    /**
     * 删除数据库。
     */
    void dropDatabase(
            String databaseName,
            boolean ignoreIfNotExists)
            throws CatalogException,
            DatabaseNotFoundException;

    /**
     * 创建表。
     * <p>
     * 表路径直接从 CatalogTable 中获取，
     * 避免同时传入 TablePath 和 CatalogTable 导致不一致。
     */
    void createTable(
            CatalogTable table,
            boolean ignoreIfExists)
            throws CatalogException,
            DatabaseNotFoundException,
            TableAlreadyExistsException;

    /**
     * Adds a column without changing existing data. Implementations that do
     * not support schema evolution should fail explicitly instead of silently
     * accepting a schema that cannot receive the source rows.
     */
    default void addColumn(TablePath tablePath, Column column)
            throws CatalogException, TableNotFoundException {
        throw new UnsupportedOperationException("Catalog does not support ADD COLUMN: " + name());
    }

    /**
     * 删除表。
     */
    void dropTable(
            TablePath tablePath,
            boolean ignoreIfNotExists)
            throws CatalogException,
            TableNotFoundException;

    /**
     * 清空表数据，但保留表结构。
     */
    void truncateTable(
            TablePath tablePath,
            boolean ignoreIfNotExists)
            throws CatalogException,
            TableNotFoundException;
}
