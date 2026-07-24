package com.link.up.connector.jdbc.core.split;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数值范围分片器。
 *
 * <p>整数类型按照离散值数量进行切分，避免生成无意义的小数边界。
 * 小数类型按照连续范围等宽切分。
 */
public final class FixedChunkSplitter implements ChunkSplitter<BigDecimal> {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final boolean integral;

    /**
     * 默认按照小数范围处理。
     */
    public FixedChunkSplitter() {
        this(false);
    }

    /**
     * @param integral 是否为整数类型
     */
    public FixedChunkSplitter(boolean integral) {
        this.integral = integral;
    }

    private static int resolveIntegralChunkCount(
            BigInteger cardinality,
            int requestedCount) {

        BigInteger requested = BigInteger.valueOf(requestedCount);
        return cardinality.min(requested).intValue();
    }

    private static void validate(
            BigDecimal lower,
            BigDecimal upper,
            int count) {

        if (lower == null || upper == null) {
            throw new IllegalArgumentException(
                    "numeric bounds must not be null");
        }

        if (lower.compareTo(upper) > 0) {
            throw new IllegalArgumentException(
                    "numeric lower bound must not be greater than upper bound");
        }

        if (count <= 0) {
            throw new IllegalArgumentException(
                    "chunkCount must be greater than 0");
        }
    }

    @Override
    public List<Chunk<BigDecimal>> split(
            BigDecimal lower,
            BigDecimal upper,
            int count) {

        validate(lower, upper, count);

        if (lower.compareTo(upper) == 0) {
            return Collections.singletonList(
                    new Chunk<BigDecimal>(lower, upper, true));
        }

        if (integral) {
            return splitIntegral(lower, upper, count);
        }

        return splitDecimal(lower, upper, count);
    }

    /**
     * 整数范围按实际值数量切分。
     *
     * <p>例如 1～10 切成 3 片：
     *
     * <pre>
     * [1, 5)
     * [5, 8)
     * [8, 10]
     * </pre>
     */
    private List<Chunk<BigDecimal>> splitIntegral(
            BigDecimal lower,
            BigDecimal upper,
            int requestedCount) {

        final BigInteger low;
        final BigInteger high;

        try {
            low = lower.toBigIntegerExact();
            high = upper.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "integral partition bounds must not contain decimal values",
                    e);
        }

        BigInteger cardinality = high.subtract(low).add(BigInteger.ONE);
        int actualCount = resolveIntegralChunkCount(cardinality, requestedCount);

        BigInteger[] division =
                cardinality.divideAndRemainder(BigInteger.valueOf(actualCount));

        BigInteger baseSize = division[0];
        int remainder = division[1].intValue();

        List<Chunk<BigDecimal>> result =
                new ArrayList<Chunk<BigDecimal>>(actualCount);

        BigInteger start = low;

        for (int index = 0; index < actualCount; index++) {
            BigInteger chunkSize =
                    baseSize.add(index < remainder
                            ? BigInteger.ONE
                            : BigInteger.ZERO);

            boolean last = index == actualCount - 1;
            BigInteger end = last ? high : start.add(chunkSize);

            result.add(
                    new Chunk<BigDecimal>(
                            new BigDecimal(start),
                            new BigDecimal(end),
                            last));

            start = end;
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * 小数范围采用连续等宽切分。
     */
    private List<Chunk<BigDecimal>> splitDecimal(
            BigDecimal lower,
            BigDecimal upper,
            int count) {

        BigDecimal width =
                upper.subtract(lower)
                        .divide(BigDecimal.valueOf(count), MathContext.DECIMAL128);

        if (width.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.singletonList(
                    new Chunk<BigDecimal>(lower, upper, true));
        }

        List<Chunk<BigDecimal>> result =
                new ArrayList<Chunk<BigDecimal>>(count);

        BigDecimal start = lower;

        for (int index = 0; index < count; index++) {
            boolean last = index == count - 1;
            BigDecimal end = last ? upper : start.add(width);

            if (end.compareTo(upper) >= 0) {
                end = upper;
                last = true;
            }

            result.add(new Chunk<BigDecimal>(start, end, last));

            if (last) {
                break;
            }

            start = end;
        }

        return Collections.unmodifiableList(result);
    }
}