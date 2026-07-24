package com.link.up.connector.jdbc.core.dialect;

import com.link.up.api.table.catalog.Catalog;
import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.connector.jdbc.config.JdbcConnectionConfig;
import com.link.up.connector.jdbc.config.ReadConsistency;
import com.link.up.connector.jdbc.core.converter.JdbcRowConverter;
import com.link.up.connector.jdbc.core.split.StringRangeSplitDecision;
import com.link.up.connector.jdbc.source.JdbcSourceTable;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 离线 JDBC 数据库方言。
 * <p>
 * 方言对象应当保持无状态和不可变。
 * <p>
 * 当前只提供离线读取、批量写入、表路径解析、类型映射和 Catalog
 * 创建所需能力，不包含 CDC Schema Change、动态采样和实时事件处理。
 */
public interface JdbcDialect extends Serializable {

    static void validateFields(
            List<String> fieldNames) {

        if (fieldNames == null
                || fieldNames.isEmpty()) {

            throw new IllegalArgumentException(
                    "fieldNames must not be empty");
        }

        for (String fieldName : fieldNames) {
            if (!hasText(fieldName)) {
                throw new IllegalArgumentException(
                        "fieldName must not be empty");
            }
        }
    }

    static Set<String> normalizeFields(
            List<String> fields) {

        if (fields == null) {
            return Collections.emptySet();
        }

        return fields.stream()
                .filter(JdbcDialect::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    static boolean hasText(String value) {
        return value != null
                && !value.trim().isEmpty();
    }

    /**
     * 方言标识。
     */
    String name();

    /**
     * 创建当前数据库对应的 Catalog。
     */
    Catalog createCatalog(
            String catalogName,
            JdbcConnectionConfig connectionConfig);

    default Catalog createCatalog(
            JdbcConnectionConfig connectionConfig) {

        return createCatalog(
                name(),
                connectionConfig);
    }

    /**
     * JDBC 查询结果和 Flux 类型转换器。
     */
    JdbcTypeMapper typeMapper();

    /**
     * JDBC 行读取和写入转换器。
     */
    JdbcRowConverter rowConverter();

    /**
     * Returns the read consistency modes implemented by this dialect.
     */
    default Set<ReadConsistency> supportedReadConsistencies() {
        return Collections.singleton(ReadConsistency.BEST_EFFORT);
    }

    /**
     * Configures a connection for a requested snapshot read. Dialects that
     * support database-coordinated snapshots should override this method.
     */
    default void configureSnapshotConnection(
            Connection connection,
            ReadConsistency consistency) throws SQLException {

        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        if (consistency != ReadConsistency.SINGLE_CONNECTION_SNAPSHOT) {
            throw new UnsupportedOperationException(
                    "Read consistency is not supported by dialect " + name());
        }
        connection.setReadOnly(true);
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        connection.setAutoCommit(false);
    }

    /**
     * 解析配置中的表路径。
     */
    default TablePath parseTablePath(
            String tablePath) {

        return TablePath.parse(tablePath);
    }

    /**
     * 引用字段或表标识。
     */
    default String quoteIdentifier(
            String identifier) {

        if (identifier == null
                || identifier.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "identifier must not be empty");
        }

        return identifier.trim();
    }

    /**
     * 构造数据库表标识。
     */
    default String tableIdentifier(
            TablePath tablePath) {

        if (tablePath == null) {
            throw new IllegalArgumentException(
                    "tablePath must not be null");
        }

        StringBuilder result =
                new StringBuilder();

        if (hasText(tablePath.getDatabaseName())) {
            result.append(
                    quoteIdentifier(
                            tablePath.getDatabaseName()))
                    .append('.');
        }

        if (hasText(tablePath.getSchemaName())) {
            result.append(
                    quoteIdentifier(
                            tablePath.getSchemaName()))
                    .append('.');
        }

        result.append(
                quoteIdentifier(
                        tablePath.getTableName()));

        return result.toString();
    }

    /**
     * 为物理表生成 SELECT SQL。
     * <p>
     * 自定义 query 直接使用配置值。
     */
    default String buildSelectSql(
            JdbcSourceTable table,
            String whereCondition) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "table must not be null");
        }

        if (hasText(table.getQuery())) {
            return JdbcSqlUtils.removeTrailingSemicolon(
                    table.getQuery());
        }

        List<Column> columns =
                table.getCatalogTable()
                        .getTableSchema()
                        .getColumns();

        String fields =
                columns.stream()
                        .map(Column::getName)
                        .map(this::quoteIdentifier)
                        .collect(Collectors.joining(", "));

        StringBuilder sql =
                new StringBuilder()
                        .append("SELECT ")
                        .append(fields)
                        .append(" FROM ")
                        .append(
                                tableIdentifier(
                                        table.getTablePath()));

        if (hasText(whereCondition)) {
            sql.append(" WHERE ")
                    .append(whereCondition.trim());
        }

        return sql.toString();
    }

    /**
     * Builds portable MIN/MAX/COUNT statistics SQL. Custom queries are safely wrapped.
     */
    default String buildSplitStatisticsSql(JdbcSourceTable table, boolean includeCount) {
        String source;
        if (hasText(table.getQuery())) {
            source = "(" + JdbcSqlUtils.removeTrailingSemicolon(table.getQuery()) + ") " + quoteIdentifier("flux_statistics");
        } else {
            source = tableIdentifier(table.getTablePath());
        }
        String column = quoteIdentifier(table.getPartitionColumn());
        return "SELECT MIN(" + column + ") AS flux_min, MAX(" + column + ") AS flux_max"
                + (includeCount ? ", COUNT(*) AS flux_count" : "") + " FROM " + source;
    }

    /**
     * Optional dialect-specific sampled statistics SQL.
     */
    default Optional<String> buildSampleSplitStatisticsSql(JdbcSourceTable table, int sampleSize) {
        return Optional.empty();
    }

    /**
     * 生成标准 JDBC INSERT SQL。
     * <p>
     * PreparedStatement 使用 ? 占位符，不使用命名参数。
     */
    default String buildInsertSql(
            TablePath tablePath,
            List<String> fieldNames) {

        validateFields(fieldNames);

        String fields =
                fieldNames.stream()
                        .map(this::quoteIdentifier)
                        .collect(Collectors.joining(", "));

        String placeholders =
                fieldNames.stream()
                        .map(field -> "?")
                        .collect(Collectors.joining(", "));

        return "INSERT INTO "
                + tableIdentifier(tablePath)
                + " ("
                + fields
                + ") VALUES ("
                + placeholders
                + ")";
    }

    /**
     * 生成数据库原生 UPSERT SQL。
     * <p>
     * 不支持时返回 Optional.empty()。
     */
    default Optional<String> buildUpsertSql(
            TablePath tablePath,
            List<String> fieldNames,
            List<String> primaryKeys) {

        return Optional.empty();
    }

    /**
     * 创建只读查询 PreparedStatement。
     */
    default PreparedStatement prepareReadStatement(
            Connection connection,
            String sql,
            int fetchSize)
            throws SQLException {

        PreparedStatement statement =
                connection.prepareStatement(
                        sql,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);

        if (fetchSize > 0) {
            statement.setFetchSize(fetchSize);
        }

        return statement;
    }

    /**
     * 统计 Source 表或自定义查询行数。
     * <p>
     * 该能力只用于显式分片分析，不应默认执行。
     */
    default long countRows(
            Connection connection,
            JdbcSourceTable table)
            throws SQLException {

        if (hasText(table.getQuery())) {
            return JdbcSqlUtils.countSubquery(
                    connection,
                    table.getQuery());
        }

        return JdbcSqlUtils.countTable(
                connection,
                tableIdentifier(
                        table.getTablePath()));
    }

    /**
     * 方言建议的默认连接属性。
     * <p>
     * ConnectionProvider 创建连接时应先加入这些默认值，
     * 再使用用户 properties 覆盖。
     */
    default Map<String, String>
    defaultConnectionProperties() {

        return Collections.emptyMap();
    }

    /**
     * 合并方言默认连接参数和用户连接参数。
     */
    default Map<String, String>
    resolveConnectionProperties(
            Map<String, String> userProperties) {

        Map<String, String> result =
                new LinkedHashMap<>(
                        defaultConnectionProperties());

        if (userProperties != null) {
            result.putAll(userProperties);
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * 判断字符串范围分片是否与数据库实际排序规则一致。
     *
     * <p>默认拒绝 RANGE，防止 Java 字典序和数据库排序规则不一致导致漏数或重复。
     */
    default StringRangeSplitDecision validateStringRangeSplit(
            JdbcSourceTable table,
            Column column,
            String lowerBound,
            String upperBound) {

        return StringRangeSplitDecision.unsafe(
                "dialect has not verified a binary/ASCII-compatible collation");
    }

    /**
     * 构造 HASH 分片谓词。
     *
     * <p>返回内容应当是完整布尔表达式，例如：
     *
     * <pre>
     * MOD(CRC32(CAST(`id` AS CHAR)), 4) = 0
     * </pre>
     *
     * <p>默认不支持 HASH 分片。
     */
    default Optional<String> buildHashPartitionPredicate(
            Column column,
            int bucket,
            int bucketCount) {

        return Optional.empty();
    }
}
