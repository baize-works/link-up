package com.link.up.connector.jdbc.catalog.mysql;

import com.link.up.api.table.catalog.*;
import com.link.up.api.table.catalog.exception.*;
import com.link.up.connector.jdbc.catalog.AbstractJdbcCatalog;
import com.link.up.connector.jdbc.catalog.JdbcCatalogConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MySQL Catalog。
 * <p>
 * 当前支持：
 * <p>
 * 1. 数据库发现；
 * 2. 数据表发现；
 * 3. 表结构读取；
 * 4. 主键读取；
 * 5. 表引擎、字符集和排序规则读取；
 * 6. 建库、删库；
 * 7. 建表、删表、清表。
 */
public final class MySqlCatalog
        extends AbstractJdbcCatalog
        implements WritableCatalog {

    public static final String TABLE_OPTION_DIALECT =
            "dialect";

    public static final String TABLE_OPTION_ENGINE =
            "engine";

    public static final String TABLE_OPTION_CHARSET =
            "charset";

    public static final String TABLE_OPTION_COLLATE =
            "collate";

    private static final String LIST_DATABASES_SQL =
            "SELECT SCHEMA_NAME "
                    + "FROM INFORMATION_SCHEMA.SCHEMATA "
                    + "ORDER BY SCHEMA_NAME";

    private static final String DATABASE_EXISTS_SQL =
            "SELECT 1 "
                    + "FROM INFORMATION_SCHEMA.SCHEMATA "
                    + "WHERE SCHEMA_NAME = ?";

    private static final String LIST_TABLES_SQL =
            "SELECT TABLE_NAME "
                    + "FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_SCHEMA = ? "
                    + "AND TABLE_TYPE = 'BASE TABLE' "
                    + "ORDER BY TABLE_NAME";

    private static final String TABLE_EXISTS_SQL =
            "SELECT 1 "
                    + "FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_SCHEMA = ? "
                    + "AND TABLE_NAME = ? "
                    + "AND TABLE_TYPE = 'BASE TABLE'";

    private static final String SELECT_COLUMNS_SQL =
            "SELECT "
                    + "COLUMN_NAME, "
                    + "DATA_TYPE, "
                    + "COLUMN_TYPE, "
                    + "CHARACTER_MAXIMUM_LENGTH, "
                    + "NUMERIC_PRECISION, "
                    + "NUMERIC_SCALE, "
                    + "DATETIME_PRECISION, "
                    + "IS_NULLABLE, "
                    + "COLUMN_DEFAULT, "
                    + "EXTRA, "
                    + "COLUMN_COMMENT, "
                    + "CHARACTER_SET_NAME, "
                    + "COLLATION_NAME "
                    + "FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = ? "
                    + "AND TABLE_NAME = ? "
                    + "ORDER BY ORDINAL_POSITION";

    private static final String SELECT_PRIMARY_KEY_SQL =
            "SELECT "
                    + "CONSTRAINT_NAME, "
                    + "COLUMN_NAME "
                    + "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
                    + "WHERE TABLE_SCHEMA = ? "
                    + "AND TABLE_NAME = ? "
                    + "AND CONSTRAINT_NAME = 'PRIMARY' "
                    + "ORDER BY ORDINAL_POSITION";

    private static final String SELECT_TABLE_META_SQL =
            "SELECT "
                    + "TABLE_COMMENT, "
                    + "ENGINE, "
                    + "TABLE_COLLATION "
                    + "FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_SCHEMA = ? "
                    + "AND TABLE_NAME = ?";

    private final MySqlTypeMapper typeMapper;

    public MySqlCatalog(
            String catalogName,
            JdbcCatalogConfig config) {

        super(catalogName, config);

        this.typeMapper =
                new MySqlTypeMapper(
                        config.isIntTypeNarrowing());
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

    private static boolean hasText(
            String value) {

        return normalize(value) != null;
    }

    @Override
    public List<String> listDatabases()
            throws CatalogException {

        checkOpened();

        try (Connection connection =
                     openRootConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             LIST_DATABASES_SQL);
             ResultSet resultSet =
                     statement.executeQuery()) {

            List<String> databases =
                    new ArrayList<>();

            while (resultSet.next()) {
                databases.add(
                        resultSet.getString(1));
            }

            return databases;

        } catch (SQLException e) {
            throw new CatalogException(
                    "获取 MySQL 数据库列表失败",
                    e);
        }
    }

    /**
     * MySQL 中 database 和 schema 是同一个概念，
     * 因此不单独返回 Schema。
     */
    @Override
    public List<String> listSchemas(
            String databaseName) {

        return Collections.emptyList();
    }

    @Override
    public List<TablePath> listTables(
            String databaseName,
            String schemaName)
            throws CatalogException {

        checkOpened();

        String database =
                resolveDatabaseName(
                        databaseName,
                        schemaName);

        if (!databaseExists(database)) {
            throw new DatabaseNotFoundException(
                    catalogName,
                    database);
        }

        try (Connection connection =
                     openRootConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             LIST_TABLES_SQL)) {

            statement.setString(
                    1,
                    database);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                List<TablePath> tables =
                        new ArrayList<>();

                while (resultSet.next()) {
                    tables.add(
                            TablePath.of(
                                    database,
                                    resultSet.getString(
                                            "TABLE_NAME")));
                }

                return tables;
            }

        } catch (SQLException e) {
            throw new CatalogException(
                    "获取 MySQL 表列表失败，database="
                            + database,
                    e);
        }
    }

    public boolean databaseExists(
            String databaseName)
            throws CatalogException {

        if (!hasText(databaseName)) {
            return false;
        }

        checkOpened();

        try (Connection connection =
                     openRootConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             DATABASE_EXISTS_SQL)) {

            statement.setString(
                    1,
                    databaseName);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new CatalogException(
                    "检查 MySQL 数据库是否存在失败，database="
                            + databaseName,
                    e);
        }
    }

    @Override
    public boolean tableExists(
            TablePath tablePath)
            throws CatalogException {

        checkOpened();

        TablePath normalized =
                normalizeTablePath(tablePath);

        try (Connection connection =
                     openRootConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             TABLE_EXISTS_SQL)) {

            statement.setString(
                    1,
                    normalized.getDatabaseName());

            statement.setString(
                    2,
                    normalized.getTableName());

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new CatalogException(
                    "检查 MySQL 表是否存在失败，table="
                            + normalized,
                    e);
        }
    }

    @Override
    public CatalogTable getTable(
            TablePath tablePath)
            throws CatalogException,
            TableNotFoundException {

        checkOpened();

        TablePath normalized =
                normalizeTablePath(tablePath);

        try (Connection connection =
                     openRootConnection()) {

            List<Column> columns =
                    readColumns(
                            connection,
                            normalized);

            if (columns.isEmpty()) {
                throw new TableNotFoundException(
                        catalogName,
                        normalized);
            }

            PrimaryKey primaryKey =
                    readPrimaryKey(
                            connection,
                            normalized);

            TableMeta tableMeta =
                    readTableMeta(
                            connection,
                            normalized);

            TableSchema tableSchema =
                    TableSchema.builder()
                            .columns(columns)
                            .primaryKey(primaryKey)
                            .build();

            CatalogTable.Builder builder =
                    CatalogTable.builder(
                            normalized,
                            tableSchema)
                            .option(
                                    TABLE_OPTION_DIALECT,
                                    "mysql");

            if (tableMeta != null) {
                builder.comment(
                        tableMeta.comment);

                if (hasText(tableMeta.engine)) {
                    builder.option(
                            TABLE_OPTION_ENGINE,
                            tableMeta.engine);
                }

                if (hasText(tableMeta.collation)) {
                    builder.option(
                            TABLE_OPTION_COLLATE,
                            tableMeta.collation);

                    int separator =
                            tableMeta.collation
                                    .indexOf('_');

                    if (separator > 0) {
                        builder.option(
                                TABLE_OPTION_CHARSET,
                                tableMeta.collation
                                        .substring(
                                                0,
                                                separator));
                    }
                }
            }

            return builder.build();

        } catch (TableNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new CatalogException(
                    "获取 MySQL 表结构失败，table="
                            + normalized,
                    e);
        }
    }

    @Override
    public void createDatabase(
            String databaseName,
            boolean ignoreIfExists)
            throws CatalogException,
            DatabaseAlreadyExistsException {

        checkOpened();

        if (databaseExists(databaseName)) {
            if (ignoreIfExists) {
                return;
            }

            throw new DatabaseAlreadyExistsException(
                    catalogName,
                    databaseName);
        }

        String sql =
                "CREATE DATABASE "
                        + quoteIdentifier(databaseName);

        try (Connection connection =
                     openRootConnection()) {

            execute(connection, sql);

        } catch (SQLException e) {
            throw new CatalogException(
                    "创建 MySQL 数据库失败，database="
                            + databaseName,
                    e);
        }
    }

    @Override
    public void dropDatabase(
            String databaseName,
            boolean ignoreIfNotExists)
            throws CatalogException,
            DatabaseNotFoundException {

        checkOpened();

        if (!databaseExists(databaseName)) {
            if (ignoreIfNotExists) {
                return;
            }

            throw new DatabaseNotFoundException(
                    catalogName,
                    databaseName);
        }

        String sql =
                "DROP DATABASE "
                        + quoteIdentifier(databaseName);

        try (Connection connection =
                     openRootConnection()) {

            execute(connection, sql);

        } catch (SQLException e) {
            throw new CatalogException(
                    "删除 MySQL 数据库失败，database="
                            + databaseName,
                    e);
        }
    }

    @Override
    public void createTable(
            CatalogTable table,
            boolean ignoreIfExists)
            throws CatalogException,
            DatabaseNotFoundException,
            TableAlreadyExistsException {

        checkOpened();

        TablePath tablePath =
                normalizeTablePath(
                        table.getTablePath());

        String databaseName =
                tablePath.getDatabaseName();

        if (!databaseExists(databaseName)) {
            throw new DatabaseNotFoundException(
                    catalogName,
                    databaseName);
        }

        if (tableExists(tablePath)) {
            if (ignoreIfExists) {
                return;
            }

            throw new TableAlreadyExistsException(
                    catalogName,
                    tablePath);
        }

        String sql =
                new MySqlCreateTableSqlBuilder(
                        tablePath,
                        table,
                        typeMapper)
                        .build();

        try (Connection connection =
                     openDatabaseConnection(
                             databaseName)) {

            execute(connection, sql);

        } catch (SQLException e) {
            throw new CatalogException(
                    "创建 MySQL 表失败，table="
                            + tablePath,
                    e);
        }
    }

    @Override
    public void addColumn(TablePath tablePath, Column column)
            throws CatalogException, TableNotFoundException {
        checkOpened();
        TablePath normalized = normalizeTablePath(tablePath);
        if (!tableExists(normalized)) {
            throw new TableNotFoundException(catalogName, normalized);
        }
        CatalogTable table = getTable(normalized);
        String definition = new MySqlCreateTableSqlBuilder(normalized, table, typeMapper)
                .buildColumnDefinition(column);
        String sql = "ALTER TABLE " + quoteTable(normalized) + " ADD COLUMN " + definition;
        try (Connection connection = openDatabaseConnection(normalized.getDatabaseName())) {
            execute(connection, sql);
        } catch (SQLException e) {
            throw new CatalogException("增加 MySQL 字段失败，table=" + normalized
                    + ", column=" + column.getName(), e);
        }
    }

    @Override
    public void dropTable(
            TablePath tablePath,
            boolean ignoreIfNotExists)
            throws CatalogException,
            TableNotFoundException {

        checkOpened();

        TablePath normalized =
                normalizeTablePath(tablePath);

        if (!tableExists(normalized)) {
            if (ignoreIfNotExists) {
                return;
            }

            throw new TableNotFoundException(
                    catalogName,
                    normalized);
        }

        String sql =
                "DROP TABLE "
                        + quoteTable(normalized);

        try (Connection connection =
                     openDatabaseConnection(
                             normalized.getDatabaseName())) {

            execute(connection, sql);

        } catch (SQLException e) {
            throw new CatalogException(
                    "删除 MySQL 表失败，table="
                            + normalized,
                    e);
        }
    }

    @Override
    public void truncateTable(
            TablePath tablePath,
            boolean ignoreIfNotExists)
            throws CatalogException,
            TableNotFoundException {

        checkOpened();

        TablePath normalized =
                normalizeTablePath(tablePath);

        if (!tableExists(normalized)) {
            if (ignoreIfNotExists) {
                return;
            }

            throw new TableNotFoundException(
                    catalogName,
                    normalized);
        }

        String sql =
                "TRUNCATE TABLE "
                        + quoteTable(normalized);

        try (Connection connection =
                     openDatabaseConnection(
                             normalized.getDatabaseName())) {

            execute(connection, sql);

        } catch (SQLException e) {
            throw new CatalogException(
                    "清空 MySQL 表失败，table="
                            + normalized,
                    e);
        }
    }

    private List<Column> readColumns(
            Connection connection,
            TablePath tablePath)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             SELECT_COLUMNS_SQL)) {

            statement.setString(
                    1,
                    tablePath.getDatabaseName());

            statement.setString(
                    2,
                    tablePath.getTableName());

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                List<Column> columns =
                        new ArrayList<>();

                while (resultSet.next()) {
                    columns.add(
                            typeMapper.toColumn(
                                    resultSet));
                }

                return columns;
            }
        }
    }

    private PrimaryKey readPrimaryKey(
            Connection connection,
            TablePath tablePath)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             SELECT_PRIMARY_KEY_SQL)) {

            statement.setString(
                    1,
                    tablePath.getDatabaseName());

            statement.setString(
                    2,
                    tablePath.getTableName());

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                String primaryKeyName = null;

                List<String> columns =
                        new ArrayList<>();

                while (resultSet.next()) {
                    if (primaryKeyName == null) {
                        primaryKeyName =
                                resultSet.getString(
                                        "CONSTRAINT_NAME");
                    }

                    columns.add(
                            resultSet.getString(
                                    "COLUMN_NAME"));
                }

                if (columns.isEmpty()) {
                    return null;
                }

                return PrimaryKey.of(
                        primaryKeyName,
                        columns);
            }
        }
    }

    private TableMeta readTableMeta(
            Connection connection,
            TablePath tablePath)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             SELECT_TABLE_META_SQL)) {

            statement.setString(
                    1,
                    tablePath.getDatabaseName());

            statement.setString(
                    2,
                    tablePath.getTableName());

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return new TableMeta(
                        normalize(
                                resultSet.getString(
                                        "TABLE_COMMENT")),
                        normalize(
                                resultSet.getString(
                                        "ENGINE")),
                        normalize(
                                resultSet.getString(
                                        "TABLE_COLLATION")));
            }
        }
    }

    private String resolveDatabaseName(
            String databaseName,
            String schemaName) {

        if (hasText(databaseName)) {
            return databaseName.trim();
        }

        if (hasText(schemaName)) {
            return schemaName.trim();
        }

        return getDefaultDatabase()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "没有指定 MySQL 数据库"));
    }

    private static final class TableMeta {

        private final String comment;
        private final String engine;
        private final String collation;

        private TableMeta(
                String comment,
                String engine,
                String collation) {

            this.comment = comment;
            this.engine = engine;
            this.collation = collation;
        }
    }
}
