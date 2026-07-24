package com.link.up.api.table.catalog;

import com.link.up.api.table.type.FluxRowType;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Catalog 中的一张物理表。
 */
public final class CatalogTable implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TablePath tablePath;
    private final TableSchema tableSchema;
    private final String comment;
    private final Map<String, String> options;

    private CatalogTable(Builder builder) {
        this.tablePath =
                Objects.requireNonNull(
                        builder.tablePath,
                        "tablePath must not be null");

        this.tableSchema =
                Objects.requireNonNull(
                        builder.tableSchema,
                        "tableSchema must not be null");

        this.comment = normalize(builder.comment);

        Map<String, String> safeOptions =
                builder.options == null
                        ? Collections.emptyMap()
                        : new LinkedHashMap<>(builder.options);

        this.options =
                Collections.unmodifiableMap(safeOptions);
    }

    public static Builder builder(
            TablePath tablePath,
            TableSchema tableSchema) {

        return new Builder(
                tablePath,
                tableSchema);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public TablePath getTablePath() {
        return tablePath;
    }

    public TableSchema getTableSchema() {
        return tableSchema;
    }

    public String getComment() {
        return comment;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public FluxRowType getRowType() {
        return tableSchema.toRowType();
    }

    /**
     * 将当前表结构替换为新的 Schema。
     */
    public CatalogTable withSchema(
            TableSchema newSchema) {

        return toBuilder()
                .tableSchema(newSchema)
                .build();
    }

    /**
     * 将当前表映射到新的表路径。
     * <p>
     * 可用于 Source 表到 Sink 表的结构复制。
     */
    public CatalogTable withPath(
            TablePath newPath) {

        return toBuilder()
                .tablePath(newPath)
                .build();
    }

    public Builder toBuilder() {
        return new Builder(
                tablePath,
                tableSchema)
                .comment(comment)
                .options(options);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof CatalogTable)) {
            return false;
        }

        CatalogTable that = (CatalogTable) obj;

        return Objects.equals(
                tablePath,
                that.tablePath)
                && Objects.equals(
                tableSchema,
                that.tableSchema)
                && Objects.equals(
                comment,
                that.comment)
                && Objects.equals(
                options,
                that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tablePath,
                tableSchema,
                comment,
                options);
    }

    @Override
    public String toString() {
        return "CatalogTable{"
                + "tablePath="
                + tablePath
                + ", tableSchema="
                + tableSchema
                + ", comment='"
                + comment
                + '\''
                + '}';
    }

    public static final class Builder {

        private TablePath tablePath;
        private TableSchema tableSchema;
        private String comment;
        private Map<String, String> options;

        private Builder(
                TablePath tablePath,
                TableSchema tableSchema) {

            this.tablePath = tablePath;
            this.tableSchema = tableSchema;
        }

        public Builder tablePath(
                TablePath tablePath) {

            this.tablePath = tablePath;
            return this;
        }

        public Builder tableSchema(
                TableSchema tableSchema) {

            this.tableSchema = tableSchema;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder option(
                String key,
                String value) {

            if (options == null) {
                options = new LinkedHashMap<>();
            }

            options.put(key, value);
            return this;
        }

        public Builder options(
                Map<String, String> options) {

            this.options =
                    options == null
                            ? null
                            : new LinkedHashMap<>(options);

            return this;
        }

        public CatalogTable build() {
            return new CatalogTable(this);
        }
    }
}
