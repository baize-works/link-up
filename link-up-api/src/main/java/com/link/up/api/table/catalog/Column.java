package com.link.up.api.table.catalog;

import com.link.up.api.table.type.FluxDataType;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 数据库物理列。
 * <p>
 * Catalog 层只描述实际存在于数据库表中的字段，
 * 不包含 CDC 元数据字段或运行时计算字段。
 */
public final class Column implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final FluxDataType<?> dataType;

    /**
     * 字符串、二进制等类型的最大长度。
     */
    private final Long length;

    /**
     * 数值精度或时间精度。
     */
    private final Integer precision;

    /**
     * 小数位数。
     */
    private final Integer scale;

    private final boolean nullable;
    private final Object defaultValue;
    private final boolean autoIncrement;
    private final String comment;

    /**
     * 数据库原始字段类型。
     * <p>
     * 例如：
     * <p>
     * varchar(255)
     * int unsigned
     * numeric(20, 4)
     * timestamp without time zone
     */
    private final String sourceType;

    /**
     * 数据库厂商特有属性。
     * <p>
     * 例如：
     * <p>
     * unsigned=true
     * charset=utf8mb4
     * collation=utf8mb4_general_ci
     */
    private final Map<String, String> attributes;

    private Column(Builder builder) {
        this.name = requireText(
                builder.name,
                "column name");

        this.dataType = Objects.requireNonNull(
                builder.dataType,
                "dataType must not be null");

        validateNonNegative(
                builder.length,
                "length");

        validateNonNegative(
                builder.precision,
                "precision");

        validateNonNegative(
                builder.scale,
                "scale");

        if (builder.precision != null
                && builder.scale != null
                && builder.scale > builder.precision) {

            throw new IllegalArgumentException(
                    "scale must not be greater than precision");
        }

        this.length = builder.length;
        this.precision = builder.precision;
        this.scale = builder.scale;
        this.nullable = builder.nullable;
        this.defaultValue = builder.defaultValue;
        this.autoIncrement = builder.autoIncrement;
        this.comment = normalize(builder.comment);
        this.sourceType = normalize(builder.sourceType);

        Map<String, String> safeAttributes =
                builder.attributes == null
                        ? Collections.emptyMap()
                        : new LinkedHashMap<>(builder.attributes);

        this.attributes =
                Collections.unmodifiableMap(safeAttributes);
    }

    public static Builder builder(
            String name,
            FluxDataType<?> dataType) {

        return new Builder(name, dataType);
    }

    private static void validateNonNegative(
            Number value,
            String fieldName) {

        if (value != null
                && value.longValue() < 0) {

            throw new IllegalArgumentException(
                    fieldName + " must not be negative");
        }
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

    public String getName() {
        return name;
    }

    public FluxDataType<?> getDataType() {
        return dataType;
    }

    public Long getLength() {
        return length;
    }

    public Integer getPrecision() {
        return precision;
    }

    public Integer getScale() {
        return scale;
    }

    public boolean isNullable() {
        return nullable;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public String getComment() {
        return comment;
    }

    public String getSourceType() {
        return sourceType;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * 创建一个修改字段类型后的副本。
     */
    public Column withType(
            FluxDataType<?> newType) {

        return toBuilder()
                .dataType(newType)
                .build();
    }

    /**
     * 创建一个重命名后的字段副本。
     */
    public Column rename(String newName) {
        return toBuilder()
                .name(newName)
                .build();
    }

    public Builder toBuilder() {
        return new Builder(name, dataType)
                .length(length)
                .precision(precision)
                .scale(scale)
                .nullable(nullable)
                .defaultValue(defaultValue)
                .autoIncrement(autoIncrement)
                .comment(comment)
                .sourceType(sourceType)
                .attributes(attributes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Column)) {
            return false;
        }

        Column that = (Column) obj;

        return nullable == that.nullable
                && autoIncrement == that.autoIncrement
                && Objects.equals(name, that.name)
                && Objects.equals(dataType, that.dataType)
                && Objects.equals(length, that.length)
                && Objects.equals(precision, that.precision)
                && Objects.equals(scale, that.scale)
                && Objects.equals(defaultValue, that.defaultValue)
                && Objects.equals(comment, that.comment)
                && Objects.equals(sourceType, that.sourceType)
                && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                dataType,
                length,
                precision,
                scale,
                nullable,
                defaultValue,
                autoIncrement,
                comment,
                sourceType,
                attributes);
    }

    @Override
    public String toString() {
        return "Column{"
                + "name='"
                + name
                + '\''
                + ", dataType="
                + dataType
                + ", nullable="
                + nullable
                + ", autoIncrement="
                + autoIncrement
                + '}';
    }

    public static final class Builder {

        private String name;
        private FluxDataType<?> dataType;
        private Long length;
        private Integer precision;
        private Integer scale;
        private boolean nullable = true;
        private Object defaultValue;
        private boolean autoIncrement;
        private String comment;
        private String sourceType;
        private Map<String, String> attributes;

        private Builder(
                String name,
                FluxDataType<?> dataType) {

            this.name = name;
            this.dataType = dataType;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder dataType(
                FluxDataType<?> dataType) {

            this.dataType = dataType;
            return this;
        }

        public Builder length(Long length) {
            this.length = length;
            return this;
        }

        public Builder precision(Integer precision) {
            this.precision = precision;
            return this;
        }

        public Builder scale(Integer scale) {
            this.scale = scale;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder autoIncrement(
                boolean autoIncrement) {

            this.autoIncrement = autoIncrement;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder sourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder attribute(
                String key,
                String value) {

            if (attributes == null) {
                attributes = new LinkedHashMap<>();
            }

            attributes.put(key, value);
            return this;
        }

        public Builder attributes(
                Map<String, String> attributes) {

            this.attributes =
                    attributes == null
                            ? null
                            : new LinkedHashMap<>(attributes);

            return this;
        }

        public Column build() {
            return new Column(this);
        }
    }
}