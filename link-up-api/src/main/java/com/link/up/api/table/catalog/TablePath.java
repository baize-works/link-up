package com.link.up.api.table.catalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据库表路径。
 * <p>
 * 支持以下形式：
 * <p>
 * 1. table
 * 2. database.table
 * 3. database.schema.table
 * <p>
 * 对于 PostgreSQL 这类使用 schema 的数据库，可通过：
 * <p>
 * TablePath.of(null, "public", "user")
 * <p>
 * 显式创建。
 */
public final class TablePath implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String databaseName;
    private final String schemaName;
    private final String tableName;

    private TablePath(
            String databaseName,
            String schemaName,
            String tableName) {

        this.databaseName = normalize(databaseName);
        this.schemaName = normalize(schemaName);
        this.tableName = requireText(tableName, "tableName");
    }

    public static TablePath of(String tableName) {
        return new TablePath(null, null, tableName);
    }

    public static TablePath of(
            String databaseName,
            String tableName) {

        return new TablePath(
                databaseName,
                null,
                tableName);
    }

    public static TablePath of(
            String databaseName,
            String schemaName,
            String tableName) {

        return new TablePath(
                databaseName,
                schemaName,
                tableName);
    }

    /**
     * 按固定规则解析表路径：
     * <p>
     * table
     * database.table
     * database.schema.table
     */
    public static TablePath parse(String fullName) {
        String value = requireText(fullName, "fullName");
        String[] parts = value.split("\\.");

        switch (parts.length) {
            case 1:
                return of(parts[0]);

            case 2:
                return of(parts[0], parts[1]);

            case 3:
                return of(parts[0], parts[1], parts[2]);

            default:
                throw new IllegalArgumentException(
                        "非法表路径：" + fullName);
        }
    }

    private static String join(
            String databaseName,
            String schemaName,
            String tableName,
            String quoteLeft,
            String quoteRight) {

        String left = quoteLeft == null ? "" : quoteLeft;
        String right = quoteRight == null ? "" : quoteRight;

        List<String> paths = new ArrayList<>(3);

        if (databaseName != null) {
            paths.add(left + databaseName + right);
        }

        if (schemaName != null) {
            paths.add(left + schemaName + right);
        }

        if (tableName != null) {
            paths.add(left + tableName + right);
        }

        return String.join(".", paths);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String requireText(
            String value,
            String fieldName) {

        String normalized = normalize(value);

        if (normalized == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be empty");
        }

        return normalized;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * 获取 schema.table。
     * <p>
     * 如果没有 schema，则只返回 table。
     */
    public String getSchemaAndTableName() {
        return join(
                null,
                schemaName,
                tableName,
                null,
                null);
    }

    /**
     * 获取 database.schema.table。
     */
    public String getFullName() {
        return join(
                databaseName,
                schemaName,
                tableName,
                null,
                null);
    }

    /**
     * 获取带引号的完整表名。
     * <p>
     * MySQL：
     * getFullNameQuoted("`", "`")
     * <p>
     * PostgreSQL：
     * getFullNameQuoted("\"", "\"")
     */
    public String getFullNameQuoted(
            String quoteLeft,
            String quoteRight) {

        return join(
                databaseName,
                schemaName,
                tableName,
                quoteLeft,
                quoteRight);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TablePath)) {
            return false;
        }

        TablePath that = (TablePath) obj;

        return Objects.equals(
                databaseName,
                that.databaseName)
                && Objects.equals(
                schemaName,
                that.schemaName)
                && Objects.equals(
                tableName,
                that.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                databaseName,
                schemaName,
                tableName);
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
