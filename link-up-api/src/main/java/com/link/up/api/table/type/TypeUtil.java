package com.link.up.api.table.type;

import java.util.Objects;

/**
 * 数据类型转换判断工具。
 */
public final class TypeUtil {

    private TypeUtil() {
    }

    /**
     * 判断一个类型是否可以安全或常规地转换为另一个类型。
     */
    public static boolean canConvert(
            FluxDataType<?> sourceType,
            FluxDataType<?> targetType) {

        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");

        if (sourceType.equals(targetType)) {
            return true;
        }

        SqlType source = sourceType.getSqlType();
        SqlType target = targetType.getSqlType();

        // 空值可以写入任意支持空值的字段。
        if (source == SqlType.NULL) {
            return true;
        }

        // 所有基础类型都可以转换为字符串。
        if (target == SqlType.STRING) {
            return true;
        }

        // 数组类型需要递归判断元素类型。
        if (source == SqlType.ARRAY && target == SqlType.ARRAY) {
            ArrayType<?> sourceArray = (ArrayType<?>) sourceType;
            ArrayType<?> targetArray = (ArrayType<?>) targetType;

            return canConvert(
                    sourceArray.getElementType(),
                    targetArray.getElementType());
        }

        switch (source) {
            case TINYINT:
                return target == SqlType.SMALLINT
                        || target == SqlType.INT
                        || target == SqlType.BIGINT
                        || target == SqlType.FLOAT
                        || target == SqlType.DOUBLE
                        || target == SqlType.DECIMAL;

            case SMALLINT:
                return target == SqlType.INT
                        || target == SqlType.BIGINT
                        || target == SqlType.FLOAT
                        || target == SqlType.DOUBLE
                        || target == SqlType.DECIMAL;

            case INT:
                return target == SqlType.BIGINT
                        || target == SqlType.FLOAT
                        || target == SqlType.DOUBLE
                        || target == SqlType.DECIMAL;

            case BIGINT:
                return target == SqlType.FLOAT
                        || target == SqlType.DOUBLE
                        || target == SqlType.DECIMAL;

            case FLOAT:
                return target == SqlType.DOUBLE
                        || target == SqlType.DECIMAL;

            case DOUBLE:
                return target == SqlType.DECIMAL;

            default:
                return false;
        }
    }

    public static boolean isNumeric(FluxDataType<?> dataType) {
        Objects.requireNonNull(dataType, "dataType must not be null");

        switch (dataType.getSqlType()) {
            case TINYINT:
            case SMALLINT:
            case INT:
            case BIGINT:
            case FLOAT:
            case DOUBLE:
            case DECIMAL:
                return true;

            default:
                return false;
        }
    }
}