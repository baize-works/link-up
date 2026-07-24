package com.link.up.connector.jdbc.utils;

import com.link.up.api.table.catalog.Catalog;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.catalog.TableSchema;
import com.link.up.api.table.catalog.exception.CatalogException;
import com.link.up.connector.jdbc.config.JdbcConnectionConfig;
import com.link.up.connector.jdbc.config.JdbcSourceConfig;
import com.link.up.connector.jdbc.config.JdbcSourceTableConfig;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.options.MultiTableFailurePolicy;
import com.link.up.connector.jdbc.source.JdbcSourceTable;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * JDBC Catalog 工具类。
 * <p>
 * 负责把 JDBC Source 配置转换成运行时使用的 JdbcSourceTable。
 * <p>
 * 当前支持：
 * <p>
 * 1. 单表元数据发现；
 * 2. 多表元数据发现；
 * 3. 自定义查询字段投影；
 * 4. 多表失败跳过策略；
 * 5. 分片参数组装。
 * <p>
 * 当前不支持：
 * <p>
 * 1. 正则表达式批量选表；
 * 2. 没有 table_path 的匿名查询；
 * 3. 自定义查询中的字段别名和表达式；
 * 4. 普通索引、外键等约束合并；
 * 5. Catalog 不存在时直接通过 JDBC 构造完整表结构。
 */
@Slf4j
public final class JdbcCatalogUtils {

    private JdbcCatalogUtils() {
    }

    /**
     * 根据 Source 配置加载表元数据。
     * <p>
     * 该方法负责创建、打开和关闭 Catalog。
     *
     * @param config  JDBC Source 配置
     * @param dialect JDBC 数据库方言
     * @return 按配置顺序排列的 Source 表
     */
    public static Map<TablePath, JdbcSourceTable> getTables(
            JdbcSourceConfig config,
            JdbcDialect dialect)
            throws Exception {

        Objects.requireNonNull(
                config,
                "config must not be null");

        Objects.requireNonNull(
                dialect,
                "dialect must not be null");

        /*
         * Catalog 的创建交给 Dialect。
         *
         * MySQL Dialect 创建 MySqlCatalog，
         * PostgreSQL Dialect 创建 PostgreSqlCatalog。
         */
        try (Catalog catalog =
                     dialect.createCatalog(
                             config.getConnectionConfig())) {

            catalog.open();

            return getTables(
                    config,
                    dialect,
                    catalog);
        }
    }

    /**
     * 使用已经创建的 Catalog 加载表元数据。
     * <p>
     * 该重载不会打开或关闭 Catalog，
     * 生命周期由调用方负责。
     */
    public static Map<TablePath, JdbcSourceTable> getTables(
            JdbcSourceConfig config,
            JdbcDialect dialect,
            Catalog catalog)
            throws Exception {

        Objects.requireNonNull(
                config,
                "config must not be null");

        Objects.requireNonNull(
                dialect,
                "dialect must not be null");

        Objects.requireNonNull(
                catalog,
                "catalog must not be null");

        Map<TablePath, JdbcSourceTable> result =
                new LinkedHashMap<>();

        MultiTableFailurePolicy failurePolicy =
                config.getMultiTableFailurePolicy();

        for (JdbcSourceTableConfig tableConfig :
                config.getTableConfigs()) {

            try {
                JdbcSourceTable sourceTable =
                        loadSourceTable(
                                config,
                                tableConfig,
                                dialect,
                                catalog);

                JdbcSourceTable previous =
                        result.put(
                                sourceTable.getTablePath(),
                                sourceTable);

                if (previous != null) {
                    throw new IllegalArgumentException(
                            "Source 表路径重复："
                                    + sourceTable.getTablePath());
                }

                log.info(
                        "JDBC Source 表结构加载完成，table={}, fields={}",
                        sourceTable.getTablePath(),
                        sourceTable
                                .getCatalogTable()
                                .getTableSchema()
                                .getColumnCount());

            } catch (Exception e) {
                if (!shouldContinue(failurePolicy)) {
                    throw wrapException(
                            tableConfig,
                            e);
                }

                log.warn(
                        "跳过加载失败的 JDBC Source 表，table={}, reason={}",
                        tableConfig.getTablePath(),
                        e.getMessage(),
                        e);
            }
        }

        if (result.isEmpty()) {
            throw new CatalogException(
                    "没有成功加载任何 JDBC Source 表");
        }

        log.info(
                "JDBC Source 表结构加载完成，success={}, configured={}",
                result.size(),
                config.getTableConfigs().size());

        return Collections.unmodifiableMap(result);
    }

    /**
     * 加载一张 Source 表。
     */
    private static JdbcSourceTable loadSourceTable(
            JdbcSourceConfig sourceConfig,
            JdbcSourceTableConfig tableConfig,
            JdbcDialect dialect,
            Catalog catalog)
            throws Exception {

        TablePath tablePath =
                parseTablePath(
                        tableConfig,
                        dialect);

        CatalogTable catalogTable =
                catalog.getTable(tablePath);

        /*
         * 自定义查询可能只读取物理表中的部分字段，
         * 此时需要按照查询字段顺序投影 CatalogTable。
         */
        if (tableConfig.hasCustomQuery()) {
            catalogTable =
                    projectCatalogTableByQuery(
                            sourceConfig.getConnectionConfig(),
                            catalogTable,
                            tableConfig.getQuery());
        }

        return JdbcSourceTable.builder()
                .tablePath(tablePath)
                .query(tableConfig.getQuery())
                .partitionColumn(
                        tableConfig.getPartitionColumn())
                .partitionNumber(
                        tableConfig.getPartitionNumber())
                .partitionStart(
                        tableConfig.getPartitionLowerBound())
                .partitionEnd(
                        tableConfig.getPartitionUpperBound())
                .catalogTable(catalogTable)
                .build();
    }

    /**
     * 解析并规范化表路径。
     * <p>
     * table_path 是必填项，即使配置了 query，也需要作为：
     * <p>
     * 1. Catalog 元数据标识；
     * 2. RecordBatch 数据集标识；
     * 3. 多表 Sink 路由标识。
     */
    private static TablePath parseTablePath(
            JdbcSourceTableConfig tableConfig,
            JdbcDialect dialect) {

        String tablePath =
                tableConfig.getTablePath();

        if (!hasText(tablePath)) {
            throw new IllegalArgumentException(
                    "table_path must not be empty");
        }

        TablePath parsed =
                dialect.parseTablePath(tablePath);

        if (parsed == null) {
            throw new IllegalArgumentException(
                    "无法解析 table_path："
                            + tablePath);
        }

        return parsed;
    }

    /**
     * 根据自定义查询结果投影 CatalogTable。
     * <p>
     * 当前查询字段必须来自 table_path 对应的物理表，并且字段标签必须和
     * 物理字段名称一致。
     * <p>
     * 支持：
     * <p>
     * SELECT id, name FROM user
     * SELECT name, id FROM user
     * <p>
     * 暂不支持：
     * <p>
     * SELECT id AS user_id FROM user
     * SELECT COUNT(*) AS total FROM user
     * SELECT CONCAT(first_name, last_name) AS name FROM user
     */
    private static CatalogTable projectCatalogTableByQuery(
            JdbcConnectionConfig connectionConfig,
            CatalogTable physicalTable,
            String query)
            throws Exception {

        validateQuery(query);

        List<String> queryFields =
                readQueryFields(
                        connectionConfig,
                        query);

        if (queryFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "无法从自定义查询中获取字段信息，table="
                            + physicalTable.getTablePath());
        }

        TableSchema physicalSchema =
                physicalTable.getTableSchema();

        for (String fieldName : queryFields) {
            if (!physicalSchema.contains(fieldName)) {
                throw new IllegalArgumentException(
                        "自定义查询字段不属于物理表，"
                                + "暂不支持字段别名或表达式，table="
                                + physicalTable.getTablePath()
                                + ", field="
                                + fieldName);
            }
        }

        TableSchema projectedSchema =
                physicalSchema.project(queryFields);

        return physicalTable.withSchema(
                projectedSchema);
    }

    /**
     * 读取自定义查询的结果字段。
     * <p>
     * 优先使用 PreparedStatement.getMetaData()，
     * 避免真正执行 SQL。
     * <p>
     * 部分 JDBC Driver 在未执行前不返回元数据，
     * 此时最多查询一行以获取 ResultSetMetaData。
     */
    private static List<String> readQueryFields(
            JdbcConnectionConfig config,
            String query)
            throws Exception {

        loadDriver(config);

        try (Connection connection =
                     DriverManager.getConnection(
                             config.getUrl(),
                             config.toProperties());
             PreparedStatement statement =
                     connection.prepareStatement(query)) {

            ResultSetMetaData metadata =
                    statement.getMetaData();

            if (metadata != null) {
                return readFieldNames(metadata);
            }

            /*
             * 查询包含占位符时无法安全执行，
             * 因为 Catalog 阶段没有参数值。
             */
            if (query.indexOf('?') >= 0) {
                throw new IllegalArgumentException(
                        "自定义查询包含参数占位符，"
                                + "Catalog 阶段无法获取结果结构："
                                + query);
            }

            statement.setMaxRows(1);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return readFieldNames(
                        resultSet.getMetaData());
            }

        } catch (SQLException e) {
            throw new CatalogException(
                    "获取自定义查询字段结构失败，sql="
                            + abbreviate(query, 300),
                    e);
        }
    }

    /**
     * 从查询结果元数据读取字段标签。
     * <p>
     * 优先使用 columnLabel，可以正确反映查询返回的字段名。
     */
    private static List<String> readFieldNames(
            ResultSetMetaData metadata)
            throws SQLException {

        int columnCount =
                metadata.getColumnCount();

        List<String> fields =
                new ArrayList<>(columnCount);

        Set<String> uniqueFields =
                new HashSet<>();

        for (int i = 1; i <= columnCount; i++) {
            String fieldName =
                    normalize(
                            metadata.getColumnLabel(i));

            if (fieldName == null) {
                fieldName =
                        normalize(
                                metadata.getColumnName(i));
            }

            if (fieldName == null) {
                throw new IllegalArgumentException(
                        "查询结果第 "
                                + i
                                + " 个字段没有名称");
            }

            if (!uniqueFields.add(fieldName)) {
                throw new IllegalArgumentException(
                        "查询结果中存在重复字段："
                                + fieldName);
            }

            fields.add(fieldName);
        }

        return fields;
    }

    /**
     * 自定义查询仅允许 SELECT 或 WITH 查询。
     * <p>
     * Catalog 阶段不能执行更新、删除或 DDL。
     */
    private static void validateQuery(
            String query) {

        String sql = normalize(query);

        if (sql == null) {
            throw new IllegalArgumentException(
                    "query must not be empty");
        }

        String lower =
                sql.toLowerCase(Locale.ROOT);

        if (!lower.startsWith("select ")
                && !lower.startsWith("select\n")
                && !lower.startsWith("select\t")
                && !lower.startsWith("with ")
                && !lower.startsWith("with\n")
                && !lower.startsWith("with\t")) {

            throw new IllegalArgumentException(
                    "JDBC Source query 只允许 SELECT 或 WITH 查询");
        }
    }

    private static void loadDriver(
            JdbcConnectionConfig config)
            throws ClassNotFoundException {

        String driverName =
                config.getDriverName();

        if (hasText(driverName)) {
            Class.forName(driverName);
        }
    }

    /**
     * 判断多表失败后是否继续处理其他表。
     */
    private static boolean shouldContinue(
            MultiTableFailurePolicy policy) {

        return policy != null
                && policy.continueOtherTables();
    }

    private static RuntimeException wrapException(
            JdbcSourceTableConfig tableConfig,
            Exception error) {

        if (error instanceof RuntimeException) {
            return (RuntimeException) error;
        }

        return new CatalogException(
                "加载 JDBC Source 表失败，table="
                        + tableConfig.getTablePath(),
                error);
    }

    private static String abbreviate(
            String value,
            int maxLength) {

        if (value == null
                || value.length() <= maxLength) {

            return value;
        }

        return value.substring(
                0,
                maxLength)
                + "...";
    }

    private static boolean hasText(
            String value) {

        return normalize(value) != null;
    }

    private static String normalize(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
