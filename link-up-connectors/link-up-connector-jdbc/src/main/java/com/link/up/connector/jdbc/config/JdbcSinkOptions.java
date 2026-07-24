package com.link.up.connector.jdbc.config;

import com.link.up.api.configuration.Option;
import com.link.up.api.configuration.Options;
import com.link.up.connector.jdbc.sink.DataSaveMode;
import com.link.up.connector.jdbc.sink.DirtyDataOutputType;
import com.link.up.connector.jdbc.sink.DirtyDataPolicy;
import com.link.up.connector.jdbc.sink.SchemaSaveMode;

import java.util.List;

/**
 * JDBC 离线 Sink 配置项。
 */
public final class JdbcSinkOptions
        extends JdbcCommonOptions {


    /**
     * 目标表路径。
     * <p>
     * 单表：
     * <p>
     * database.table
     * <p>
     * 多表可以不配置，默认沿用 Source 表路径。支持
     * {@code ${schema_name}} 和 {@code ${table_name}} 占位符，例如
     * {@code archive.${schema_name}_${table_name}}。
     * <p>
     * 配置目标表时优先使用自动生成的 INSERT/UPSERT SQL，忽略
     * {@code custom_sql}（以及其 {@code query} 别名）。
     */
    public static final Option<String> TABLE_PATH =
            Options.key("table_path")
                    .stringType()
                    .noDefaultValue()
                    .withFallbackKeys("table")
                    .withDescription("目标表路径或多表目标模板，支持 ${schema_name} 和 ${table_name}");

    public static final Option<SchemaSaveMode>
            SCHEMA_SAVE_MODE =
            Options.key("schema_save_mode")
                    .enumType(
                            SchemaSaveMode.class)
                    .defaultValue(
                            SchemaSaveMode
                                    .CREATE_SCHEMA_WHEN_NOT_EXIST)
                    .withDescription("目标表结构处理方式");

    public static final Option<DataSaveMode>
            DATA_SAVE_MODE =
            Options.key("data_save_mode")
                    .enumType(
                            DataSaveMode.class)
                    .defaultValue(
                            DataSaveMode.APPEND_DATA)
                    .withDescription("目标表已有数据处理方式");

    /**
     * 未配置时，由 JdbcDialect 自动生成 INSERT/UPSERT SQL。
     */
    public static final Option<String> CUSTOM_SQL =
            Options.key("custom_sql")
                    .stringType()
                    .noDefaultValue()
                    .withFallbackKeys("query")
                    .withDescription("自定义写入 SQL");

    public static final Option<JdbcWriteMode>
            WRITE_MODE =
            Options.key("write_mode")
                    .enumType(
                            JdbcWriteMode.class)
                    .defaultValue(
                            JdbcWriteMode.INSERT)
                    .withDescription("INSERT 或 UPSERT");

    /**
     * 用户显式指定的主键。
     * <p>
     * 未配置时可以使用 CatalogTable 中发现的主键。
     */
    public static final Option<List<String>>
            PRIMARY_KEYS =
            Options.key("primary_keys")
                    .listType()
                    .noDefaultValue()
                    .withDescription("UPSERT 使用的主键字段");

    public static final Option<Integer> BATCH_SIZE =
            Options.key("batch_size")
                    .intType()
                    .defaultValue(1000)
                    .withDescription("JDBC executeBatch 每批写入数量");

    /**
     * Number of write statements retained per SinkTask. The cache is bounded
     * so dynamic multi-table routing cannot grow JDBC driver resources without limit.
     */
    public static final Option<Integer> PREPARED_STATEMENT_CACHE_SIZE =
            Options.key("prepared_statement_cache_size")
                    .intType()
                    .defaultValue(32)
                    .withDescription("每个 SinkTask 缓存的目标表 PreparedStatement 数量上限");

    /**
     * JDBC query timeout applied to every cached write statement. A value of
     * {@code 0} leaves timeout handling to the JDBC driver.
     */
    public static final Option<Integer> QUERY_TIMEOUT_SEC =
            Options.key("query_timeout_sec")
                    .intType()
                    .defaultValue(0)
                    .withDescription("写入 PreparedStatement 的查询超时时间，单位秒，0 表示不主动限制");

    public static final Option<Integer> MAX_RETRIES =
            Options.key("max_retries")
                    .intType()
                    .defaultValue(3)
                    .withDescription("批次写入失败后的最大重试次数");

    /**
     * 脏数据处理策略。SKIP 会使用 Savepoint 回滚失败批次，然后逐行定位并跳过
     * 无法写入的记录；FAIL_FAST 保持事务失败即回滚的默认行为。
     */
    public static final Option<DirtyDataPolicy> DIRTY_DATA_POLICY =
            Options.key("dirty_data_policy")
                    .enumType(DirtyDataPolicy.class)
                    .defaultValue(DirtyDataPolicy.FAIL_FAST)
                    .withDescription("脏数据处理策略：FAIL_FAST 或 SKIP");

    public static final Option<DirtyDataOutputType> DIRTY_DATA_OUTPUT_TYPE =
            Options.key("dirty_data_output_type").enumType(DirtyDataOutputType.class).defaultValue(DirtyDataOutputType.MEMORY).withDescription("脏数据样例输出类型：MEMORY、LOGGING 或 JSONL");
    public static final Option<String> DIRTY_DATA_OUTPUT_PATH =
            Options.key("dirty_data_output_path").stringType().noDefaultValue().withDescription("JSONL 脏数据输出文件路径");
    public static final Option<Integer> DIRTY_DATA_MAX_SAMPLES =
            Options.key("dirty_data_max_samples").intType().defaultValue(100).withDescription("内存保留的脏数据样例上限");
    public static final Option<Integer> DIRTY_DATA_MAX_COUNT =
            Options.key("dirty_data_max_count").intType().defaultValue(Integer.MAX_VALUE).withDescription("允许跳过的脏数据数量上限");
    public static final Option<Double> DIRTY_DATA_MAX_PERCENTAGE =
            Options.key("dirty_data_max_percentage").doubleType().defaultValue(1D).withDescription("允许跳过的脏数据比例上限；分母为已尝试写入记录数");

    /**
     * 自动创建目标表时是否创建主键。
     * <p>
     * 当前 Catalog 第一版只支持主键，不处理普通索引。
     */
    public static final Option<Boolean>
            CREATE_PRIMARY_KEY =
            Options.key("create_primary_key")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("自动建表时是否创建主键");
}
