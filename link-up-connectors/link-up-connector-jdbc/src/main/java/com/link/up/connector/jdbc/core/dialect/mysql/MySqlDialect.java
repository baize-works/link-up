package com.link.up.connector.jdbc.core.dialect.mysql;

import com.link.up.api.table.catalog.Catalog;
import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.connector.jdbc.catalog.JdbcCatalogConfig;
import com.link.up.connector.jdbc.catalog.mysql.MySqlCatalog;
import com.link.up.connector.jdbc.config.JdbcConnectionConfig;
import com.link.up.connector.jdbc.config.ReadConsistency;
import com.link.up.connector.jdbc.core.converter.JdbcRowConverter;
import com.link.up.connector.jdbc.core.dialect.DatabaseIdentifier;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.core.dialect.JdbcTypeMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MySQL 离线 JDBC 方言。
 */
public final class MySqlDialect
        implements JdbcDialect {

    private final JdbcConnectionConfig connectionConfig;
    private final MySqlTypeMapper typeMapper;

    public MySqlDialect(
            JdbcConnectionConfig connectionConfig) {

        if (connectionConfig == null) {
            throw new IllegalArgumentException(
                    "connectionConfig must not be null");
        }

        this.connectionConfig =
                connectionConfig;

        this.typeMapper =
                new MySqlTypeMapper(false);
    }

    @Override
    public String name() {
        return DatabaseIdentifier.MYSQL;
    }

    @Override
    public Catalog createCatalog(
            String catalogName,
            JdbcConnectionConfig connectionConfig) {

        return new MySqlCatalog(
                catalogName,
                new JdbcCatalogConfig(
                        connectionConfig.getUrl(),
                        connectionConfig.getUsername(),
                        connectionConfig.getPassword(),
                        connectionConfig.getDriverName(),
                        connectionConfig.getProperties(),
                        false));
    }

    @Override
    public JdbcTypeMapper typeMapper() {
        return typeMapper;
    }

    @Override
    public JdbcRowConverter rowConverter() {
        return new MySqlJdbcRowConverter();
    }

    @Override
    public Set<ReadConsistency> supportedReadConsistencies() {
        return EnumSet.of(
                ReadConsistency.BEST_EFFORT,
                ReadConsistency.SINGLE_CONNECTION_SNAPSHOT);
    }

    @Override
    public TablePath parseTablePath(
            String tablePath) {

        return TablePath.parse(tablePath);
    }

    @Override
    public String quoteIdentifier(
            String identifier) {

        if (!JdbcDialect.hasText(identifier)) {
            throw new IllegalArgumentException(
                    "identifier must not be empty");
        }

        return "`"
                + identifier.trim()
                .replace("`", "``")
                + "`";
    }

    @Override
    public String tableIdentifier(
            TablePath tablePath) {

        String database =
                tablePath.getDatabaseName();

        if (!JdbcDialect.hasText(database)) {
            database = connectionConfig.getSchema();
        }

        if (!JdbcDialect.hasText(database)) {
            database = databaseFromUrl(
                    connectionConfig.getUrl());
        }

        if (!JdbcDialect.hasText(database)) {
            throw new IllegalArgumentException(
                    "MySQL 表路径缺少 database，且 JDBC URL 未指定默认数据库："
                            + tablePath);
        }

        return quoteIdentifier(database)
                + "."
                + quoteIdentifier(
                tablePath.getTableName());
    }

    private static String databaseFromUrl(
            String url) {

        if (!JdbcDialect.hasText(url)) {
            return null;
        }

        String normalized = url.trim();
        int protocolSeparator =
                normalized.indexOf("://");

        if (protocolSeparator < 0) {
            return null;
        }

        int databaseStart =
                normalized.indexOf(
                        '/',
                        protocolSeparator + 3);

        if (databaseStart < 0
                || databaseStart
                == normalized.length() - 1) {
            return null;
        }

        int databaseEnd =
                normalized.length();

        int queryStart =
                normalized.indexOf(
                        '?',
                        databaseStart + 1);

        if (queryStart >= 0) {
            databaseEnd = queryStart;
        }

        int fragmentStart =
                normalized.indexOf(
                        '#',
                        databaseStart + 1);

        if (fragmentStart >= 0
                && fragmentStart < databaseEnd) {
            databaseEnd = fragmentStart;
        }

        String database =
                normalized.substring(
                        databaseStart + 1,
                        databaseEnd)
                        .trim();

        return JdbcDialect.hasText(database)
                ? database
                : null;
    }

    @Override
    public Optional<String> buildUpsertSql(
            TablePath tablePath,
            List<String> fieldNames,
            List<String> primaryKeys) {

        JdbcDialect.validateFields(fieldNames);

        Set<String> primaryKeySet =
                JdbcDialect.normalizeFields(
                        primaryKeys);

        if (primaryKeySet.isEmpty()) {
            throw new IllegalArgumentException(
                    "MySQL UPSERT 必须配置主键字段");
        }

        List<String> updateFields =
                fieldNames.stream()
                        .filter(
                                field ->
                                        !primaryKeySet.contains(
                                                field))
                        .collect(Collectors.toList());

        /*
         * 当所有字段都是主键时，使用第一个主键进行无操作更新，
         * 保证 SQL 语法合法。
         */
        if (updateFields.isEmpty()) {
            updateFields =
                    Collections.singletonList(
                            primaryKeys.get(0));
        }

        String updateClause =
                updateFields.stream()
                        .map(
                                field ->
                                        quoteIdentifier(field)
                                                + " = VALUES("
                                                + quoteIdentifier(field)
                                                + ")")
                        .collect(Collectors.joining(", "));

        return Optional.of(
                buildInsertSql(
                        tablePath,
                        fieldNames)
                        + " ON DUPLICATE KEY UPDATE "
                        + updateClause);
    }

    @Override
    public PreparedStatement prepareReadStatement(
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

    @Override
    public Map<String, String>
    defaultConnectionProperties() {

        Map<String, String> result =
                new LinkedHashMap<>();

        result.put(
                "rewriteBatchedStatements",
                "true");

        /*
         * 配合正数 fetchSize 使用服务端游标读取大表。
         * 用户可通过 properties 覆盖。
         */
        result.put(
                "useCursorFetch",
                "true");

        /*
         * tinyint(1) 是否映射 Boolean 由 MySqlTypeMapper 决定。
         */
        result.put(
                "tinyInt1isBit",
                "false");

        return Collections.unmodifiableMap(result);
    }

    @Override
    public Optional<String> buildHashPartitionPredicate(
            Column column,
            int bucket,
            int bucketCount) {

        if (bucketCount <= 0) {
            throw new IllegalArgumentException(
                    "bucketCount must be greater than 0");
        }

        if (bucket < 0 || bucket >= bucketCount) {
            throw new IllegalArgumentException(
                    "bucket must be between 0 and bucketCount - 1");
        }

        String field = quoteIdentifier(column.getName());

        return Optional.of(
                "MOD(CRC32(CAST("
                        + field
                        + " AS CHAR)), "
                        + bucketCount
                        + ") = "
                        + bucket);
    }
}
