package com.link.up.connector.jdbc.config;

import com.link.up.api.configuration.ReadonlyConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;
import java.util.*;

/**
 * 单张 JDBC 表的读取配置。
 * <p>
 * tablePath 既是物理表路径，也是多表任务中的数据集标识。
 * query 不为空时，使用 query 替代默认的 SELECT 语句。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JdbcSourceTableConfig
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_PARTITION_NUMBER = 4;

    private final String tablePath;
    private final String query;

    private final String partitionColumn;
    private final int partitionNumber;
    private final String partitionLowerBound;
    private final String partitionUpperBound;

    @JsonCreator
    public JdbcSourceTableConfig(
            @JsonProperty("table_path")
                    String tablePath,
            @JsonProperty("query")
                    String query,
            @JsonProperty("partition_column")
                    String partitionColumn,
            @JsonProperty("partition_num")
                    Integer partitionNumber,
            @JsonProperty("partition_lower_bound")
                    String partitionLowerBound,
            @JsonProperty("partition_upper_bound")
                    String partitionUpperBound) {

        this.tablePath = normalize(tablePath);
        this.query = normalize(query);
        this.partitionColumn =
                normalize(partitionColumn);

        this.partitionNumber =
                partitionNumber == null
                        ? DEFAULT_PARTITION_NUMBER
                        : partitionNumber;

        this.partitionLowerBound =
                normalize(partitionLowerBound);

        this.partitionUpperBound =
                normalize(partitionUpperBound);

        validateBasic();
    }

    /**
     * 将顶层单表配置或 table_list 统一转换为列表。
     */
    public static List<JdbcSourceTableConfig> from(
            ReadonlyConfig config) {

        List<JdbcSourceTableConfig> result;

        if (config.getOptional(
                JdbcSourceOptions.TABLE_LIST)
                .isPresent()) {

            if (config.getOptional(
                    JdbcSourceOptions.TABLE_PATH)
                    .isPresent()
                    || config.getOptional(
                    JdbcSourceOptions.QUERY)
                    .isPresent()) {

                throw new IllegalArgumentException(
                        "table_list 不能与顶层 table_path/query 同时配置");
            }

            result =
                    new ArrayList<>(
                            config.get(
                                    JdbcSourceOptions.TABLE_LIST));
        } else {
            result =
                    Collections.singletonList(
                            new JdbcSourceTableConfig(
                                    config.getOptional(
                                            JdbcSourceOptions
                                                    .TABLE_PATH)
                                            .orElse(null),
                                    config.getOptional(
                                            JdbcSourceOptions
                                                    .QUERY)
                                            .orElse(null),
                                    config.getOptional(
                                            JdbcSourceOptions
                                                    .PARTITION_COLUMN)
                                            .orElse(null),
                                    config.get(
                                            JdbcSourceOptions
                                                    .PARTITION_NUM),
                                    config.getOptional(
                                            JdbcSourceOptions
                                                    .PARTITION_LOWER_BOUND)
                                            .orElse(null),
                                    config.getOptional(
                                            JdbcSourceOptions
                                                    .PARTITION_UPPER_BOUND)
                                            .orElse(null)));
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "至少需要配置一张 JDBC 源表");
        }

        validateUniqueTablePath(result);

        return Collections.unmodifiableList(
                new ArrayList<>(result));
    }

    private static void validateUniqueTablePath(
            List<JdbcSourceTableConfig> tables) {

        Set<String> tablePaths =
                new HashSet<>();

        for (JdbcSourceTableConfig table :
                tables) {

            if (!tablePaths.add(
                    table.getTablePath())) {

                throw new IllegalArgumentException(
                        "table_path 不允许重复："
                                + table.getTablePath());
            }
        }
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

    /**
     * 校验分片配置。
     */
    public void validatePartition(
            boolean partitionReadEnabled) {

        boolean hasPartitionConfig =
                partitionColumn != null
                        || partitionLowerBound != null
                        || partitionUpperBound != null;

        if (!partitionReadEnabled) {
            if (hasPartitionConfig) {
                throw new IllegalArgumentException(
                        "未开启 enable_partition_read，"
                                + "但表 "
                                + tablePath
                                + " 配置了分片参数");
            }

            return;
        }



        /*
         * 开启分片能力，但当前表没有配置分片字段时，
         * 当前表仍按单分片读取。
         */
        if (!hasPartitionConfig) {
            return;
        }

        if (partitionColumn == null) {
            throw new IllegalArgumentException(
                    "表 "
                            + tablePath
                            + " 配置分片边界时必须指定 partition_column");
        }

        if (partitionNumber <= 1) {
            throw new IllegalArgumentException(
                    "partition_num 必须大于 1，table="
                            + tablePath);
        }

        boolean onlyOneBoundConfigured =
                (partitionLowerBound == null)
                        != (partitionUpperBound == null);

        if (onlyOneBoundConfigured) {
            throw new IllegalArgumentException(
                    "partition_lower_bound 和 "
                            + "partition_upper_bound 必须同时配置，table="
                            + tablePath);
        }
    }

    /**
     * 校验当前表的分片配置。
     * <p>
     * 未配置 partition_column 时按单分片读取；
     * 配置 partition_column 时自动启用分片读取。
     */
    public void validatePartition() {
        boolean hasLowerBound =
                partitionLowerBound != null;

        boolean hasUpperBound =
                partitionUpperBound != null;

        boolean hasAnyPartitionConfig =
                partitionColumn != null
                        || hasLowerBound
                        || hasUpperBound;

        if (!hasAnyPartitionConfig) {
            return;
        }

        if (partitionColumn == null) {
            throw new IllegalArgumentException(
                    "配置分片边界时必须指定 partition_column，table="
                            + tablePath);
        }

        if (partitionNumber <= 1) {
            throw new IllegalArgumentException(
                    "partition_num 必须大于 1，table="
                            + tablePath);
        }

        if (hasLowerBound != hasUpperBound) {
            throw new IllegalArgumentException(
                    "partition_lower_bound 和 "
                            + "partition_upper_bound 必须同时配置，table="
                            + tablePath);
        }
    }

    public boolean hasCustomQuery() {
        return query != null;
    }

    public boolean hasPartition() {
        return partitionColumn != null;
    }

    private void validateBasic() {
        /*
         * 即使是自定义 query，也要求 table_path。
         *
         * table_path 作为：
         * 1. Schema 标识；
         * 2. RecordBatch.dataSetId；
         * 3. 多表 Sink 路由标识。
         */
        if (tablePath == null) {
            throw new IllegalArgumentException(
                    "table_path must not be empty");
        }

        if (partitionNumber <= 0) {
            throw new IllegalArgumentException(
                    "partition_num must be greater than 0");
        }
    }
}