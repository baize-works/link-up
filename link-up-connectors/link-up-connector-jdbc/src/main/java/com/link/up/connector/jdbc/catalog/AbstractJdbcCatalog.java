package com.link.up.connector.jdbc.catalog;

import com.link.up.api.table.catalog.Catalog;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.catalog.exception.CatalogException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

/**
 * JDBC Catalog 基础实现。
 * <p>
 * 主要负责：
 * <p>
 * 1. JDBC Driver 加载；
 * 2. JDBC URL 解析；
 * 3. 连接创建；
 * 4. Catalog 生命周期校验；
 * 5. 通用 SQL 执行。
 * <p>
 * 不缓存 Connection，避免：
 * <p>
 * 1. 长连接失效；
 * 2. 多线程共享 Connection；
 * 3. Catalog 长时间占用数据库资源。
 */
@Slf4j
public abstract class AbstractJdbcCatalog
        implements Catalog {

    protected final String catalogName;
    protected final JdbcCatalogConfig config;
    protected final JdbcUrlInfo urlInfo;

    private volatile boolean opened;

    protected AbstractJdbcCatalog(
            String catalogName,
            JdbcCatalogConfig config) {

        if (catalogName == null
                || catalogName.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "catalogName must not be empty");
        }

        this.catalogName = catalogName.trim();
        this.config = config;
        this.urlInfo =
                JdbcUrlInfo.parseMySql(config.getUrl());
    }

    protected static String quoteIdentifier(
            String identifier) {

        if (identifier == null
                || identifier.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "identifier must not be empty");
        }

        return "`"
                + identifier.replace("`", "``")
                + "`";
    }

    protected static String quoteTable(
            TablePath tablePath) {

        return quoteIdentifier(
                tablePath.getDatabaseName())
                + "."
                + quoteIdentifier(
                tablePath.getTableName());
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public String name() {
        return catalogName;
    }

    @Override
    public Optional<String> getDefaultDatabase() {
        return Optional.ofNullable(
                urlInfo.getDefaultDatabase());
    }

    @Override
    public synchronized void open()
            throws CatalogException {

        if (opened) {
            return;
        }

        loadDriver();

        try (Connection connection =
                     newConnection(config.getUrl())) {

            if (!connection.isValid(5)) {
                throw new CatalogException(
                        "JDBC Catalog 连接校验失败：" + config.getUrl());
            }

            opened = true;

            log.info(
                    "JDBC Catalog 已连接，catalog={}, url={}",
                    catalogName,
                    config.getUrl());

        } catch (SQLException e) {
            throw new CatalogException(
                    "JDBC Catalog 连接失败：" + config.getUrl(),
                    e);
        }
    }

    @Override
    public synchronized void close() {
        /*
         * 当前实现不缓存 Connection，
         * 因此没有需要主动释放的长期资源。
         */
        opened = false;

        log.info(
                "JDBC Catalog 已关闭，catalog={}",
                catalogName);
    }

    /**
     * 创建连接到 JDBC URL 中默认数据库的连接。
     */
    protected final Connection openDefaultConnection()
            throws SQLException {

        checkOpened();
        return newConnection(config.getUrl());
    }

    /**
     * 创建不指定数据库的连接。
     * <p>
     * 主要用于：
     * <p>
     * 1. 查询数据库列表；
     * 2. 创建数据库；
     * 3. 删除数据库；
     * 4. 查询 information_schema。
     */
    protected final Connection openRootConnection()
            throws SQLException {

        checkOpened();
        return newConnection(urlInfo.getRootUrl());
    }

    /**
     * 创建指定数据库连接。
     */
    protected final Connection openDatabaseConnection(
            String databaseName)
            throws SQLException {

        checkOpened();

        return newConnection(
                urlInfo.buildDatabaseUrl(databaseName));
    }

    protected final String getDatabaseUrl(
            String databaseName) {

        return urlInfo.buildDatabaseUrl(databaseName);
    }

    /**
     * 获取 TablePath 中的数据库。
     * <p>
     * 如果没有显式指定，则使用 JDBC URL 中的默认数据库。
     */
    protected final String resolveDatabase(
            TablePath tablePath) {

        String databaseName =
                normalize(tablePath.getDatabaseName());

        /*
         * MySQL 中 database 和 schema 是同一个概念。
         * 为兼容使用 schemaName 的调用方，这里允许使用 schemaName。
         */
        if (databaseName == null) {
            databaseName =
                    normalize(tablePath.getSchemaName());
        }

        if (databaseName == null) {
            databaseName =
                    normalize(urlInfo.getDefaultDatabase());
        }

        if (databaseName == null) {
            throw new IllegalArgumentException(
                    "没有指定数据库，table="
                            + tablePath
                            + "，JDBC URL 中也没有默认数据库");
        }

        return databaseName;
    }

    /**
     * 将 TablePath 规范化为 MySQL database.table。
     */
    protected final TablePath normalizeTablePath(
            TablePath tablePath) {

        return TablePath.of(
                resolveDatabase(tablePath),
                tablePath.getTableName());
    }

    /**
     * 执行 DDL 或普通 SQL。
     */
    protected final void execute(
            Connection connection,
            String sql)
            throws SQLException {

        log.info("执行 Catalog SQL：{}", sql);

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.execute();
        }
    }

    protected final Connection newConnection(
            String url)
            throws SQLException {

        Properties properties =
                config.toConnectionProperties();

        return DriverManager.getConnection(
                url,
                properties);
    }

    private void loadDriver() {
        String driverClass =
                config.getDriverClass();

        if (driverClass == null) {
            return;
        }

        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new CatalogException(
                    "找不到 JDBC Driver：" + driverClass,
                    e);
        }
    }

    protected final void checkOpened() {
        if (!opened) {
            throw new IllegalStateException(
                    "Catalog 尚未打开，请先调用 open()");
        }
    }

    /**
     * JDBC URL 解析结果。
     */
    protected static final class JdbcUrlInfo {

        private final String rootUrl;
        private final String defaultDatabase;
        private final String suffix;

        private JdbcUrlInfo(
                String rootUrl,
                String defaultDatabase,
                String suffix) {

            this.rootUrl = rootUrl;
            this.defaultDatabase = defaultDatabase;
            this.suffix = suffix;
        }

        public static JdbcUrlInfo parseMySql(
                String jdbcUrl) {

            if (jdbcUrl == null
                    || !jdbcUrl.startsWith("jdbc:mysql://")) {

                throw new IllegalArgumentException(
                        "非法 MySQL JDBC URL：" + jdbcUrl);
            }

            int queryIndex =
                    jdbcUrl.indexOf('?');

            String mainUrl =
                    queryIndex >= 0
                            ? jdbcUrl.substring(0, queryIndex)
                            : jdbcUrl;

            String suffix =
                    queryIndex >= 0
                            ? jdbcUrl.substring(queryIndex)
                            : "";

            int hostStart =
                    "jdbc:mysql://".length();

            int databaseSeparator =
                    mainUrl.indexOf('/', hostStart);

            if (databaseSeparator < 0) {
                return new JdbcUrlInfo(
                        mainUrl + "/",
                        null,
                        suffix);
            }

            String rootUrl =
                    mainUrl.substring(
                            0,
                            databaseSeparator + 1);

            String database =
                    mainUrl.substring(
                            databaseSeparator + 1);

            if (database.trim().isEmpty()) {
                database = null;
            }

            return new JdbcUrlInfo(
                    rootUrl,
                    database,
                    suffix);
        }

        public String getRootUrl() {
            return rootUrl + suffix;
        }

        public String getDefaultDatabase() {
            return defaultDatabase;
        }

        public String buildDatabaseUrl(
                String databaseName) {

            if (databaseName == null
                    || databaseName.trim().isEmpty()) {

                return getRootUrl();
            }

            return rootUrl
                    + databaseName.trim()
                    + suffix;
        }
    }
}