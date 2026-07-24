package com.link.up.api.source;

import com.link.up.api.table.type.FluxRow;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * A stable, deliberately approximate estimate of the memory occupied by a record.
 *
 * <p>The estimate is intended for channel backpressure, not for accounting or serialization.
 */
@FunctionalInterface
public interface RecordSizeEstimator<T> {

    long UNKNOWN_RECORD_BYTES = 256L;

    static RecordSizeEstimator<FluxRow> fluxRowEstimator() {
        return FluxRow::estimatedSizeBytes;
    }

    /**
     * Estimates common JVM values and uses a conservative value for unknown objects.
     */
    static long estimateObjectSizeBytes(Object value) {
        if (value == null) return 4L;
        if (value instanceof Boolean || value instanceof Byte) return 16L;
        if (value instanceof Character || value instanceof Short) return 16L;
        if (value instanceof Integer || value instanceof Float) return 16L;
        if (value instanceof Long || value instanceof Double) return 24L;
        if (value instanceof String) return 40L + ((String) value).length() * 2L;
        if (value instanceof byte[]) return 24L + ((byte[]) value).length;
        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            return 48L + Math.max(16L, decimal.unscaledValue().toByteArray().length);
        }
        if (value instanceof Date || value instanceof TemporalAccessor) return 32L;
        if (value instanceof FluxRow) return ((FluxRow) value).estimatedSizeBytes();
        return UNKNOWN_RECORD_BYTES;
    }

    long estimateSizeBytes(T record);
}
