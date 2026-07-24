package com.link.up.connector.jdbc.config;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.connector.jdbc.options.MultiTableCommonOptions;
import com.link.up.connector.jdbc.options.MultiTableFailurePolicy;

import java.io.Serializable;
import java.util.*;

/**
 * JDBC 离线 Source 运行配置。
 * <p>
 * 该类只保存已经解析和校验完成的运行配置，
 * 不承担 JDBC 连接、表结构发现或数据读取工作。
 */
public final class JdbcSourceConfig
        implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JDBC 连接配置。
     */
    private final JdbcConnectionConfig connectionConfig;

    /**
     * 单表或多表读取配置。
     */
    private final List<JdbcSourceTableConfig> tableConfigs;

    /**
     * 全局过滤条件。
     * <p>
     * 内部统一不包含 WHERE 关键字，例如：
     * <p>
     * id > 100
     */
    private final String whereCondition;

    /**
     * JDBC Statement FetchSize。
     * <p>
     * 0 表示使用 JDBC Driver 默认值。
     */
    private final int fetchSize;

    /**
     * Requested consistency for the source read.
     */
    private final ReadConsistency readConsistency;
    private final SplitPlanningMode splitPlanningMode;
    private final int statisticsQueryTimeout;
    private final int sampleSize;
    private final boolean allowStatisticsFallback;
    private final boolean nullPartitionSingleSplit;

    /**
     * MySQL 整数类型缩小策略。
     */
    private final boolean intTypeNarrowing;

    /**
     * 多表任务中单张表发现失败时的处理策略。
     */
    private final MultiTableFailurePolicy
            multiTableFailurePolicy;

    private JdbcSourceConfig(
            JdbcConnectionConfig connectionConfig,
            List<JdbcSourceTableConfig> tableConfigs,
            String whereCondition,
            int fetchSize,
            ReadConsistency readConsistency,
            SplitPlanningMode splitPlanningMode, int statisticsQueryTimeout, int sampleSize, boolean allowStatisticsFallback, boolean nullPartitionSingleSplit,
            boolean intTypeNarrowing,
            MultiTableFailurePolicy
                    multiTableFailurePolicy) {

        this.connectionConfig =
                Objects.requireNonNull(
                        connectionConfig,
                        "connectionConfig must not be null");

        this.tableConfigs =
                immutableTableConfigs(tableConfigs);

        this.whereCondition =
                normalizeWhereCondition(
                        whereCondition);

        if (fetchSize < 0) {
            throw new IllegalArgumentException(
                    "fetchSize must not be negative");
        }

        this.fetchSize = fetchSize;
        this.readConsistency = Objects.requireNonNull(
                readConsistency, "readConsistency must not be null");
        this.splitPlanningMode = Objects.requireNonNull(splitPlanningMode, "splitPlanningMode");
        if (statisticsQueryTimeout <= 0 || sampleSize <= 0)
            throw new IllegalArgumentException("statistics_query_timeout and sample_size must be positive");
        this.statisticsQueryTimeout = statisticsQueryTimeout;
        this.sampleSize = sampleSize;
        this.allowStatisticsFallback = allowStatisticsFallback;
        this.nullPartitionSingleSplit = nullPartitionSingleSplit;
        this.intTypeNarrowing =
                intTypeNarrowing;

        this.multiTableFailurePolicy =
                Objects.requireNonNull(
                        multiTableFailurePolicy,
                        "multiTableFailurePolicy must not be null");

        validate();
    }

    /**
     * 从插件配置创建 JDBC Source 运行配置。
     */
    public static JdbcSourceConfig of(
            ReadonlyConfig config) {

        Objects.requireNonNull(
                config,
                "config must not be null");

        return new JdbcSourceConfig(
                JdbcConnectionConfig.of(config),
                JdbcSourceTableConfig.from(config),
                config.getOptional(
                        JdbcSourceOptions
                                .WHERE_CONDITION)
                        .orElse(null),
                config.get(
                        JdbcSourceOptions.FETCH_SIZE),
                config.get(
                        JdbcSourceOptions.READ_CONSISTENCY),
                config.get(JdbcSourceOptions.SPLIT_PLANNING_MODE), config.get(JdbcSourceOptions.STATISTICS_QUERY_TIMEOUT), config.get(JdbcSourceOptions.SAMPLE_SIZE), config.get(JdbcSourceOptions.ALLOW_STATISTICS_FALLBACK), config.get(JdbcSourceOptions.NULL_PARTITION_SINGLE_SPLIT),
                config.get(
                        JdbcCommonOptions
                                .INT_TYPE_NARROWING),
                config.get(
                        MultiTableCommonOptions
                                .MULTI_TABLE_FAILURE_POLICY));
    }

    private static List<JdbcSourceTableConfig>
    immutableTableConfigs(
            List<JdbcSourceTableConfig> tables) {

        Objects.requireNonNull(
                tables,
                "tableConfigs must not be null");

        if (tables.isEmpty()) {
            throw new IllegalArgumentException(
                    "tableConfigs must not be empty");
        }

        List<JdbcSourceTableConfig> result =
                new ArrayList<>(tables.size());

        for (JdbcSourceTableConfig table : tables) {
            result.add(
                    Objects.requireNonNull(
                            table,
                            "tableConfig must not be null"));
        }

        return Collections.unmodifiableList(
                result);
    }

    /**
     * 兼容以下两种写法：
     * <p>
     * where id > 100
     * <p>
     * id > 100
     * <p>
     * 内部统一保存为：
     * <p>
     * id > 100
     */
    private static String normalizeWhereCondition(
            String value) {

        String condition = normalize(value);

        if (condition == null) {
            return null;
        }

        String lower =
                condition.toLowerCase(
                        Locale.ROOT);

        if ("where".equals(lower)) {
            throw new IllegalArgumentException(
                    "where_condition 不能只有 where 关键字");
        }

        /*
         * 只在 where 后面是空白字符时删除关键字。
         *
         * 避免把字段名 where_code 错误处理为：
         *
         * _code
         */
        if (lower.startsWith("where")
                && condition.length() > 5
                && Character.isWhitespace(
                condition.charAt(5))) {

            condition =
                    condition.substring(6)
                            .trim();
        }

        if (condition.isEmpty()) {
            throw new IllegalArgumentException(
                    "where_condition must not be empty");
        }

        return condition;
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

    public JdbcConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public List<JdbcSourceTableConfig> getTableConfigs() {
        return tableConfigs;
    }

    public String getWhereCondition() {
        return whereCondition;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public ReadConsistency getReadConsistency() {
        return readConsistency;
    }

    public SplitPlanningMode getSplitPlanningMode() {
        return splitPlanningMode;
    }

    public int getStatisticsQueryTimeout() {
        return statisticsQueryTimeout;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public boolean isAllowStatisticsFallback() {
        return allowStatisticsFallback;
    }

    public boolean isNullPartitionSingleSplit() {
        return nullPartitionSingleSplit;
    }

    public boolean isIntTypeNarrowing() {
        return intTypeNarrowing;
    }

    public MultiTableFailurePolicy
    getMultiTableFailurePolicy() {

        return multiTableFailurePolicy;
    }

    /**
     * 当前任务是否为多表同步。
     */
    public boolean isMultiTable() {
        return tableConfigs.size() > 1;
    }

    /**
     * 当前任务是否配置了全局过滤条件。
     */
    public boolean hasWhereCondition() {
        return whereCondition != null;
    }

    /**
     * 当前任务是否包含自定义查询。
     */
    public boolean hasCustomQuery() {
        for (JdbcSourceTableConfig table :
                tableConfigs) {

            if (table.hasCustomQuery()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 当前任务是否启用了分片读取。
     * <p>
     * 不再额外维护 enable_partition_read，
     * 只要任意表配置 partition_column，即认为需要分片。
     */
    public boolean hasPartitionTable() {
        for (JdbcSourceTableConfig table :
                tableConfigs) {

            if (table.hasPartition()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取单表配置。
     * <p>
     * 仅用于明确要求单表的执行逻辑。
     */
    public JdbcSourceTableConfig getSingleTableConfig() {
        if (isMultiTable()) {
            throw new IllegalStateException(
                    "当前任务不是单表任务，tableCount="
                            + tableConfigs.size());
        }

        return tableConfigs.get(0);
    }

    /**
     * 校验 Source 级别的配置关系。
     */
    private void validate() {
        for (JdbcSourceTableConfig table :
                tableConfigs) {

            /*
             * 分片参数属于单表配置，
             * 由每张表自行校验。
             */
            table.validatePartition();

            /*
             * 自定义查询本身已经是一条完整 SQL，
             * 不再向其后面拼接全局 where_condition。
             */
            if (whereCondition != null
                    && table.hasCustomQuery()) {

                throw new IllegalArgumentException(
                        "query 不能与全局 where_condition 同时配置，table="
                                + table.getTablePath());
            }
        }
    }

    @Override
    public String toString() {
        /*
         * 不输出密码等连接敏感信息。
         */
        return "JdbcSourceConfig{"
                + "tableCount="
                + tableConfigs.size()
                + ", whereCondition='"
                + whereCondition
                + '\''
                + ", fetchSize="
                + fetchSize
                + ", readConsistency="
                + readConsistency
                + ", splitPlanningMode=" + splitPlanningMode
                + ", intTypeNarrowing="
                + intTypeNarrowing
                + ", multiTableFailurePolicy="
                + multiTableFailurePolicy
                + '}';
    }
}