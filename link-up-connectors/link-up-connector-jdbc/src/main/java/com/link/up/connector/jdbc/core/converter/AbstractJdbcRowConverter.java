package com.link.up.connector.jdbc.core.converter;

import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.catalog.TableSchema;
import com.link.up.api.table.type.FluxDataType;
import com.link.up.api.table.type.FluxRow;
import com.link.up.api.table.type.SqlType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.Objects;

/**
 * JDBC 行转换器基础实现。
 * <p>
 * 当前支持关系型数据库常用基础类型：
 * <p>
 * STRING、BOOLEAN、整数、浮点数、DECIMAL、
 * DATE、TIME、TIMESTAMP、TIMESTAMP_TZ、BYTES。
 * <p>
 * ARRAY、MAP、ROW 等复杂类型由具体数据库方言按需扩展。
 */
public abstract class AbstractJdbcRowConverter
        implements JdbcRowConverter {

    private static final long serialVersionUID = 1L;

    @Override
    public FluxRow read(
            ResultSet resultSet,
            TableSchema tableSchema)
            throws SQLException {

        Objects.requireNonNull(resultSet, "resultSet must not be null");
        Objects.requireNonNull(tableSchema, "tableSchema must not be null");

        List<Column> columns = tableSchema.getColumns();
        FluxRow row = new FluxRow(columns.size());

        for (int fieldIndex = 0; fieldIndex < columns.size(); fieldIndex++) {
            Column column = columns.get(fieldIndex);
            int resultSetIndex = fieldIndex + 1;

            try {
                Object value =
                        readValue(
                                resultSet,
                                resultSetIndex,
                                column);

                row.setField(fieldIndex, value);
            } catch (SQLException e) {
                throw new SQLException(
                        "读取 JDBC 字段失败，converter="
                                + name()
                                + ", field="
                                + column.getName()
                                + ", index="
                                + resultSetIndex
                                + ", type="
                                + column.getDataType().getSqlType(),
                        e);
            } catch (RuntimeException e) {
                throw new SQLException(
                        "转换 JDBC 字段失败，converter="
                                + name()
                                + ", field="
                                + column.getName()
                                + ", index="
                                + resultSetIndex
                                + ", type="
                                + column.getDataType().getSqlType(),
                        e);
            }
        }

        return row;
    }

    @Override
    public void write(
            PreparedStatement statement,
            FluxRow row,
            TableSchema tableSchema)
            throws SQLException {

        Objects.requireNonNull(
                statement,
                "statement must not be null");

        Objects.requireNonNull(
                row,
                "row must not be null");

        Objects.requireNonNull(
                tableSchema,
                "tableSchema must not be null");

        List<Column> columns =
                tableSchema.getColumns();

        for (int fieldIndex = 0;
             fieldIndex < columns.size();
             fieldIndex++) {

            Column column =
                    columns.get(fieldIndex);

            Object value;

            try {
                value = row.getField(fieldIndex);
            } catch (RuntimeException e) {
                throw new SQLException(
                        "FluxRow 字段数量与 TableSchema 不匹配，"
                                + "field="
                                + column.getName()
                                + ", index="
                                + fieldIndex
                                + ", schemaFieldCount="
                                + columns.size(),
                        e);
            }

            int statementIndex =
                    fieldIndex + 1;

            try {
                if (value == null) {
                    writeNull(
                            statement,
                            statementIndex,
                            column);
                } else {
                    writeValue(
                            statement,
                            statementIndex,
                            value,
                            column);
                }
            } catch (SQLException e) {
                throw new SQLException(
                        "绑定 JDBC 参数失败，converter="
                                + name()
                                + ", field="
                                + column.getName()
                                + ", index="
                                + statementIndex
                                + ", type="
                                + column.getDataType().getSqlType()
                                + ", valueType="
                                + (value == null
                                ? "null"
                                : value.getClass().getName()),
                        e);
            } catch (RuntimeException e) {
                throw new SQLException(
                        "转换 JDBC 写入字段失败，converter="
                                + name()
                                + ", field="
                                + column.getName()
                                + ", index="
                                + statementIndex
                                + ", type="
                                + column.getDataType().getSqlType(),
                        e);
            }
        }
    }

    /**
     * 读取一个 JDBC 字段。
     * <p>
     * 数据库存在特殊类型时，子类可以覆盖该方法。
     */
    protected Object readValue(
            ResultSet resultSet,
            int index,
            Column column)
            throws SQLException {

        FluxDataType<?> dataType =
                column.getDataType();

        SqlType sqlType =
                dataType.getSqlType();

        switch (sqlType) {
            case STRING:
                return resultSet.getString(index);

            case BOOLEAN:
                return readBoolean(
                        resultSet,
                        index);

            case TINYINT:
                return readByte(
                        resultSet,
                        index);

            case SMALLINT:
                return readShort(
                        resultSet,
                        index);

            case INT:
                return readInteger(
                        resultSet,
                        index);

            case BIGINT:
                return readLong(
                        resultSet,
                        index);

            case FLOAT:
                return readFloat(
                        resultSet,
                        index);

            case DOUBLE:
                return readDouble(
                        resultSet,
                        index);

            case DECIMAL:
                return resultSet.getBigDecimal(index);

            case DATE:
                return readDate(
                        resultSet,
                        index);

            case TIME:
                return readTime(
                        resultSet,
                        index);

            case TIMESTAMP:
                return readTimestamp(
                        resultSet,
                        index);

            case TIMESTAMP_TZ:
                return readTimestampWithTimezone(
                        resultSet,
                        index);

            case BYTES:
                return resultSet.getBytes(index);

            case NULL:
                return null;

            case ARRAY:
            case MAP:
            case ROW:
            default:
                throw unsupportedType(
                        column,
                        "读取");
        }
    }

    /**
     * 写入一个非空 JDBC 参数。
     * <p>
     * 数据库存在特殊写入逻辑时，子类可以覆盖该方法。
     */
    protected void writeValue(
            PreparedStatement statement,
            int index,
            Object value,
            Column column)
            throws SQLException {

        SqlType sqlType =
                column.getDataType()
                        .getSqlType();

        switch (sqlType) {
            case STRING:
                statement.setString(
                        index,
                        asString(value));
                break;

            case BOOLEAN:
                statement.setBoolean(
                        index,
                        asBoolean(value));
                break;

            case TINYINT:
                statement.setByte(
                        index,
                        asNumber(value).byteValue());
                break;

            case SMALLINT:
                statement.setShort(
                        index,
                        asNumber(value).shortValue());
                break;

            case INT:
                statement.setInt(
                        index,
                        asNumber(value).intValue());
                break;

            case BIGINT:
                statement.setLong(
                        index,
                        asNumber(value).longValue());
                break;

            case FLOAT:
                statement.setFloat(
                        index,
                        asNumber(value).floatValue());
                break;

            case DOUBLE:
                statement.setDouble(
                        index,
                        asNumber(value).doubleValue());
                break;

            case DECIMAL:
                statement.setBigDecimal(
                        index,
                        asBigDecimal(value));
                break;

            case DATE:
                writeDate(
                        statement,
                        index,
                        value);
                break;

            case TIME:
                writeTime(
                        statement,
                        index,
                        asLocalTime(value));
                break;

            case TIMESTAMP:
                writeTimestamp(
                        statement,
                        index,
                        value);
                break;

            case TIMESTAMP_TZ:
                writeTimestampWithTimezone(
                        statement,
                        index,
                        value);
                break;

            case BYTES:
                statement.setBytes(
                        index,
                        asBytes(value));
                break;

            case NULL:
                statement.setNull(
                        index,
                        Types.NULL);
                break;

            case ARRAY:
            case MAP:
            case ROW:
            default:
                throw unsupportedType(
                        column,
                        "写入");
        }
    }

    /**
     * 写入空值。
     * <p>
     * 默认根据 Flux SqlType 选择 JDBC Types。
     * 数据库存在特殊 Null 类型要求时可以覆盖。
     */
    protected void writeNull(
            PreparedStatement statement,
            int index,
            Column column)
            throws SQLException {

        statement.setNull(
                index,
                resolveJdbcType(column));
    }

    /**
     * 将 Flux 类型转换为 java.sql.Types。
     */
    protected int resolveJdbcType(
            Column column) {

        SqlType sqlType =
                column.getDataType()
                        .getSqlType();

        switch (sqlType) {
            case STRING:
                return Types.VARCHAR;

            case BOOLEAN:
                return Types.BOOLEAN;

            case TINYINT:
                return Types.TINYINT;

            case SMALLINT:
                return Types.SMALLINT;

            case INT:
                return Types.INTEGER;

            case BIGINT:
                return Types.BIGINT;

            case FLOAT:
                return Types.FLOAT;

            case DOUBLE:
                return Types.DOUBLE;

            case DECIMAL:
                return Types.DECIMAL;

            case DATE:
                return Types.DATE;

            case TIME:
                return Types.TIME;

            case TIMESTAMP:
                return Types.TIMESTAMP;

            case TIMESTAMP_TZ:
                return Types.TIMESTAMP_WITH_TIMEZONE;

            case BYTES:
                return Types.VARBINARY;

            case ARRAY:
                return Types.ARRAY;

            case NULL:
                return Types.NULL;

            case MAP:
            case ROW:
            default:
                return Types.OTHER;
        }
    }

    protected Boolean readBoolean(
            ResultSet resultSet,
            int index)
            throws SQLException {

        boolean value =
                resultSet.getBoolean(index);

        return resultSet.wasNull()
                ? null
                : value;
    }

    protected Byte readByte(
            ResultSet resultSet,
            int index)
            throws SQLException {

        byte value =
                resultSet.getByte(index);

        return resultSet.wasNull()
                ? null
                : value;
    }

    protected Short readShort(
            ResultSet resultSet,
            int index)
            throws SQLException {

        short value =
                resultSet.getShort(index);

        return resultSet.wasNull()
                ? null
                : value;
    }

    protected Integer readInteger(
            ResultSet resultSet,
            int index)
            throws SQLException {

        int value =
                resultSet.getInt(index);

        return resultSet.wasNull()
                ? null
                : value;
    }

    protected Long readLong(
            ResultSet resultSet,
            int index)
            throws SQLException {

        long value =
                resultSet.getLong(index);

        return resultSet.wasNull()
                ? null
                : value;
    }

    protected Float readFloat(
            ResultSet resultSet,
            int index)
            throws SQLException {

        float value =
                resultSet.getFloat(index);

        return resultSet.wasNull()
                ? null
                : value;
    }

    protected Double readDouble(
            ResultSet resultSet,
            int index)
            throws SQLException {

        double value =
                resultSet.getDouble(index);

        return resultSet.wasNull()
                ? null
                : value;
    }

    protected LocalDate readDate(
            ResultSet resultSet,
            int index)
            throws SQLException {

        try {
            return resultSet.getObject(
                    index,
                    LocalDate.class);
        } catch (SQLFeatureNotSupportedException
                | AbstractMethodError e) {

            java.sql.Date date =
                    resultSet.getDate(index);

            return date == null
                    ? null
                    : date.toLocalDate();
        }
    }

    protected LocalTime readTime(
            ResultSet resultSet,
            int index)
            throws SQLException {

        try {
            return resultSet.getObject(
                    index,
                    LocalTime.class);
        } catch (SQLFeatureNotSupportedException
                | AbstractMethodError e) {

            Time time =
                    resultSet.getTime(index);

            return time == null
                    ? null
                    : time.toLocalTime();
        }
    }

    protected LocalDateTime readTimestamp(
            ResultSet resultSet,
            int index)
            throws SQLException {

        try {
            return resultSet.getObject(
                    index,
                    LocalDateTime.class);
        } catch (SQLFeatureNotSupportedException
                | AbstractMethodError e) {

            Timestamp timestamp =
                    resultSet.getTimestamp(index);

            return timestamp == null
                    ? null
                    : timestamp.toLocalDateTime();
        }
    }

    protected OffsetDateTime readTimestampWithTimezone(
            ResultSet resultSet,
            int index)
            throws SQLException {

        try {
            return resultSet.getObject(
                    index,
                    OffsetDateTime.class);
        } catch (SQLFeatureNotSupportedException
                | AbstractMethodError e) {

            Timestamp timestamp =
                    resultSet.getTimestamp(index);

            return timestamp == null
                    ? null
                    : timestamp.toInstant()
                    .atOffset(ZoneOffset.UTC);
        }
    }

    protected void writeDate(
            PreparedStatement statement,
            int index,
            Object value)
            throws SQLException {

        if (value instanceof LocalDate) {
            statement.setDate(
                    index,
                    java.sql.Date.valueOf(
                            (LocalDate) value));

            return;
        }

        if (value instanceof java.sql.Date) {
            statement.setDate(
                    index,
                    (java.sql.Date) value);

            return;
        }

        throw new IllegalArgumentException(
                "无法转换为 DATE："
                        + value.getClass().getName());
    }

    /**
     * 默认 TIME 写入方式。
     * <p>
     * MySQL 可以覆盖该方法，使用 Timestamp 保留小数秒。
     */
    protected void writeTime(
            PreparedStatement statement,
            int index,
            LocalTime value)
            throws SQLException {

        statement.setTime(
                index,
                Time.valueOf(value));
    }

    protected void writeTimestamp(
            PreparedStatement statement,
            int index,
            Object value)
            throws SQLException {

        if (value instanceof LocalDateTime) {
            statement.setTimestamp(
                    index,
                    Timestamp.valueOf(
                            (LocalDateTime) value));

            return;
        }

        if (value instanceof Timestamp) {
            statement.setTimestamp(
                    index,
                    (Timestamp) value);

            return;
        }

        throw new IllegalArgumentException(
                "无法转换为 TIMESTAMP："
                        + value.getClass().getName());
    }

    protected void writeTimestampWithTimezone(
            PreparedStatement statement,
            int index,
            Object value)
            throws SQLException {

        if (!(value instanceof OffsetDateTime)) {
            throw new IllegalArgumentException(
                    "TIMESTAMP_TZ 必须使用 OffsetDateTime，实际类型："
                            + value.getClass().getName());
        }

        OffsetDateTime offsetDateTime =
                (OffsetDateTime) value;

        try {
            statement.setObject(
                    index,
                    offsetDateTime);
        } catch (SQLFeatureNotSupportedException
                | AbstractMethodError e) {

            statement.setTimestamp(
                    index,
                    Timestamp.from(
                            offsetDateTime.toInstant()));
        }
    }

    protected Number asNumber(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }

        if (value instanceof String) {
            try {
                return new BigDecimal(
                        ((String) value).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "无法转换为数字："
                                + value,
                        e);
            }
        }

        throw new IllegalArgumentException(
                "无法转换为数字，valueType="
                        + value.getClass().getName());
    }

    protected BigDecimal asBigDecimal(
            Object value) {

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number
                || value instanceof String) {

            return new BigDecimal(
                    value.toString());
        }

        throw new IllegalArgumentException(
                "无法转换为 BigDecimal，valueType="
                        + value.getClass().getName());
    }

    protected Boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value)
                    .intValue()
                    != 0;
        }

        if (value instanceof String) {
            String text =
                    ((String) value)
                            .trim();

            if ("1".equals(text)
                    || "true".equalsIgnoreCase(text)
                    || "yes".equalsIgnoreCase(text)) {

                return true;
            }

            if ("0".equals(text)
                    || "false".equalsIgnoreCase(text)
                    || "no".equalsIgnoreCase(text)) {

                return false;
            }
        }

        throw new IllegalArgumentException(
                "无法转换为 Boolean，value="
                        + value);
    }

    protected String asString(Object value) {
        return value instanceof String
                ? (String) value
                : String.valueOf(value);
    }

    protected byte[] asBytes(Object value) {
        if (value instanceof byte[]) {
            return (byte[]) value;
        }

        throw new IllegalArgumentException(
                "无法转换为 byte[]，valueType="
                        + value.getClass().getName());
    }

    protected LocalTime asLocalTime(
            Object value) {

        if (value instanceof LocalTime) {
            return (LocalTime) value;
        }

        if (value instanceof Time) {
            return ((Time) value)
                    .toLocalTime();
        }

        if (value instanceof String) {
            return LocalTime.parse(
                    ((String) value).trim());
        }

        throw new IllegalArgumentException(
                "无法转换为 LocalTime，valueType="
                        + value.getClass().getName());
    }

    protected SQLException unsupportedType(
            Column column,
            String operation) {

        return new SQLException(
                "JDBC 转换器 "
                        + name()
                        + " 暂不支持"
                        + operation
                        + "类型 "
                        + column.getDataType().getSqlType()
                        + "，field="
                        + column.getName());
    }
}