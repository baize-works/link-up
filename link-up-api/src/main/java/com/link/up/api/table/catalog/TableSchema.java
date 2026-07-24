package com.link.up.api.table.catalog;

import com.link.up.api.table.type.FluxDataType;
import com.link.up.api.table.type.FluxRowType;

import java.io.Serializable;
import java.util.*;

/**
 * 数据库表结构。
 * <p>
 * TableSchema 是不可变对象，可以安全地在 Source、Channel 和 Sink
 * 之间共享。
 */
public final class TableSchema implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Column> columns;
    private final Map<String, Integer> columnIndexes;
    private final PrimaryKey primaryKey;

    private TableSchema(Builder builder) {
        if (builder.columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Table schema columns must not be empty");
        }

        List<Column> safeColumns =
                new ArrayList<>(builder.columns.size());

        Map<String, Integer> indexes =
                new LinkedHashMap<>();

        for (int i = 0;
             i < builder.columns.size();
             i++) {

            Column column =
                    Objects.requireNonNull(
                            builder.columns.get(i),
                            "column must not be null");

            Integer oldIndex =
                    indexes.put(
                            column.getName(),
                            i);

            if (oldIndex != null) {
                throw new IllegalArgumentException(
                        "Duplicate column name: "
                                + column.getName());
            }

            safeColumns.add(column);
        }

        validatePrimaryKey(
                builder.primaryKey,
                indexes);

        this.columns =
                Collections.unmodifiableList(safeColumns);

        this.columnIndexes =
                Collections.unmodifiableMap(indexes);

        this.primaryKey = builder.primaryKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void validatePrimaryKey(
            PrimaryKey primaryKey,
            Map<String, Integer> indexes) {

        if (primaryKey == null) {
            return;
        }

        for (String columnName :
                primaryKey.getColumnNames()) {

            if (!indexes.containsKey(columnName)) {
                throw new IllegalArgumentException(
                        "Primary key column does not exist: "
                                + columnName);
            }
        }
    }

    public List<Column> getColumns() {
        return columns;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public Column getColumn(int index) {
        return columns.get(index);
    }

    public Column getColumn(String name) {
        Integer index = columnIndexes.get(name);

        if (index == null) {
            throw new IllegalArgumentException(
                    "Cannot find column: " + name);
        }

        return columns.get(index);
    }

    public int indexOf(String name) {
        Integer index = columnIndexes.get(name);
        return index == null ? -1 : index;
    }

    public boolean contains(String name) {
        return columnIndexes.containsKey(name);
    }

    public PrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    /**
     * 将数据库表结构转换为 FluxRowType。
     */
    public FluxRowType toRowType() {
        String[] fieldNames =
                new String[columns.size()];

        FluxDataType<?>[] fieldTypes =
                new FluxDataType<?>[columns.size()];

        for (int i = 0;
             i < columns.size();
             i++) {

            Column column = columns.get(i);

            fieldNames[i] = column.getName();
            fieldTypes[i] = column.getDataType();
        }

        return new FluxRowType(
                fieldNames,
                fieldTypes);
    }

    /**
     * 根据字段列表生成投影后的表结构。
     * <p>
     * 主键字段没有全部保留时，投影结果不再携带主键。
     */
    public TableSchema project(
            List<String> fieldNames) {

        if (fieldNames == null
                || fieldNames.isEmpty()) {

            throw new IllegalArgumentException(
                    "fieldNames must not be empty");
        }

        Builder builder = builder();

        for (String fieldName : fieldNames) {
            builder.column(getColumn(fieldName));
        }

        if (primaryKey != null
                && fieldNames.containsAll(
                primaryKey.getColumnNames())) {

            builder.primaryKey(primaryKey);
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TableSchema)) {
            return false;
        }

        TableSchema that = (TableSchema) obj;

        return Objects.equals(columns, that.columns)
                && Objects.equals(
                primaryKey,
                that.primaryKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                columns,
                primaryKey);
    }

    @Override
    public String toString() {
        return "TableSchema{"
                + "columns="
                + columns
                + ", primaryKey="
                + primaryKey
                + '}';
    }

    public static final class Builder {

        private final List<Column> columns =
                new ArrayList<>();

        private PrimaryKey primaryKey;

        public Builder column(Column column) {
            columns.add(column);
            return this;
        }

        public Builder columns(
                List<Column> columns) {

            if (columns != null) {
                this.columns.addAll(columns);
            }

            return this;
        }

        public Builder primaryKey(
                PrimaryKey primaryKey) {

            this.primaryKey = primaryKey;
            return this;
        }

        public TableSchema build() {
            return new TableSchema(this);
        }
    }
}
