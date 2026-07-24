package com.link.up.api.table.type;

import com.link.up.api.source.RecordSizeEstimator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Flux 离线数据行。
 * <p>
 * FluxRow 只保存字段值，
 * <p>
 * 表名和 Schema 应由同步任务上下文管理。
 */
public final class FluxRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前行的字段值。
     */
    private final Object[] fields;

    /**
     * 创建指定字段数量的空行。
     * <p>
     * JDBC Source 可以逐个调用 setField 写入字段。
     */
    public FluxRow(int arity) {
        if (arity < 0) {
            throw new IllegalArgumentException("arity must not be less than 0");
        }

        this.fields = new Object[arity];
    }

    private FluxRow(Object[] fields, boolean copy) {
        Objects.requireNonNull(fields, "fields must not be null");

        this.fields =
                copy
                        ? Arrays.copyOf(fields, fields.length)
                        : fields;
    }

    /**
     * 根据字段值创建一行数据。
     */
    public static FluxRow of(Object... fields) {
        return new FluxRow(fields, true);
    }

    /**
     * 从数组创建 FluxRow，并复制原数组。
     */
    public static FluxRow copyOf(Object[] fields) {
        return new FluxRow(fields, true);
    }

    public int getArity() {
        return fields.length;
    }

    public void setField(int index, Object value) {
        fields[index] = value;
    }

    public Object getField(int index) {
        return fields[index];
    }

    public boolean isNullAt(int index) {
        return fields[index] == null;
    }

    /**
     * 返回字段数组副本，避免外部直接修改内部数据。
     */
    public Object[] toArray() {
        return Arrays.copyOf(fields, fields.length);
    }

    /**
     * 复制完整数据行。
     */
    public FluxRow copy() {
        return new FluxRow(fields, true);
    }

    /**
     * Returns a stable approximate size for backpressure accounting.
     */
    public long estimatedSizeBytes() {
        long size = 24L + 16L + fields.length * 8L;
        for (Object field : fields) {
            size += RecordSizeEstimator.estimateObjectSizeBytes(field);
        }
        return size;
    }

    /**
     * 按字段下标投影生成新的数据行。
     * <p>
     * 例如：
     * project(new int[]{2, 0})
     * 表示只保留原来的第 3、1 个字段。
     */
    public FluxRow project(int[] indexMapping) {
        Objects.requireNonNull(indexMapping, "indexMapping must not be null");

        Object[] projectedFields = new Object[indexMapping.length];

        for (int i = 0; i < indexMapping.length; i++) {
            int sourceIndex = indexMapping[i];

            if (sourceIndex < 0 || sourceIndex >= fields.length) {
                throw new IndexOutOfBoundsException(
                        "Invalid source field index: " + sourceIndex);
            }

            projectedFields[i] = fields[sourceIndex];
        }

        return new FluxRow(projectedFields, false);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FluxRow)) {
            return false;
        }

        FluxRow that = (FluxRow) obj;
        return Arrays.deepEquals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(fields);
    }

    @Override
    public String toString() {
        return "FluxRow" + Arrays.deepToString(fields);
    }
}
