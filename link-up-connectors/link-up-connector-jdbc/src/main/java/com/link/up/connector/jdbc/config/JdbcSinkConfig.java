package com.link.up.connector.jdbc.config;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.connector.jdbc.sink.DataSaveMode;
import com.link.up.connector.jdbc.sink.DirtyDataOutputType;
import com.link.up.connector.jdbc.sink.DirtyDataPolicy;
import com.link.up.connector.jdbc.sink.SchemaSaveMode;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JDBC 离线 Sink 运行配置。
 * <p>
 * Sink 固定使用显式事务：
 * <p>
 * 1. connection.setAutoCommit(false)；
 * 2. executeBatch；
 * 3. commit；
 * 4. 失败时 rollback。
 * <p>
 * 每个 SinkTask 独立提交；普通 JDBC Sink 不提供 Job 级原子性或全局回滚。
 * <p>
 * 因此不再对外暴露 auto_commit、XA、Exactly Once 等配置。
 */
@Getter
public final class JdbcSinkConfig
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private final JdbcConnectionConfig connectionConfig;

    /**
     * 目标表或多表目标模板。
     * <p>
     * 为空时默认沿用 Source TablePath。
     */
    private final String targetTablePath;

    private final SchemaSaveMode schemaSaveMode;
    private final DataSaveMode dataSaveMode;
    private final JdbcWriteMode writeMode;

    private final String customSql;
    private final List<String> primaryKeys;

    private final int batchSize;
    private final int preparedStatementCacheSize;
    private final int queryTimeoutSec;
    private final int maxRetries;
    private final DirtyDataPolicy dirtyDataPolicy;
    private final DirtyDataOutputType dirtyDataOutputType;
    private final String dirtyDataOutputPath;
    private final int dirtyDataMaxSamples;
    private final long dirtyDataMaxCount;
    private final double dirtyDataMaxPercentage;
    private final boolean createPrimaryKey;

    private JdbcSinkConfig(
            JdbcConnectionConfig connectionConfig,
            String targetTablePath,
            SchemaSaveMode schemaSaveMode,
            DataSaveMode dataSaveMode,
            JdbcWriteMode writeMode,
            String customSql,
            List<String> primaryKeys,
            int batchSize,
            int preparedStatementCacheSize,
            int queryTimeoutSec,
            int maxRetries,
            DirtyDataPolicy dirtyDataPolicy,
            DirtyDataOutputType dirtyDataOutputType, String dirtyDataOutputPath, int dirtyDataMaxSamples, long dirtyDataMaxCount, double dirtyDataMaxPercentage,
            boolean createPrimaryKey) {

        this.connectionConfig =
                Objects.requireNonNull(
                        connectionConfig,
                        "connectionConfig must not be null");

        this.targetTablePath =
                normalize(targetTablePath);

        this.schemaSaveMode =
                Objects.requireNonNull(
                        schemaSaveMode,
                        "schemaSaveMode must not be null");

        this.dataSaveMode =
                Objects.requireNonNull(
                        dataSaveMode,
                        "dataSaveMode must not be null");

        this.writeMode =
                Objects.requireNonNull(
                        writeMode,
                        "writeMode must not be null");

        this.customSql = normalize(customSql);

        List<String> safePrimaryKeys =
                primaryKeys == null
                        ? Collections.emptyList()
                        : normalizePrimaryKeys(
                        primaryKeys);

        this.primaryKeys =
                Collections.unmodifiableList(
                        safePrimaryKeys);

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than 0");
        }

        if (preparedStatementCacheSize <= 0) {
            throw new IllegalArgumentException(
                    "preparedStatementCacheSize must be greater than 0");
        }

        if (queryTimeoutSec < 0) {
            throw new IllegalArgumentException(
                    "queryTimeoutSec must not be negative");
        }

        if (maxRetries < 0) {
            throw new IllegalArgumentException(
                    "maxRetries must not be negative");
        }

        this.batchSize = batchSize;
        this.preparedStatementCacheSize = preparedStatementCacheSize;
        this.queryTimeoutSec = queryTimeoutSec;
        this.maxRetries = maxRetries;
        this.dirtyDataPolicy = Objects.requireNonNull(dirtyDataPolicy, "dirtyDataPolicy must not be null");
        this.dirtyDataOutputType = Objects.requireNonNull(dirtyDataOutputType, "dirtyDataOutputType must not be null");
        this.dirtyDataOutputPath = normalize(dirtyDataOutputPath);
        if (dirtyDataMaxSamples < 0 || dirtyDataMaxCount < 0 || dirtyDataMaxPercentage < 0D || dirtyDataMaxPercentage > 1D)
            throw new IllegalArgumentException("Invalid dirty-data limits");
        if (dirtyDataOutputType == DirtyDataOutputType.JSONL && this.dirtyDataOutputPath == null)
            throw new IllegalArgumentException("dirty_data_output_path is required for JSONL output");
        this.dirtyDataMaxSamples = dirtyDataMaxSamples;
        this.dirtyDataMaxCount = dirtyDataMaxCount;
        this.dirtyDataMaxPercentage = dirtyDataMaxPercentage;
        this.createPrimaryKey =
                createPrimaryKey;
    }

    public static JdbcSinkConfig of(
            ReadonlyConfig config) {

        return new JdbcSinkConfig(
                JdbcConnectionConfig.of(config),
                config.getOptional(
                        JdbcSinkOptions.TABLE_PATH)
                        .orElse(null),
                config.get(
                        JdbcSinkOptions
                                .SCHEMA_SAVE_MODE),
                config.get(
                        JdbcSinkOptions.DATA_SAVE_MODE),
                config.get(
                        JdbcSinkOptions.WRITE_MODE),
                config.getOptional(
                        JdbcSinkOptions.CUSTOM_SQL)
                        .orElse(null),
                config.getOptional(
                        JdbcSinkOptions.PRIMARY_KEYS)
                        .orElse(Collections.emptyList()),
                config.get(
                        JdbcSinkOptions.BATCH_SIZE),
                config.get(
                        JdbcSinkOptions.PREPARED_STATEMENT_CACHE_SIZE),
                config.get(
                        JdbcSinkOptions.QUERY_TIMEOUT_SEC),
                config.get(
                        JdbcSinkOptions.MAX_RETRIES),
                config.get(JdbcSinkOptions.DIRTY_DATA_POLICY),
                config.get(JdbcSinkOptions.DIRTY_DATA_OUTPUT_TYPE), config.getOptional(JdbcSinkOptions.DIRTY_DATA_OUTPUT_PATH).orElse(null), config.get(JdbcSinkOptions.DIRTY_DATA_MAX_SAMPLES), config.get(JdbcSinkOptions.DIRTY_DATA_MAX_COUNT).longValue(), config.get(JdbcSinkOptions.DIRTY_DATA_MAX_PERCENTAGE),
                config.get(
                        JdbcSinkOptions
                                .CREATE_PRIMARY_KEY));
    }

    private static List<String> normalizePrimaryKeys(
            List<String> primaryKeys) {

        List<String> result =
                new ArrayList<>(
                        primaryKeys.size());

        for (String primaryKey : primaryKeys) {
            String normalized =
                    normalize(primaryKey);

            if (normalized == null) {
                throw new IllegalArgumentException(
                        "primary key must not be empty");
            }

            if (result.contains(normalized)) {
                throw new IllegalArgumentException(
                        "primary key 不允许重复："
                                + normalized);
            }

            result.add(normalized);
        }

        return result;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    public boolean isUpsert() {
        return writeMode
                == JdbcWriteMode.UPSERT;
    }

    public boolean hasCustomSql() {
        // A configured table is the authoritative routing instruction for a
        // multi-table job.  In that case SQL must be generated for each
        // resolved target table instead of reusing a statement that can only
        // address one table.
        return customSql != null && targetTablePath == null;
    }

    /**
     * Resolves the configured target-table template for one source table.
     *
     * <p>{@code ${table_name}} is replaced with the source table name. {@code
     * ${schema_name}} is replaced with the source schema; for two-part paths
     * (the usual MySQL {@code database.table} form), the database is used as
     * the schema name. A target table without placeholders remains a fixed
     * target, which is useful for single-table jobs.
     *
     * @param sourceTablePath the table that produced the current batch
     * @return the configured and expanded target table path, or {@code null}
     * when the source path should be retained
     */
    public String resolveTargetTablePath(TablePath sourceTablePath) {
        if (targetTablePath == null) {
            return null;
        }
        Objects.requireNonNull(sourceTablePath, "sourceTablePath must not be null");

        String schemaName = sourceTablePath.getSchemaName();
        if (schemaName == null) {
            schemaName = sourceTablePath.getDatabaseName();
        }
        if (schemaName == null) {
            schemaName = "";
        }

        return targetTablePath
                .replace("${schema_name}", schemaName)
                .replace("${table_name}", sourceTablePath.getTableName());
    }

    public boolean hasConfiguredPrimaryKeys() {
        return !primaryKeys.isEmpty();
    }

    public boolean shouldSkipDirtyData() {
        return dirtyDataPolicy == DirtyDataPolicy.SKIP;
    }
}
