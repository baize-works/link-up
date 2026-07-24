package com.link.up.connector.jdbc.sink;

import com.link.up.api.dirtydata.DirtyDataCollector;
import com.link.up.api.dirtydata.DirtyDataContext;
import com.link.up.api.dirtydata.DirtyDataSummary;
import com.link.up.api.dirtydata.DirtyRecord;
import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.catalog.TableSchema;
import com.link.up.api.table.type.FluxRow;
import com.link.up.connector.jdbc.config.JdbcSinkConfig;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.core.dialect.JdbcDialectLoader;
import com.link.up.connector.jdbc.internal.JdbcConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC 批量输出组件。
 *
 * <p>一个 JdbcOutputFormat 对应一个 SinkTask，
 * 同一个 SinkTask 的所有 DML 使用同一条 JDBC 事务。
 *
 * <p>事务生命周期：
 *
 * <pre>
 * open
 *   -> write batch 1
 *   -> write batch 2
 *   -> commit
 *
 * 发生异常：
 *
 * open
 *   -> write batch 1
 *   -> write batch 2 failed
 *   -> rollback
 * </pre>
 */
public final class JdbcOutputFormat
        implements AutoCloseable {

    private static final Logger LOG =
            LoggerFactory.getLogger(JdbcOutputFormat.class);

    private final JdbcSinkConfig config;

    private final JdbcDialect dialect;

    private final JdbcConnectionProvider connectionProvider;

    private final PreparedSinkMetadata metadata;
    /**
     * Statements are reused across internal batches and bounded for multi-table jobs.
     */
    private final Map<TablePath, CachedStatement> preparedStatementCache =
            new LinkedHashMap<TablePath, CachedStatement>(16, 0.75F, true);
    private DirtyDataCollector dirtyDataCollector;
    private DirtyDataContext dirtyDataContext;
    private Connection connection;
    private Boolean supportsSavepoints;

    private boolean opened;

    private boolean transactionCompleted;
    private boolean committed;

    private boolean closed;

    JdbcOutputFormat(
            JdbcSinkConfig config,
            PreparedSinkMetadata metadata) {

        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");

        this.config =
                Objects.requireNonNull(
                        config,
                        "config must not be null");

        this.dialect =
                JdbcDialectLoader.load(
                        config.getConnectionConfig());

        this.connectionProvider =
                new JdbcConnectionProvider(
                        config.getConnectionConfig(),
                        dialect);
    }

    private static String sanitize(String message) {
        return message == null ? "" : message.replaceAll("(?i)(password|secret|token)=[^\\s,;]+", "$1=***");
    }

    /**
     * 初始化输出组件。
     *
     * <p>这里暂时不立即创建连接，避免空任务无意义地连接数据库。
     * 第一批数据写入时才真正开启事务。
     */
    public void setDirtyDataCollector(DirtyDataCollector collector, DirtyDataContext context) {
        this.dirtyDataCollector = collector;
        this.dirtyDataContext = context;
    }

    public void updateDirtyDataContext(DirtyDataContext context) {
        this.dirtyDataContext = context;
    }

    public void open() throws Exception {
        if (closed) {
            throw new IllegalStateException(
                    "JdbcOutputFormat has already been closed");
        }

        if (opened) {
            throw new IllegalStateException(
                    "JdbcOutputFormat has already been opened");
        }

        opened = true;
        if (dirtyDataCollector != null) dirtyDataCollector.open();
    }

    /**
     * 写入一批数据。
     *
     * <p>这里只执行 SQL，不提交事务。
     * 最终提交由 commit 方法统一完成。
     */
    public void write(
            List<FluxRow> rows,
            CatalogTable sourceTable)
            throws Exception {

        checkWritable();

        if (rows == null || rows.isEmpty()) {
            return;
        }

        Objects.requireNonNull(
                sourceTable,
                "sourceTable must not be null");

        CatalogTable targetTable = metadata.getTargetTable(sourceTable.getTablePath());
        if (targetTable == null) {
            throw new IllegalArgumentException("Source table was not prepared: " + sourceTable.getTablePath());
        }

        ensureTransactionConnection();

        List<String> fields =
                targetTable
                        .getTableSchema()
                        .getColumns()
                        .stream()
                        .map(column -> column.getName())
                        .collect(Collectors.toList());

        String sql =
                createWriteSql(
                        targetTable,
                        fields);

        for (int start = 0;
             start < rows.size();
             start += config.getBatchSize()) {

            int end =
                    Math.min(
                            start + config.getBatchSize(),
                            rows.size());

            if (dirtyDataCollector != null) dirtyDataCollector.recordAttempt(end - start);
            executeBatch(
                    targetTable.getTablePath(),
                    sql,
                    rows.subList(start, end),
                    targetTable.getTableSchema());
        }
    }

    /**
     * 提交当前 SinkTask 的完整事务。
     */
    public void commit() throws Exception {
        checkOpened();

        if (transactionCompleted) {
            return;
        }

        /*
         * 空任务可能从未创建 JDBC 连接。
         */
        if (connection != null) {
            connection.commit();
        }

        transactionCompleted = true;
        committed = true;
    }

    /**
     * 回滚当前 SinkTask 的完整事务。
     */
    public void rollback() throws Exception {
        if (!opened || transactionCompleted) {
            return;
        }

        Exception rollbackFailure = null;

        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (Exception exception) {
            rollbackFailure = exception;
        } finally {
            transactionCompleted = true;
        }

        if (rollbackFailure != null) {
            throw rollbackFailure;
        }
    }

    public DirtyDataSummary getDirtyDataSummary() {
        return dirtyDataCollector == null ? DirtyDataSummary.empty() : dirtyDataCollector.summary();
    }

    /**
     * 执行一个 JDBC Batch。
     *
     * <p>整个 SinkTask 共用一条事务，因此批次重试不能执行完整 rollback。
     * 这里通过 Savepoint 只回滚当前失败批次。
     */
    private void executeBatch(
            TablePath tablePath,
            String sql,
            List<FluxRow> rows,
            TableSchema schema)
            throws Exception {

        ensureTransactionConnection();

        try {
            executeRows(tablePath, sql, rows, schema);
        } catch (Exception batchFailure) {
            if (!config.shouldSkipDirtyData()) {
                throw batchFailure;
            }

            if (!isTransactionConnectionValid()) {
                throw batchFailure;
            }

            /*
             * The failed batch has already been rolled back to its savepoint.
             * Replay it one row at a time so valid rows remain in the task
             * transaction and only the rows that still fail are discarded.
             */
            for (FluxRow row : rows) {
                try {
                    executeRows(tablePath, sql, Collections.singletonList(row), schema);
                } catch (Exception rowFailure) {
                    if (!isTransactionConnectionValid()) {
                        throw rowFailure;
                    }
                    if (dirtyDataCollector != null) {
                        DirtyDataContext context = dirtyDataContext == null ? new DirtyDataContext(null, null, "jdbc", null, null) : dirtyDataContext;
                        dirtyDataCollector.collect(DirtyRecord.from(rowFailure, sanitize(rowFailure.getMessage()), context));
                        if (dirtyDataCollector.summary().isThresholdExceeded())
                            throw new IllegalStateException("Dirty-data threshold exceeded");
                    }
                    LOG.warn(
                            "Skipping dirty JDBC row because dirty_data_policy=SKIP; table={}, row={}",
                            tablePath,
                            row,
                            rowFailure);
                }
            }
        }
    }

    private void executeRows(
            TablePath tablePath,
            String sql,
            List<FluxRow> rows,
            TableSchema schema)
            throws Exception {

        int maxRetries = config.getMaxRetries();
        boolean requiresSavepoint = maxRetries > 0 || config.shouldSkipDirtyData();

        if (requiresSavepoint
                && !supportsSavepoints()) {

            throw new IllegalStateException(
                    "当前 JDBC 数据库或驱动不支持 Savepoint，"
                            + "无法在任务级事务中安全执行批次重试或脏数据隔离。"
                            + "请关闭重试和脏数据跳过，"
                            + "或者使用支持 Savepoint 的 JDBC 驱动");
        }

        Exception lastFailure = null;

        for (int attempt = 0;
             attempt <= maxRetries;
             attempt++) {

            Savepoint savepoint = null;

            /*
             * 配置了批次重试时，为当前批次创建保存点。
             */
            if (requiresSavepoint) {
                savepoint =
                        connection.setSavepoint(
                                "link_up_batch_"
                                        + attempt);
            }

            try {
                PreparedStatement statement = getPreparedStatement(tablePath, sql);
                statement.clearBatch();

                for (FluxRow row : rows) {
                    dialect.rowConverter().write(statement, row, schema);
                    statement.addBatch();
                }

                statement.executeBatch();
                statement.clearBatch();
                releaseSavepointQuietly(savepoint, null);
                return;

            } catch (Exception exception) {
                lastFailure = exception;

                /*
                 * 没有 Savepoint 时不能在当前事务内安全重试。
                 * 将异常交给 SinkTask，由 SinkTask 回滚整个事务。
                 */
                if (savepoint == null) {
                    throw exception;
                }

                try {
                    connection.rollback(savepoint);
                } catch (Exception rollbackFailure) {
                    exception.addSuppressed(
                            rollbackFailure);

                    throw exception;
                } finally {
                    releaseSavepointQuietly(
                            savepoint,
                            exception);
                }

                /*
                 * 事务连接已经断开时，原事务已经无法继续。
                 *
                 * 不能重新建立连接后继续写，因为新连接不包含前面批次
                 * 尚未提交的事务数据。
                 */
                if (!isTransactionConnectionValid()) {
                    SQLException connectionFailure =
                            new SQLException(
                                    "JDBC transaction connection was lost; "
                                            + "the whole SinkTask must be rolled back "
                                            + "and restarted",
                                    exception);

                    throw connectionFailure;
                }

                if (attempt >= maxRetries) {
                    throw exception;
                }
            }
        }

        throw lastFailure == null
                ? new IllegalStateException(
                "JDBC batch execution failed")
                : lastFailure;
    }

    private PreparedStatement getPreparedStatement(
            TablePath tablePath, String sql) throws SQLException {
        CachedStatement cached = preparedStatementCache.get(tablePath);
        if (cached != null && cached.sql.equals(sql) && !cached.statement.isClosed()) {
            return cached.statement;
        }
        if (cached != null) {
            closeStatementQuietly(cached.statement);
            preparedStatementCache.remove(tablePath);
        }
        PreparedStatement statement = connection.prepareStatement(sql);
        if (config.getQueryTimeoutSec() > 0) {
            statement.setQueryTimeout(config.getQueryTimeoutSec());
        }
        preparedStatementCache.put(tablePath, new CachedStatement(sql, statement));
        evictPreparedStatements();
        return statement;
    }

    private void evictPreparedStatements() {
        while (preparedStatementCache.size() > config.getPreparedStatementCacheSize()) {
            TablePath eldest = preparedStatementCache.keySet().iterator().next();
            CachedStatement evicted = preparedStatementCache.remove(eldest);
            closeStatementQuietly(evicted.statement);
        }
    }

    private void closePreparedStatements() {
        for (CachedStatement cached : preparedStatementCache.values()) {
            closeStatementQuietly(cached.statement);
        }
        preparedStatementCache.clear();
    }

    private void closeStatementQuietly(PreparedStatement statement) {
        try {
            statement.close();
        } catch (SQLException ignored) {
            // Connection close is still attempted below.
        }
    }

    /**
     * 生成目标端写入 SQL。
     */
    private String createWriteSql(
            CatalogTable targetTable,
            List<String> fields) {

        if (config.hasCustomSql()) {
            return config.getCustomSql();
        }

        if (config.isUpsert()) {
            return dialect
                    .buildUpsertSql(
                            targetTable.getTablePath(),
                            fields,
                            resolvePreparedPrimaryKeys(targetTable))
                    .orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Dialect does not support UPSERT: "
                                            + dialect.name()));
        }

        return dialect.buildInsertSql(
                targetTable.getTablePath(),
                fields);
    }

    @SuppressWarnings("unchecked")
    private List<String> resolvePreparedPrimaryKeys(CatalogTable targetTable) {
        for (Map.Entry<TablePath, CatalogTable> entry : metadata.getTargetTables().entrySet()) {
            if (entry.getValue().getTablePath().equals(targetTable.getTablePath())) {
                Object keys = metadata.getAttribute(entry.getKey());
                return keys instanceof List ? (List<String>) keys : Collections.<String>emptyList();
            }
        }
        return Collections.emptyList();
    }

    /**
     * 获取或检查当前事务连接。
     *
     * <p>事务已经开始后，不允许自动重连并继续写入。
     * 因为新连接无法继承原连接中尚未提交的数据。
     */
    private void ensureTransactionConnection()
            throws Exception {

        if (connection == null) {
            connection =
                    connectionProvider
                            .getOrEstablishConnection();

            connection.setAutoCommit(false);

            return;
        }

        if (!isTransactionConnectionValid()) {
            throw new SQLException(
                    "JDBC transaction connection is invalid; "
                            + "the transaction cannot continue");
        }
    }

    private boolean isTransactionConnectionValid() {
        try {
            return connection != null
                    && !connection.isClosed()
                    && connectionProvider
                    .isConnectionValid();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean supportsSavepoints()
            throws SQLException {

        if (supportsSavepoints == null) {
            supportsSavepoints =
                    connection
                            .getMetaData()
                            .supportsSavepoints();
        }

        return supportsSavepoints;
    }

    private void releaseSavepointQuietly(
            Savepoint savepoint,
            Exception originalFailure) {

        if (savepoint == null
                || connection == null) {
            return;
        }

        try {
            connection.releaseSavepoint(
                    savepoint);
        } catch (Exception releaseFailure) {
            /*
             * releaseSavepoint 失败通常不影响事务正确性。
             * 如果当前已经有主异常，则保留为 suppressed。
             */
            if (originalFailure != null) {
                originalFailure.addSuppressed(
                        releaseFailure);
            }
        }
    }

    private void checkOpened() {
        if (!opened) {
            throw new IllegalStateException(
                    "JdbcOutputFormat has not been opened");
        }

        if (closed) {
            throw new IllegalStateException(
                    "JdbcOutputFormat has already been closed");
        }
    }

    private void checkWritable() {
        checkOpened();

        if (transactionCompleted) {
            throw new IllegalStateException(
                    "JDBC transaction has already been completed");
        }
    }

    /**
     * 关闭连接。
     *
     * <p>如果调用方忘记 commit 或 rollback，
     * close 会尽量回滚尚未完成的事务，防止意外提交。
     */
    @Override
    public void close() throws Exception {
        if (closed) {
            return;
        }

        closed = true;

        Exception failure = null;

        if (opened && !transactionCompleted) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (Exception rollbackFailure) {
                failure = rollbackFailure;
            } finally {
                transactionCompleted = true;
            }
        }

        try {
            closePreparedStatements();
            connectionProvider.closeConnection();
        } catch (Exception closeFailure) {
            if (failure == null) {
                failure = closeFailure;
            } else {
                failure.addSuppressed(
                        closeFailure);
            }
        } finally {
            connection = null;
        }

        if (dirtyDataCollector != null) {
            try {
                dirtyDataCollector.close(failure == null && committed);
            } catch (Exception collectorFailure) {
                if (failure == null) failure = collectorFailure;
                else failure.addSuppressed(collectorFailure);
            }
        }
        if (failure != null) throw failure;
    }

    private static final class CachedStatement {
        private final String sql;
        private final PreparedStatement statement;

        private CachedStatement(String sql, PreparedStatement statement) {
            this.sql = sql;
            this.statement = statement;
        }
    }
}
