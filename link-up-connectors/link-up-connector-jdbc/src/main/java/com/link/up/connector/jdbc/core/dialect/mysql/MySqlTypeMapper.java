package com.link.up.connector.jdbc.core.dialect.mysql;

import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.type.BasicType;
import com.link.up.api.table.type.DecimalType;
import com.link.up.api.table.type.FluxDataType;
import com.link.up.api.table.type.SqlType;
import com.link.up.connector.jdbc.core.dialect.JdbcTypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/**
 * MySQL 类型映射器。
 * <p>
 * 不依赖 MySQL Driver 内部 MysqlType 枚举，
 * 只使用 JDBC 标准类型和 INFORMATION_SCHEMA 元数据。
 */
public final class MySqlTypeMapper
        implements JdbcTypeMapper {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    MySqlTypeMapper.class);

    private static final int
            MAX_FLUX_DECIMAL_PRECISION = 38;

    private static final int
            DEFAULT_DECIMAL_PRECISION = 38;

    private static final int
            DEFAULT_DECIMAL_SCALE = 18;

    private static final int
            MAX_TIME_PRECISION = 6;

    private final boolean intTypeNarrowing;

    public MySqlTypeMapper(
            boolean intTypeNarrowing) {

        this.intTypeNarrowing =
                intTypeNarrowing;
    }

    private static void applyTypeProperties(
            Column.Builder builder,
            SqlType sqlType,
            int precision,
            int scale) {

        if (sqlType == SqlType.STRING
                || sqlType == SqlType.BYTES) {

            if (precision > 0) {
                builder.length(
                        (long) precision);
            }

            return;
        }

        if (sqlType == SqlType.DECIMAL) {
            if (precision > 0) {
                builder.precision(
                        precision);
            }

            if (scale >= 0) {
                builder.scale(scale);
            }

            return;
        }

        if (sqlType == SqlType.TIME
                || sqlType == SqlType.TIMESTAMP
                || sqlType
                == SqlType.TIMESTAMP_TZ) {

            if (scale > 0) {
                builder.precision(scale);
            }
        }
    }

    private static String stringType(
            Column column) {

        Long length =
                column.getLength();

        if (length == null
                || length <= 0) {

            return "LONGTEXT";
        }

        if (length <= 255) {
            return "VARCHAR(" + length + ")";
        }

        if (length <= 65_535) {
            return "TEXT";
        }

        if (length <= 16_777_215) {
            return "MEDIUMTEXT";
        }

        return "LONGTEXT";
    }

    private static String binaryType(
            Column column) {

        Long length =
                column.getLength();

        if (length == null
                || length <= 0) {

            return "LONGBLOB";
        }

        if (length <= 65_532) {
            return "VARBINARY(" + length + ")";
        }

        if (length <= 16_777_215) {
            return "MEDIUMBLOB";
        }

        return "LONGBLOB";
    }

    private static String decimalType(
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
                Math.max(
                        1,
                        Math.min(
                                precision,
                                65));

        scale =
                Math.max(
                        0,
                        Math.min(
                                scale,
                                Math.min(
                                        30,
                                        precision)));

        return "DECIMAL("
                + precision
                + ","
                + scale
                + ")";
    }

    private static String temporalType(
            String type,
            Integer precision) {

        if (precision == null
                || precision <= 0) {

            return type;
        }

        return type
                + "("
                + Math.min(
                precision,
                MAX_TIME_PRECISION)
                + ")";
    }

    private static boolean isInteger(
            SqlType sqlType) {

        return sqlType == SqlType.TINYINT
                || sqlType == SqlType.SMALLINT
                || sqlType == SqlType.INT
                || sqlType == SqlType.BIGINT;
    }

    private static boolean isNumeric(
            SqlType sqlType) {

        return isInteger(sqlType)
                || sqlType == SqlType.FLOAT
                || sqlType == SqlType.DOUBLE
                || sqlType == SqlType.DECIMAL;
    }

    private static String normalizeType(
            String value) {

        String normalized =
                normalize(value);

        if (normalized == null) {
            return "";
        }

        return normalized.toUpperCase(
                Locale.ROOT);
    }

    private static String firstNonEmpty(
            String first,
            String second) {

        String normalized =
                normalize(first);

        return normalized == null
                ? normalize(second)
                : normalized;
    }

    private static Integer getInteger(
            ResultSet resultSet,
            String column)
            throws SQLException {

        Object value =
                resultSet.getObject(column);

        return value == null
                ? null
                : ((Number) value)
                .intValue();
    }

    private static Long getLong(
            ResultSet resultSet,
            String column)
            throws SQLException {

        Object value =
                resultSet.getObject(column);

        return value == null
                ? null
                : ((Number) value)
                .longValue();
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

        return normalize(value) != null;
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

    /**
     * 自定义查询 ResultSetMetaData 类型映射。
     */
    @Override
    public Column map(
            ResultSetMetaData metadata,
            int columnIndex)
            throws SQLException {

        String name =
                firstNonEmpty(
                        metadata.getColumnLabel(
                                columnIndex),
                        metadata.getColumnName(
                                columnIndex));

        int jdbcType =
                metadata.getColumnType(
                        columnIndex);

        String sourceType =
                metadata.getColumnTypeName(
                        columnIndex);

        int precision =
                metadata.getPrecision(
                        columnIndex);

        int scale =
                metadata.getScale(
                        columnIndex);

        boolean nullable =
                metadata.isNullable(
                        columnIndex)
                        != ResultSetMetaData
                        .columnNoNulls;

        FluxDataType<?> dataType =
                mapJdbcType(
                        name,
                        jdbcType,
                        sourceType,
                        precision,
                        scale,
                        false);

        Column.Builder builder =
                Column.builder(
                        name,
                        dataType)
                        .nullable(nullable)
                        .sourceType(sourceType);

        applyTypeProperties(
                builder,
                dataType.getSqlType(),
                precision,
                scale);

        return builder.build();
    }

    /**
     * MySqlCatalog 从 INFORMATION_SCHEMA.COLUMNS 读取字段时使用。
     */
    public Column toColumn(
            ResultSet resultSet)
            throws SQLException {

        String name =
                resultSet.getString(
                        "COLUMN_NAME");

        String dataTypeName =
                resultSet.getString(
                        "DATA_TYPE");

        String columnType =
                resultSet.getString(
                        "COLUMN_TYPE");

        boolean unsigned =
                columnType != null
                        && columnType
                        .toLowerCase(
                                Locale.ROOT)
                        .contains("unsigned");

        Integer precision =
                getInteger(
                        resultSet,
                        "NUMERIC_PRECISION");

        Integer scale =
                getInteger(
                        resultSet,
                        "NUMERIC_SCALE");

        Integer dateTimePrecision =
                getInteger(
                        resultSet,
                        "DATETIME_PRECISION");

        Long characterLength =
                getLong(
                        resultSet,
                        "CHARACTER_MAXIMUM_LENGTH");

        boolean nullable =
                "YES".equalsIgnoreCase(
                        resultSet.getString(
                                "IS_NULLABLE"));

        String extra =
                normalize(
                        resultSet.getString(
                                "EXTRA"));

        FluxDataType<?> fluxType =
                mapMySqlType(
                        name,
                        dataTypeName,
                        columnType,
                        precision,
                        scale,
                        unsigned);

        Column.Builder builder =
                Column.builder(
                        name,
                        fluxType)
                        .nullable(nullable)
                        .defaultValue(
                                resultSet.getObject(
                                        "COLUMN_DEFAULT"))
                        .autoIncrement(
                                extra != null
                                        && extra.toLowerCase(
                                        Locale.ROOT)
                                        .contains(
                                                "auto_increment"))
                        .comment(
                                resultSet.getString(
                                        "COLUMN_COMMENT"))
                        .sourceType(columnType)
                        .attribute(
                                "unsigned",
                                String.valueOf(
                                        unsigned));

        String charset =
                normalize(
                        resultSet.getString(
                                "CHARACTER_SET_NAME"));

        String collation =
                normalize(
                        resultSet.getString(
                                "COLLATION_NAME"));

        if (charset != null) {
            builder.attribute(
                    "charset",
                    charset);
        }

        if (collation != null) {
            builder.attribute(
                    "collation",
                    collation);
        }

        if (extra != null) {
            builder.attribute(
                    "extra",
                    extra);
        }

        SqlType sqlType =
                fluxType.getSqlType();

        if (sqlType == SqlType.STRING
                || sqlType == SqlType.BYTES) {

            builder.length(
                    characterLength);
        } else if (sqlType
                == SqlType.DECIMAL) {

            builder.precision(
                    precision);

            builder.scale(scale);
        } else if (sqlType == SqlType.TIME
                || sqlType == SqlType.TIMESTAMP
                || sqlType
                == SqlType.TIMESTAMP_TZ) {

            builder.precision(
                    dateTimePrecision);
        } else if (isInteger(sqlType)) {
            builder.precision(
                    precision);
        }

        return builder.build();
    }

    @Override
    public String toDatabaseType(
            Column column) {

        return toDatabaseType(
                column,
                false);
    }

    /**
     * preserveSourceType=true 时，优先保留 MySQL 原始字段类型。
     */
    public String toDatabaseType(
            Column column,
            boolean preserveSourceType) {

        if (preserveSourceType
                && hasText(
                column.getSourceType())) {

            return column.getSourceType();
        }

        SqlType sqlType =
                column.getDataType()
                        .getSqlType();

        String type;

        switch (sqlType) {
            case STRING:
                type = stringType(column);
                break;

            case BOOLEAN:
                type = "TINYINT(1)";
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
                type = decimalType(column);
                break;

            case BYTES:
                type = binaryType(column);
                break;

            case DATE:
                type = "DATE";
                break;

            case TIME:
                type = temporalType(
                        "TIME",
                        column.getPrecision());
                break;

            case TIMESTAMP:
                type = temporalType(
                        "DATETIME",
                        column.getPrecision());
                break;

            case TIMESTAMP_TZ:
                type = temporalType(
                        "TIMESTAMP",
                        column.getPrecision());
                break;

            default:
                throw new IllegalArgumentException(
                        "MySQL 不支持 Flux 类型："
                                + sqlType
                                + "，column="
                                + column.getName());
        }

        if (isNumeric(sqlType)
                && Boolean.parseBoolean(
                column.getAttributes()
                        .get("unsigned"))) {

            type += " UNSIGNED";
        }

        return type;
    }

    private FluxDataType<?> mapMySqlType(
            String fieldName,
            String dataTypeName,
            String columnType,
            Integer precision,
            Integer scale,
            boolean unsigned) {

        String normalized =
                normalizeType(dataTypeName);

        switch (normalized) {
            case "BIT":
                if (valueOrDefault(
                        precision,
                        1)
                        <= 1) {

                    return BasicType.BOOLEAN_TYPE;
                }

                return BasicType.BYTES_TYPE;

            case "BOOL":
            case "BOOLEAN":
                return BasicType.BOOLEAN_TYPE;

            case "TINYINT":
                if (intTypeNarrowing
                        && columnType != null
                        && columnType
                        .toLowerCase(
                                Locale.ROOT)
                        .startsWith(
                                "tinyint(1)")) {

                    return BasicType.BOOLEAN_TYPE;
                }

                if (!intTypeNarrowing) {
                    return BasicType.INT_TYPE;
                }

                return unsigned
                        ? BasicType.SHORT_TYPE
                        : BasicType.BYTE_TYPE;

            case "SMALLINT":
                if (!intTypeNarrowing) {
                    return BasicType.INT_TYPE;
                }

                return unsigned
                        ? BasicType.INT_TYPE
                        : BasicType.SHORT_TYPE;

            case "MEDIUMINT":
            case "INT":
            case "INTEGER":
            case "YEAR":
                return unsigned
                        ? BasicType.LONG_TYPE
                        : BasicType.INT_TYPE;

            case "BIGINT":
                return unsigned
                        ? new DecimalType(20, 0)
                        : BasicType.LONG_TYPE;

            case "FLOAT":
                return BasicType.FLOAT_TYPE;

            case "DOUBLE":
            case "REAL":
                return BasicType.DOUBLE_TYPE;

            case "DECIMAL":
            case "NUMERIC":
                return decimal(
                        fieldName,
                        precision,
                        scale);

            case "DATE":
                return BasicType.DATE_TYPE;

            case "TIME":
                return BasicType.TIME_TYPE;

            case "DATETIME":
                return BasicType.TIMESTAMP_TYPE;

            case "TIMESTAMP":
                return BasicType.TIMESTAMP_TZ_TYPE;

            case "BINARY":
            case "VARBINARY":
            case "TINYBLOB":
            case "BLOB":
            case "MEDIUMBLOB":
            case "LONGBLOB":
            case "GEOMETRY":
            case "POINT":
            case "LINESTRING":
            case "POLYGON":
            case "MULTIPOINT":
            case "MULTILINESTRING":
            case "MULTIPOLYGON":
            case "GEOMETRYCOLLECTION":
                return BasicType.BYTES_TYPE;

            case "CHAR":
            case "VARCHAR":
            case "TINYTEXT":
            case "TEXT":
            case "MEDIUMTEXT":
            case "LONGTEXT":
            case "ENUM":
            case "SET":
            case "JSON":
                return BasicType.STRING_TYPE;

            default:
                throw new IllegalArgumentException(
                        "不支持 MySQL 类型："
                                + dataTypeName
                                + "，column="
                                + fieldName);
        }
    }

    private FluxDataType<?> mapJdbcType(
            String fieldName,
            int jdbcType,
            String sourceType,
            int precision,
            int scale,
            boolean unsigned) {

        switch (jdbcType) {
            case Types.BOOLEAN:
                return BasicType.BOOLEAN_TYPE;

            case Types.BIT:
                return precision <= 1
                        ? BasicType.BOOLEAN_TYPE
                        : BasicType.BYTES_TYPE;

            case Types.TINYINT:
                if (intTypeNarrowing
                        && "tinyint".equalsIgnoreCase(
                        sourceType)
                        && precision == 1) {

                    return BasicType.BOOLEAN_TYPE;
                }

                return intTypeNarrowing
                        ? BasicType.BYTE_TYPE
                        : BasicType.INT_TYPE;

            case Types.SMALLINT:
                return intTypeNarrowing
                        ? BasicType.SHORT_TYPE
                        : BasicType.INT_TYPE;

            case Types.INTEGER:
                return BasicType.INT_TYPE;

            case Types.BIGINT:
                return BasicType.LONG_TYPE;

            case Types.REAL:
            case Types.FLOAT:
                return BasicType.FLOAT_TYPE;

            case Types.DOUBLE:
                return BasicType.DOUBLE_TYPE;

            case Types.NUMERIC:
            case Types.DECIMAL:
                return decimal(
                        fieldName,
                        precision,
                        scale);

            case Types.DATE:
                return BasicType.DATE_TYPE;

            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                return BasicType.TIME_TYPE;

            case Types.TIMESTAMP:
                return BasicType.TIMESTAMP_TYPE;

            case Types.TIMESTAMP_WITH_TIMEZONE:
                return BasicType.TIMESTAMP_TZ_TYPE;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return BasicType.BYTES_TYPE;

            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
            case Types.SQLXML:
            case Types.OTHER:
                return BasicType.STRING_TYPE;

            default:
                throw new IllegalArgumentException(
                        "不支持 JDBC 类型："
                                + jdbcType
                                + "，sourceType="
                                + sourceType
                                + "，column="
                                + fieldName);
        }
    }

    private DecimalType decimal(
            String fieldName,
            Integer precision,
            Integer scale) {

        int safePrecision =
                precision == null
                        || precision <= 0
                        ? DEFAULT_DECIMAL_PRECISION
                        : precision;

        int safeScale =
                scale == null
                        ? 0
                        : Math.max(scale, 0);

        if (safePrecision
                > MAX_FLUX_DECIMAL_PRECISION) {

            LOG.warn(
                    "字段 {} 的 DECIMAL 精度 {} 超过 Flux 最大精度 {}，将进行收缩",
                    fieldName,
                    safePrecision,
                    MAX_FLUX_DECIMAL_PRECISION);

            safePrecision =
                    MAX_FLUX_DECIMAL_PRECISION;

            safeScale =
                    Math.min(
                            safeScale,
                            Math.min(
                                    DEFAULT_DECIMAL_SCALE,
                                    safePrecision));
        }

        safeScale =
                Math.min(
                        safeScale,
                        safePrecision);

        return new DecimalType(
                safePrecision,
                safeScale);
    }
}
