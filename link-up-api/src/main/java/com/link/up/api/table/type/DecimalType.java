package com.link.up.api.table.type;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 高精度数字类型。
 */
public final class DecimalType extends BasicType<BigDecimal> {

    private static final long serialVersionUID = 1L;

    private final int precision;
    private final int scale;

    public DecimalType(int precision, int scale) {
        super(BigDecimal.class, SqlType.DECIMAL);

        if (precision <= 0) {
            throw new IllegalArgumentException("precision must be greater than 0");
        }

        if (scale < 0) {
            throw new IllegalArgumentException("scale must not be less than 0");
        }

        if (scale > precision) {
            throw new IllegalArgumentException("scale must not be greater than precision");
        }

        this.precision = precision;
        this.scale = scale;
    }

    public int getPrecision() {
        return precision;
    }

    public int getScale() {
        return scale;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DecimalType)) {
            return false;
        }

        DecimalType that = (DecimalType) obj;
        return precision == that.precision
                && scale == that.scale;
    }

    @Override
    public int hashCode() {
        return Objects.hash(precision, scale);
    }

    @Override
    public String toString() {
        return String.format("DECIMAL(%d,%d)", precision, scale);
    }
}