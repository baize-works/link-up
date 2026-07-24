package com.link.up.api.table.catalog;

import com.link.up.api.table.catalog.exception.CatalogException;
import com.link.up.api.table.catalog.exception.TableNotFoundException;

import java.util.*;

/**
 * 离线表 Catalog。
 * <p>
 * Catalog 只负责数据库和表元数据发现。
 * <p>
 * 多表失败策略、配置解析、日志记录等逻辑不属于 Catalog，
 * 应由 JdbcSource 或任务校验层处理。
 */
public interface Catalog extends AutoCloseable {

    /**
     * Catalog 名称。
     * <p>
     * 例如：
     * <p>
     * mysql
     * postgresql
     * oracle
     */
    String name();

    /**
     * 打开 Catalog 使用的连接或其他资源。
     */
    void open() throws CatalogException;

    /**
     * 获取默认数据库。
     * <p>
     * 部分数据库可能没有默认数据库，因此使用 Optional。
     */
    default Optional<String> getDefaultDatabase()
            throws CatalogException {

        return Optional.empty();
    }

    /**
     * 获取所有数据库。
     */
    List<String> listDatabases()
            throws CatalogException;

    /**
     * 获取指定数据库下的所有 Schema。
     * <p>
     * MySQL 等没有独立 Schema 概念的数据库可以返回空集合。
     */
    default List<String> listSchemas(
            String databaseName)
            throws CatalogException {

        return Collections.emptyList();
    }

    /**
     * 获取指定数据库和 Schema 下的表路径。
     * <p>
     * schemaName 可以为空。
     */
    List<TablePath> listTables(
            String databaseName,
            String schemaName)
            throws CatalogException;

    /**
     * 判断表是否存在。
     */
    boolean tableExists(TablePath tablePath)
            throws CatalogException;

    /**
     * 获取一张表的完整元数据。
     */
    CatalogTable getTable(TablePath tablePath)
            throws CatalogException,
            TableNotFoundException;

    /**
     * 获取多张表的元数据。
     * <p>
     * 默认采用严格模式：
     * 任意一张表获取失败，立即抛出异常。
     * <p>
     * 跳过失败表的策略应在 JdbcSource 层处理，
     * 不应污染 Catalog 接口。
     */
    default CatalogTables getTables(
            Collection<TablePath> tablePaths)
            throws CatalogException {

        if (tablePaths == null
                || tablePaths.isEmpty()) {

            throw new IllegalArgumentException(
                    "tablePaths must not be empty");
        }

        List<CatalogTable> tables =
                new ArrayList<>(tablePaths.size());

        for (TablePath tablePath : tablePaths) {
            tables.add(getTable(tablePath));
        }

        return CatalogTables.of(tables);
    }

    @Override
    void close() throws CatalogException;
}