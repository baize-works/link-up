package com.link.up.connector.jdbc.source;


import com.link.up.api.table.type.BasicType;
import com.link.up.api.table.type.DecimalType;
import com.link.up.api.table.type.FluxDataType;
import com.link.up.api.table.type.FluxRowType;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 将 JDBC 字段类型转换为 Flux 数据类型。
 */
public final class JdbcTypeMapper {

    private JdbcTypeMapper() {
    }

    /**
     * 根据 ResultSetMetaData 自动生成 FluxRowType。
     */
    public static FluxRowType from(ResultSetMetaData metadata)
            throws SQLException {

        int columnCount = metadata.getColumnCount();

        String[] fieldNames = new String[columnCount];
        FluxDataType<?>[] fieldTypes =
                new FluxDataType<?>[columnCount];

        for (int i = 1; i <= columnCount; i++) {
            String columnLabel = metadata.getColumnLabel(i);

            if (columnLabel == null || columnLabel.trim().isEmpty()) {
                columnLabel = metadata.getColumnName(i);
            }

            fieldNames[i - 1] = columnLabel;

            fieldTypes[i - 1] =
                    fromJdbcType(
                            metadata.getColumnType(i),
                            metadata.getPrecision(i),
                            metadata.getScale(i),
                            metadata.getColumnTypeName(i));
        }

        return new FluxRowType(fieldNames, fieldTypes);
    }

    private static FluxDataType<?> fromJdbcType(
            int jdbcType,
            int precision,
            int scale,
            String typeName)
            throws SQLException {

        switch (jdbcType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
            case Types.SQLXML:
                return BasicType.STRING_TYPE;

            case Types.BOOLEAN:
            case Types.BIT:
                return BasicType.BOOLEAN_TYPE;

            case Types.TINYINT:
                return BasicType.BYTE_TYPE;

            case Types.SMALLINT:
                return BasicType.SHORT_TYPE;

            case Types.INTEGER:
                return BasicType.INT_TYPE;

            case Types.BIGINT:
                return BasicType.LONG_TYPE;

            case Types.REAL:
                return BasicType.FLOAT_TYPE;

            case Types.FLOAT:
                return precision > 0 && precision <= 24
                        ? BasicType.FLOAT_TYPE
                        : BasicType.DOUBLE_TYPE;

            case Types.DOUBLE:
                return BasicType.DOUBLE_TYPE;

            case Types.NUMERIC:
            case Types.DECIMAL:
                return createDecimalType(precision, scale);

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return BasicType.BYTES_TYPE;

            case Types.DATE:
                return BasicType.DATE_TYPE;

            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                return BasicType.TIME_TYPE;

            case Types.TIMESTAMP:
                return BasicType.TIMESTAMP_TYPE;

            case Types.TIMESTAMP_WITH_TIMEZONE:
                return BasicType.TIMESTAMP_TZ_TYPE;

            case Types.NULL:
                return BasicType.NULL_TYPE;

            /*
             * UUID、JSON、JSONB 等字段通常由驱动返回 Types.OTHER。
             * 离线同步中默认按字符串处理，兼容性更高。
             */
            case Types.OTHER:
                return BasicType.STRING_TYPE;

            case Types.ARRAY:
                throw new SQLException(
                        "JDBC ARRAY requires an explicit element type, typeName="
                                + typeName);

            default:
                throw new SQLException(
                        "Unsupported JDBC type: "
                                + jdbcType
                                + ", typeName="
                                + typeName);
        }
    }

    private static DecimalType createDecimalType(
            int precision,
            int scale) {

        int safeScale = Math.max(scale, 0);
        int safePrecision = precision > 0 ? precision : 38;

        // 部分 JDBC 驱动可能返回 scale 大于 precision，进行保护。
        safePrecision = Math.max(safePrecision, safeScale);

        return new DecimalType(safePrecision, safeScale);
    }
}
