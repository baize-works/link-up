package com.link.up.api.table.type;

import java.util.*;

/**
 * FluxRow 对应的字段结构。
 * <p>
 * 一个同步任务通常只维护一个 FluxRowType，
 * 不需要在每一条 FluxRow 中重复保存 Schema。
 */
public final class FluxRowType implements FluxDataType<FluxRow> {

    private static final long serialVersionUID = 1L;

    private final String[] fieldNames;
    private final FluxDataType<?>[] fieldTypes;

    /**
     * 字段名称和下标的映射，用于快速查找字段位置。
     */
    private final Map<String, Integer> fieldIndex;

    public FluxRowType(
            String[] fieldNames,
            FluxDataType<?>[] fieldTypes) {

        Objects.requireNonNull(fieldNames, "fieldNames must not be null");
        Objects.requireNonNull(fieldTypes, "fieldTypes must not be null");

        if (fieldNames.length != fieldTypes.length) {
            throw new IllegalArgumentException(
                    "The number of field names must be equal to the number of field types");
        }

        this.fieldNames = Arrays.copyOf(fieldNames, fieldNames.length);
        this.fieldTypes = Arrays.copyOf(fieldTypes, fieldTypes.length);

        Map<String, Integer> indexes = new LinkedHashMap<>();

        for (int i = 0; i < this.fieldNames.length; i++) {
            String fieldName = this.fieldNames[i];
            FluxDataType<?> fieldType = this.fieldTypes[i];

            if (fieldName == null || fieldName.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Field name at index " + i + " must not be empty");
            }

            if (fieldType == null) {
                throw new IllegalArgumentException(
                        "Field type at index " + i + " must not be null");
            }

            if (indexes.put(fieldName, i) != null) {
                throw new IllegalArgumentException(
                        "Duplicate field name: " + fieldName);
            }
        }

        this.fieldIndex = Collections.unmodifiableMap(indexes);
    }

    @Override
    public Class<FluxRow> getTypeClass() {
        return FluxRow.class;
    }

    @Override
    public SqlType getSqlType() {
        return SqlType.ROW;
    }

    public int getFieldCount() {
        return fieldTypes.length;
    }

    public String getFieldName(int index) {
        return fieldNames[index];
    }

    public FluxDataType<?> getFieldType(int index) {
        return fieldTypes[index];
    }

    /**
     * 返回副本，防止调用方修改 Schema。
     */
    public String[] getFieldNames() {
        return Arrays.copyOf(fieldNames, fieldNames.length);
    }

    /**
     * 返回副本，防止调用方修改 Schema。
     */
    public FluxDataType<?>[] getFieldTypes() {
        return Arrays.copyOf(fieldTypes, fieldTypes.length);
    }

    public int indexOf(String fieldName) {
        Integer index = fieldIndex.get(fieldName);

        if (index == null) {
            throw new IllegalArgumentException(
                    "Cannot find field: " + fieldName);
        }

        return index;
    }

    public int indexOf(String fieldName, int defaultValue) {
        Integer index = fieldIndex.get(fieldName);
        return index == null ? defaultValue : index;
    }

    /**
     * 校验一行数据的字段数量是否与 Schema 一致。
     */
    public void validate(FluxRow row) {
        Objects.requireNonNull(row, "row must not be null");

        if (row.getArity() != fieldTypes.length) {
            throw new IllegalArgumentException(
                    String.format(
                            "Row field count mismatch, expected=%d, actual=%d",
                            fieldTypes.length,
                            row.getArity()));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FluxRowType)) {
            return false;
        }

        FluxRowType that = (FluxRowType) obj;
        return Arrays.equals(fieldNames, that.fieldNames)
                && Arrays.equals(fieldTypes, that.fieldTypes);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(fieldNames);
        result = 31 * result + Arrays.hashCode(fieldTypes);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ROW<");

        for (int i = 0; i < fieldNames.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }

            builder.append(fieldNames[i])
                    .append(' ')
                    .append(fieldTypes[i]);
        }

        return builder.append('>').toString();
    }
}