package com.link.up.api.table.catalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 表主键定义。
 */
public final class PrimaryKey implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键名称。
     * <p>
     * 部分数据库可能无法获取主键名称，因此允许为空。
     */
    private final String name;

    /**
     * 主键字段，保留数据库中的字段顺序。
     */
    private final List<String> columnNames;

    private PrimaryKey(
            String name,
            List<String> columnNames) {

        if (columnNames == null
                || columnNames.isEmpty()) {

            throw new IllegalArgumentException(
                    "Primary key columns must not be empty");
        }

        List<String> safeColumns =
                new ArrayList<>(columnNames.size());

        for (String columnName : columnNames) {
            if (columnName == null
                    || columnName.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Primary key column must not be empty");
            }

            safeColumns.add(columnName.trim());
        }

        this.name = normalize(name);
        this.columnNames =
                Collections.unmodifiableList(safeColumns);
    }

    public static PrimaryKey of(
            String name,
            List<String> columnNames) {

        return new PrimaryKey(
                name,
                columnNames);
    }

    public static PrimaryKey of(
            List<String> columnNames) {

        return new PrimaryKey(
                null,
                columnNames);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public String getName() {
        return name;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public boolean contains(String fieldName) {
        return columnNames.contains(fieldName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof PrimaryKey)) {
            return false;
        }

        PrimaryKey that = (PrimaryKey) obj;

        return Objects.equals(name, that.name)
                && Objects.equals(
                columnNames,
                that.columnNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                columnNames);
    }

    @Override
    public String toString() {
        return "PrimaryKey{"
                + "name='"
                + name
                + '\''
                + ", columnNames="
                + columnNames
                + '}';
    }
}