package com.link.up.api.table.type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Flux 基础数据类型。
 * <p>
 * 时间类型和字节类型统一放在这里，不再单独创建 LocalTimeType、
 * PrimitiveByteArrayType 等类型。
 */
public class BasicType<T> implements FluxDataType<T> {

    public static final BasicType<String> STRING_TYPE =
            new BasicType<>(String.class, SqlType.STRING);
    public static final BasicType<Boolean> BOOLEAN_TYPE =
            new BasicType<>(Boolean.class, SqlType.BOOLEAN);
    public static final BasicType<Byte> BYTE_TYPE =
            new BasicType<>(Byte.class, SqlType.TINYINT);
    public static final BasicType<Short> SHORT_TYPE =
            new BasicType<>(Short.class, SqlType.SMALLINT);
    public static final BasicType<Integer> INT_TYPE =
            new BasicType<>(Integer.class, SqlType.INT);
    public static final BasicType<Long> LONG_TYPE =
            new BasicType<>(Long.class, SqlType.BIGINT);
    public static final BasicType<Float> FLOAT_TYPE =
            new BasicType<>(Float.class, SqlType.FLOAT);
    public static final BasicType<Double> DOUBLE_TYPE =
            new BasicType<>(Double.class, SqlType.DOUBLE);
    public static final BasicType<byte[]> BYTES_TYPE =
            new BasicType<>(byte[].class, SqlType.BYTES);
    public static final BasicType<LocalDate> DATE_TYPE =
            new BasicType<>(LocalDate.class, SqlType.DATE);
    public static final BasicType<LocalTime> TIME_TYPE =
            new BasicType<>(LocalTime.class, SqlType.TIME);
    public static final BasicType<LocalDateTime> TIMESTAMP_TYPE =
            new BasicType<>(LocalDateTime.class, SqlType.TIMESTAMP);
    public static final BasicType<OffsetDateTime> TIMESTAMP_TZ_TYPE =
            new BasicType<>(OffsetDateTime.class, SqlType.TIMESTAMP_TZ);
    public static final BasicType<Void> NULL_TYPE =
            new BasicType<>(Void.class, SqlType.NULL);
    private static final long serialVersionUID = 1L;
    private final Class<T> typeClass;
    private final SqlType sqlType;

    protected BasicType(Class<T> typeClass, SqlType sqlType) {
        this.typeClass = Objects.requireNonNull(typeClass, "typeClass must not be null");
        this.sqlType = Objects.requireNonNull(sqlType, "sqlType must not be null");
    }

    @Override
    public Class<T> getTypeClass() {
        return typeClass;
    }

    @Override
    public SqlType getSqlType() {
        return sqlType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        // 使用具体类型判断，避免 BasicType 和 DecimalType 出现非对称相等。
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        BasicType<?> that = (BasicType<?>) obj;
        return Objects.equals(typeClass, that.typeClass)
                && sqlType == that.sqlType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeClass, sqlType);
    }

    @Override
    public String toString() {
        return sqlType.name();
    }
}