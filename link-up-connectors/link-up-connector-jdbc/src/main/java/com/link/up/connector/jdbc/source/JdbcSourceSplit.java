package com.link.up.connector.jdbc.source;

import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.type.FluxDataType;

import java.util.Objects;

/**
 * JDBC 数据读取分片。
 * <p>
 * 一个分片通常对应一段查询范围，例如：
 * <p>
 * id >= 1 and id < 10000
 */
public final class JdbcSourceSplit implements SourceSplit {

    private static final long serialVersionUID = 1L;

    /**
     * 当前分片所属的表。
     */
    private final TablePath tablePath;

    /**
     * 分片唯一标识。
     */
    private final String splitId;

    /**
     * 当前分片实际执行的 SQL。
     */
    private final String splitQuery;

    /**
     * 分片字段名称。
     */
    private final String splitKeyName;

    /**
     * 分片字段类型。
     */
    private final FluxDataType<?> splitKeyType;

    /**
     * 分片起始值。
     */
    private final Object splitStart;

    /**
     * 分片结束值。
     */
    private final Object splitEnd;

    public JdbcSourceSplit(
            TablePath tablePath,
            String splitId,
            String splitQuery,
            String splitKeyName,
            FluxDataType<?> splitKeyType,
            Object splitStart,
            Object splitEnd) {

        this.tablePath = Objects.requireNonNull(
                tablePath,
                "tablePath must not be null");

        this.splitId = Objects.requireNonNull(
                splitId,
                "splitId must not be null");

        this.splitQuery = Objects.requireNonNull(
                splitQuery,
                "splitQuery must not be null");

        this.splitKeyName = splitKeyName;
        this.splitKeyType = splitKeyType;
        this.splitStart = splitStart;
        this.splitEnd = splitEnd;
    }

    @Override
    public String splitId() {
        return splitId;
    }

    /**
     * 使用表路径作为数据集标识。
     * <p>
     * 后续 Sink 可以根据 dataSetId 找到对应的目标表。
     */
    @Override
    public String dataSetId() {
        return tablePath.toString();
    }

    public TablePath getTablePath() {
        return tablePath;
    }

    public String getSplitQuery() {
        return splitQuery;
    }

    public String getSplitKeyName() {
        return splitKeyName;
    }

    public FluxDataType<?> getSplitKeyType() {
        return splitKeyType;
    }

    public Object getSplitStart() {
        return splitStart;
    }

    public Object getSplitEnd() {
        return splitEnd;
    }

    @Override
    public String toString() {
        return "JdbcSourceSplit{"
                + "tablePath="
                + tablePath
                + ", splitId='"
                + splitId
                + '\''
                + ", splitQuery='"
                + splitQuery
                + '\''
                + '}';
    }
}
