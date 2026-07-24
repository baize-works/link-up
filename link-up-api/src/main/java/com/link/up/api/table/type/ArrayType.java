package com.link.up.api.table.type;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * 数组数据类型。
 * <p>
 * 使用统一的 ArrayType 即可，不再为 Decimal、时间等类型
 * 分别创建独立的数组类型。
 *
 * @param <E> 数组元素类型
 */
public final class ArrayType<E> implements FluxDataType<E[]> {

    private static final long serialVersionUID = 1L;

    private final Class<E[]> arrayClass;
    private final FluxDataType<E> elementType;

    public ArrayType(Class<E[]> arrayClass, FluxDataType<E> elementType) {
        this.arrayClass = Objects.requireNonNull(arrayClass, "arrayClass must not be null");
        this.elementType = Objects.requireNonNull(elementType, "elementType must not be null");

        if (!arrayClass.isArray()) {
            throw new IllegalArgumentException("arrayClass must be an array type");
        }
    }

    /**
     * 根据元素类型创建数组类型。
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayType<E> of(FluxDataType<E> elementType) {
        Objects.requireNonNull(elementType, "elementType must not be null");

        Class<E[]> arrayClass =
                (Class<E[]>)
                        Array.newInstance(elementType.getTypeClass(), 0)
                                .getClass();

        return new ArrayType<>(arrayClass, elementType);
    }

    public FluxDataType<E> getElementType() {
        return elementType;
    }

    @Override
    public Class<E[]> getTypeClass() {
        return arrayClass;
    }

    @Override
    public SqlType getSqlType() {
        return SqlType.ARRAY;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ArrayType)) {
            return false;
        }

        ArrayType<?> that = (ArrayType<?>) obj;
        return Objects.equals(arrayClass, that.arrayClass)
                && Objects.equals(elementType, that.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrayClass, elementType);
    }

    @Override
    public String toString() {
        return String.format("ARRAY<%s>", elementType);
    }
}