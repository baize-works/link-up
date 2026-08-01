package com.link.up.connector.jdbc.config;

import com.link.up.api.configuration.Option;
import com.link.up.api.configuration.Options;
import com.link.up.api.connector.schema.ConnectorOptionScope;

import java.util.List;

/**
 * JDBC 离线 Source 配置项。
 */
public final class JdbcSourceOptions
        extends JdbcCommonOptions {

    /**
     * 数据表路径。
     *
     * <p>例如 database.table 或 database.schema.table。
     */
    public static final Option<String> TABLE_PATH =
            Options.key("table_path")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("源表完整路径")
                    .withSemanticType("TABLE_PATH")
                    .withScope(
                            ConnectorOptionScope.TASK);

    /**
     * 自定义查询 SQL。
     *
     * <p>配置 query 时仍建议配置 table_path，
     * table_path 作为该查询结果的数据集标识。
     */
    public static final Option<String> QUERY =
            Options.key("query")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("自定义查询 SQL")
                    .withSemanticType("SQL")
                    .withScope(
                            ConnectorOptionScope.TASK);

    /**
     * 多表读取配置。
     */
    public static final Option<List<JdbcSourceTableConfig>>
            TABLE_LIST =
            Options.key("table_list")
                    .listType(
                            JdbcSourceTableConfig.class)
                    .noDefaultValue()
                    .withDescription("多表读取配置")
                    .withSemanticType("TABLE_LIST")
                    .withScope(
                            ConnectorOptionScope.TASK);

    /**
     * 公共过滤条件，不需要包含 where。
     */
    public static final Option<String> WHERE_CONDITION =
            Options.key("where_condition")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("公共查询过滤条件，不需要包含 where")
                    .withSemanticType("SQL_PREDICATE")
                    .withScope(
                            ConnectorOptionScope.TASK);

    /**
     * Requested consistency for the source read.
     */
    public static final Option<ReadConsistency>
            READ_CONSISTENCY =
            Options.key("read_consistency")
                    .enumType(
                            ReadConsistency.class)
                    .defaultValue(
                            ReadConsistency.BEST_EFFORT)
                    .withDescription("JDBC Source 读取一致性")
                    .withSemanticType("READ_CONSISTENCY")
                    .withScope(
                            ConnectorOptionScope.TASK);

    public static final Option<Integer> FETCH_SIZE =
            Options.key("fetch_size")
                    .intType()
                    .defaultValue(1000)
                    .withDescription("JDBC 每次从数据库获取的行数")
                    .withSemanticType("FETCH_SIZE")
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    /**
     * 是否启用字段范围分片读取。
     */
    public static final Option<Boolean>
            ENABLE_PARTITION_READ =
            Options.key("enable_partition_read")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("是否启用分片并行读取")
                    .withSemanticType("PARTITION_ENABLED")
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    public static final Option<SplitPlanningMode>
            SPLIT_PLANNING_MODE =
            Options.key("split_planning_mode")
                    .enumType(
                            SplitPlanningMode.class)
                    .defaultValue(
                            SplitPlanningMode.MANUAL)
                    .withDescription("分片边界规划模式")
                    .withSemanticType("SPLIT_PLANNING_MODE")
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    public static final Option<Integer>
            STATISTICS_QUERY_TIMEOUT =
            Options.key("statistics_query_timeout")
                    .intType()
                    .defaultValue(30)
                    .withDescription("统计查询超时秒数")
                    .withSemanticType("TIMEOUT_SECONDS")
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    public static final Option<Integer> SAMPLE_SIZE =
            Options.key("sample_size")
                    .intType()
                    .defaultValue(1000)
                    .withDescription("统计样本大小")
                    .withSemanticType("SAMPLE_SIZE")
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    public static final Option<Boolean>
            ALLOW_STATISTICS_FALLBACK =
            Options.key("allow_statistics_fallback")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("是否允许统计模式降级")
                    .withSemanticType("FALLBACK_POLICY")
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    public static final Option<Boolean>
            NULL_PARTITION_SINGLE_SPLIT =
            Options.key("null_partition_single_split")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("分区列全 NULL 时是否返回单 split")
                    .withSemanticType("NULL_PARTITION_POLICY")
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    public static final Option<String> PARTITION_COLUMN =
            Options.key("partition_column")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("分片字段")
                    .withSemanticType("PARTITION_COLUMN")
                    .withScope(
                            ConnectorOptionScope.TASK);

    public static final Option<Integer> PARTITION_NUM =
            Options.key("partition_num")
                    .intType()
                    .defaultValue(4)
                    .withDescription("分片数量")
                    .withSemanticType("PARTITION_COUNT")
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    public static final Option<String>
            PARTITION_LOWER_BOUND =
            Options.key("partition_lower_bound")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("分片字段下界")
                    .withSemanticType("PARTITION_BOUND")
                    .withScope(
                            ConnectorOptionScope.TASK);

    public static final Option<String>
            PARTITION_UPPER_BOUND =
            Options.key("partition_upper_bound")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("分片字段上界")
                    .withSemanticType("PARTITION_BOUND")
                    .withScope(
                            ConnectorOptionScope.TASK);
}
