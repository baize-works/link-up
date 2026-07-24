package com.link.up.connector.jdbc.catalog.mysql;

import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.type.BasicType;
import com.link.up.api.table.type.DecimalType;
import com.link.up.api.table.type.FluxDataType;
import com.link.up.api.table.type.SqlType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * MySQL 与 Flux 类型之间的转换器。
 */
public final class MySqlTypeMapper {

    private static final int DEFAULT_DECIMAL_PRECISION = 38;
    private static final int MAX_MYSQL_DECIMAL_PRECISION = 65;

    private final boolean intTypeNarrowing;

    public MySqlTypeMapper(
            boolean intTypeNarrowing) {

        this.intTypeNarrowing =
                intTypeNarrowing;
    }

    private static boolean isInteger(
            SqlType type) {

        return type == SqlType.TINYINT
                || type == SqlType.SMALLINT
                || type == SqlType.INT
                || type == SqlType.BIGINT;
    }

    private static boolean isNumeric(
            SqlType type) {

        return isInteger(type)
                || type == SqlType.FLOAT
                || type == SqlType.DOUBLE
                || type == SqlType.DECIMAL;
    }

    private static int valueOrDefault(
            Integer value,
            int defaultValue) {

        return value == null
                ? defaultValue
                : value;
    }

    private static boolean hasText(
            String value) {

        return value != null
                && !value.trim().isEmpty();
    }

    /**
     * 将 INFORMATION_SCHEMA.COLUMNS 当前行转换为 Flux Column。
     */
    public Column toColumn(
            ResultSet resultSet)
            throws SQLException {

        MySqlColumnMetadata metadata =
                MySqlColumnMetadata.from(resultSet);

        FluxDataType<?> fluxType =
                toFluxType(metadata);

        Column.Builder builder =
                Column.builder(
                        metadata.columnName,
                        fluxType)
                        .nullable(metadata.nullable)
                        .defaultValue(metadata.defaultValue)
                        .autoIncrement(metadata.autoIncrement)
                        .comment(metadata.comment)
                        .sourceType(metadata.columnType);

        SqlType sqlType =
                fluxType.getSqlType();

        if (sqlType == SqlType.STRING
                || sqlType == SqlType.BYTES) {

            builder.length(
                    metadata.characterLength);
        }

        if (sqlType == SqlType.DECIMAL) {
            builder.precision(
                    metadata.numericPrecision);

            builder.scale(
                    metadata.numericScale);
        } else if (sqlType == SqlType.TIME
                || sqlType == SqlType.TIMESTAMP
                || sqlType == SqlType.TIMESTAMP_TZ) {

            builder.precision(
                    metadata.dateTimePrecision);
        } else if (isInteger(sqlType)) {
            builder.precision(
                    metadata.numericPrecision);
        }

        builder.attribute(
                "unsigned",
                String.valueOf(metadata.unsigned));

        if (metadata.characterSet != null) {
            builder.attribute(
                    "charset",
                    metadata.characterSet);
        }

        if (metadata.collation != null) {
            builder.attribute(
                    "collation",
                    metadata.collation);
        }

        if (metadata.extra != null) {
            builder.attribute(
                    "extra",
                    metadata.extra);
        }

        return builder.build();
    }

    /**
     * 将 Flux 字段转换成 MySQL 字段类型。
     *
     * @param preserveSourceType 当字段本身来自 MySQL 时，是否优先保留原始类型
     */
    public String toMySqlType(
            Column column,
            boolean preserveSourceType) {

        if (preserveSourceType
                && hasText(column.getSourceType())) {

            return column.getSourceType();
        }

        SqlType sqlType =
                column.getDataType().getSqlType();

        String type;

        switch (sqlType) {
            case STRING:
                type = buildStringType(column);
                break;

            case BOOLEAN:
                type = "BOOLEAN";
                break;

            case TINYINT:
                type = "TINYINT";
                break;

            case SMALLINT:
                type = "SMALLINT";
                break;

            case INT:
                type = "INT";
                break;

            case BIGINT:
                type = "BIGINT";
                break;

            case FLOAT:
                type = "FLOAT";
                break;

            case DOUBLE:
                type = "DOUBLE";
                break;

            case DECIMAL:
                type = buildDecimalType(column);
                break;

            case BYTES:
                type = buildBinaryType(column);
                break;

            case DATE:
                type = "DATE";
                break;

            case TIME:
                type = buildTimeType(
                        "TIME",
                        column.getPrecision());
                break;

            case TIMESTAMP:
                type = buildTimeType(
                        "DATETIME",
                        column.getPrecision());
                break;

            case TIMESTAMP_TZ:
                /*
                 * MySQL 没有完整的 TIMESTAMP WITH TIME ZONE。
                 * 当前统一映射为 TIMESTAMP。
                 */
                type = buildTimeType(
                        "TIMESTAMP",
                        column.getPrecision());
                break;

            case ARRAY:
            case ROW:
            case NULL:
            default:
                throw new IllegalArgumentException(
                        "MySQL 不支持 Flux 类型："
                                + sqlType
                                + "，column="
                                + column.getName());
        }

        if (isUnsigned(column, sqlType)) {
            type += " UNSIGNED";
        }

        return type;
    }

    private FluxDataType<?> toFluxType(
            MySqlColumnMetadata metadata) {

        String dataType =
                metadata.dataType.toLowerCase(
                        Locale.ROOT);

        switch (dataType) {
            case "bool":
            case "boolean":
                return BasicType.BOOLEAN_TYPE;

            case "bit":
                if (valueOrDefault(
                        metadata.numericPrecision,
                        1)
                        <= 1) {

                    return BasicType.BOOLEAN_TYPE;
                }

                return BasicType.BYTES_TYPE;

            case "tinyint":
                if (intTypeNarrowing
                        && metadata.columnType
                        .toLowerCase(Locale.ROOT)
                        .startsWith("tinyint(1)")) {

                    return BasicType.BOOLEAN_TYPE;
                }

                if (!intTypeNarrowing) {
                    return metadata.unsigned
                            ? BasicType.INT_TYPE
                            : BasicType.INT_TYPE;
                }

                return metadata.unsigned
                        ? BasicType.SHORT_TYPE
                        : BasicType.BYTE_TYPE;

            case "smallint":
                if (!intTypeNarrowing) {
                    return BasicType.INT_TYPE;
                }

                return metadata.unsigned
                        ? BasicType.INT_TYPE
                        : BasicType.SHORT_TYPE;

            case "mediumint":
            case "int":
            case "integer":
                return metadata.unsigned
                        ? BasicType.LONG_TYPE
                        : BasicType.INT_TYPE;

            case "bigint":
                if (metadata.unsigned) {
                    return new DecimalType(20, 0);
                }

                return BasicType.LONG_TYPE;

            case "float":
                return BasicType.FLOAT_TYPE;

            case "double":
            case "real":
                return BasicType.DOUBLE_TYPE;

            case "decimal":
            case "numeric":
                return decimalType(metadata);

            case "date":
                return BasicType.DATE_TYPE;

            case "time":
                return BasicType.TIME_TYPE;

            case "datetime":
            case "timestamp":
                return BasicType.TIMESTAMP_TYPE;

            case "year":
                return BasicType.INT_TYPE;

            case "binary":
            case "varbinary":
            case "tinyblob":
            case "blob":
            case "mediumblob":
            case "longblob":
            case "geometry":
            case "point":
            case "linestring":
            case "polygon":
            case "multipoint":
            case "multilinestring":
            case "multipolygon":
            case "geometrycollection":
                return BasicType.BYTES_TYPE;

            case "char":
            case "varchar":
            case "tinytext":
            case "text":
            case "mediumtext":
            case "longtext":
            case "enum":
            case "set":
            case "json":
                return BasicType.STRING_TYPE;

            default:
                throw new IllegalArgumentException(
                        "暂不支持 MySQL 字段类型："
                                + metadata.columnType
                                + "，column="
                                + metadata.columnName);
        }
    }

    private DecimalType decimalType(
            MySqlColumnMetadata metadata) {

        int precision =
                valueOrDefault(
                        metadata.numericPrecision,
                        DEFAULT_DECIMAL_PRECISION);

        int scale =
                valueOrDefault(
                        metadata.numericScale,
                        0);

        precision =
                Math.min(
                        precision,
                        MAX_MYSQL_DECIMAL_PRECISION);

        precision =
                Math.max(
                        precision,
                        scale);

        return new DecimalType(
                precision,
                scale);
    }

    private String buildStringType(
            Column column) {

        Long length =
                column.getLength();

        if (length == null || length <= 0) {
            return "TEXT";
        }

        /*
         * utf8mb4 下 VARCHAR 受单行最大长度限制。
         * 超过常用安全范围时直接使用 LONGTEXT。
         */
        if (length <= 16383) {
            return "VARCHAR(" + length + ")";
        }

        return "LONGTEXT";
    }

    private String buildBinaryType(
            Column column) {

        Long length =
                column.getLength();

        if (length != null
                && length > 0
                && length <= 65535) {

            return "VARBINARY(" + length + ")";
        }

        return "LONGBLOB";
    }

    private String buildDecimalType(
            Column column) {

        int precision =
                column.getPrecision() == null
                        ? DEFAULT_DECIMAL_PRECISION
                        : column.getPrecision();

        int scale =
                column.getScale() == null
                        ? 0
                        : column.getScale();

        precision =
                Math.min(
                        precision,
                        MAX_MYSQL_DECIMAL_PRECISION);

        precision =
                Math.max(
                        precision,
                        scale);

        return "DECIMAL("
                + precision
                + ","
                + scale
                + ")";
    }

    private String buildTimeType(
            String type,
            Integer precision) {

        if (precision == null
                || precision <= 0) {

            return type;
        }

        int safePrecision =
                Math.min(precision, 6);

        return type
                + "("
                + safePrecision
                + ")";
    }

    private boolean isUnsigned(
            Column column,
            SqlType sqlType) {

        if (!isNumeric(sqlType)) {
            return false;
        }

        return Boolean.parseBoolean(
                column.getAttributes()
                        .get("unsigned"));
    }

    /**
     * INFORMATION_SCHEMA.COLUMNS 中的一行字段元数据。
     */
    private static final class MySqlColumnMetadata {

        private final String columnName;
        private final String dataType;
        private final String columnType;
        private final Long characterLength;
        private final Integer numericPrecision;
        private final Integer numericScale;
        private final Integer dateTimePrecision;
        private final boolean nullable;
        private final Object defaultValue;
        private final boolean unsigned;
        private final boolean autoIncrement;
        private final String comment;
        private final String characterSet;
        private final String collation;
        private final String extra;

        private MySqlColumnMetadata(
                String columnName,
                String dataType,
                String columnType,
                Long characterLength,
                Integer numericPrecision,
                Integer numericScale,
                Integer dateTimePrecision,
                boolean nullable,
                Object defaultValue,
                boolean unsigned,
                boolean autoIncrement,
                String comment,
                String characterSet,
                String collation,
                String extra) {

            this.columnName = columnName;
            this.dataType = dataType;
            this.columnType = columnType;
            this.characterLength = characterLength;
            this.numericPrecision = numericPrecision;
            this.numericScale = numericScale;
            this.dateTimePrecision = dateTimePrecision;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
            this.unsigned = unsigned;
            this.autoIncrement = autoIncrement;
            this.comment = normalize(comment);
            this.characterSet = normalize(characterSet);
            this.collation = normalize(collation);
            this.extra = normalize(extra);
        }

        private static MySqlColumnMetadata from(
                ResultSet resultSet)
                throws SQLException {

            String columnType =
                    resultSet.getString(
                            "COLUMN_TYPE");

            String extra =
                    resultSet.getString(
                            "EXTRA");

            return new MySqlColumnMetadata(
                    resultSet.getString("COLUMN_NAME"),
                    resultSet.getString("DATA_TYPE"),
                    columnType,
                    getLongOrNull(
                            resultSet,
                            "CHARACTER_MAXIMUM_LENGTH"),
                    getIntegerOrNull(
                            resultSet,
                            "NUMERIC_PRECISION"),
                    getIntegerOrNull(
                            resultSet,
                            "NUMERIC_SCALE"),
                    getIntegerOrNull(
                            resultSet,
                            "DATETIME_PRECISION"),
                    "YES".equalsIgnoreCase(
                            resultSet.getString(
                                    "IS_NULLABLE")),
                    resultSet.getObject(
                            "COLUMN_DEFAULT"),
                    columnType != null
                            && columnType
                            .toLowerCase(Locale.ROOT)
                            .contains("unsigned"),
                    extra != null
                            && extra
                            .toLowerCase(Locale.ROOT)
                            .contains("auto_increment"),
                    resultSet.getString(
                            "COLUMN_COMMENT"),
                    resultSet.getString(
                            "CHARACTER_SET_NAME"),
                    resultSet.getString(
                            "COLLATION_NAME"),
                    extra);
        }

        private static Long getLongOrNull(
                ResultSet resultSet,
                String name)
                throws SQLException {

            Object value =
                    resultSet.getObject(name);

            if (value == null) {
                return null;
            }

            return ((Number) value)
                    .longValue();
        }

        private static Integer getIntegerOrNull(
                ResultSet resultSet,
                String name)
                throws SQLException {

            Object value =
                    resultSet.getObject(name);

            if (value == null) {
                return null;
            }

            return ((Number) value)
                    .intValue();
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
    }
}