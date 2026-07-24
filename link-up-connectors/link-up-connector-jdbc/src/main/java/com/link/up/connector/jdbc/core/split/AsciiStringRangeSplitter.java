package com.link.up.connector.jdbc.core.split;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 固定长度、可打印 ASCII 字符串范围分片器。
 *
 * <p>该映射只在数据库使用二进制或 ASCII 兼容排序规则时保持顺序一致。
 * 是否能够安全使用 RANGE 模式，由 JdbcDialect 决定。
 */
public final class AsciiStringRangeSplitter
        implements ChunkSplitter<String> {

    private static final char FIRST_PRINTABLE_ASCII = ' ';
    private static final char LAST_PRINTABLE_ASCII = '~';

    private static final BigInteger RADIX = BigInteger.valueOf(
            LAST_PRINTABLE_ASCII - FIRST_PRINTABLE_ASCII + 1L);

    /**
     * 只校验字符串本身是否可以进行 ASCII 范围映射。
     *
     * <p>数据库排序规则是否安全，仍然需要由 JdbcDialect 判断。
     */
    public static StringRangeSplitDecision assess(
            String lower,
            String upper) {

        if (lower == null || upper == null) {
            return StringRangeSplitDecision.unsafe(
                    "string partition bounds must not be null");
        }

        if (lower.length() != upper.length()) {
            return StringRangeSplitDecision.unsafe(
                    "string range split requires fixed-length bounds");
        }

        if (lower.compareTo(upper) > 0) {
            return StringRangeSplitDecision.unsafe(
                    "string lower bound must not be greater than upper bound");
        }

        for (int index = 0; index < lower.length(); index++) {
            if (!isPrintableAscii(lower.charAt(index))) {
                return StringRangeSplitDecision.unsafe(
                        "string lower bound contains non-printable ASCII characters");
            }

            if (!isPrintableAscii(upper.charAt(index))) {
                return StringRangeSplitDecision.unsafe(
                        "string upper bound contains non-printable ASCII characters");
            }
        }

        return StringRangeSplitDecision.safe(
                "fixed-length printable ASCII bounds");
    }

    private static boolean isPrintableAscii(char value) {
        return value >= FIRST_PRINTABLE_ASCII
                && value <= LAST_PRINTABLE_ASCII;
    }

    private static BigInteger encode(String value) {
        BigInteger result = BigInteger.ZERO;

        for (int index = 0; index < value.length(); index++) {
            int digit = value.charAt(index) - FIRST_PRINTABLE_ASCII;

            result = result.multiply(RADIX)
                    .add(BigInteger.valueOf(digit));
        }

        return result;
    }

    private static String decode(
            BigInteger value,
            int length) {

        char[] result = new char[length];
        BigInteger remaining = value;

        for (int index = length - 1; index >= 0; index--) {
            BigInteger[] division =
                    remaining.divideAndRemainder(RADIX);

            result[index] = (char) (
                    division[1].intValue() + FIRST_PRINTABLE_ASCII);

            remaining = division[0];
        }

        if (remaining.signum() != 0) {
            throw new IllegalArgumentException(
                    "generated string boundary exceeds fixed length");
        }

        return new String(result);
    }

    private static void validateGeneratedChunks(
            List<Chunk<String>> chunks) {

        for (int index = 0; index < chunks.size(); index++) {
            Chunk<String> current = chunks.get(index);

            if (current.getStart().compareTo(current.getEnd()) > 0) {
                throw new IllegalStateException(
                        "generated string chunk is not ordered: " + current);
            }

            if (index > 0) {
                Chunk<String> previous = chunks.get(index - 1);

                if (!previous.getEnd().equals(current.getStart())) {
                    throw new IllegalStateException(
                            "generated string chunks are not continuous");
                }
            }
        }
    }

    @Override
    public List<Chunk<String>> split(
            String lower,
            String upper,
            int count) {

        StringRangeSplitDecision decision = assess(lower, upper);

        if (!decision.isSafe()) {
            throw new IllegalArgumentException(decision.getReason());
        }

        if (count <= 0) {
            throw new IllegalArgumentException(
                    "chunkCount must be greater than 0");
        }

        if (lower.equals(upper)) {
            return Collections.singletonList(
                    new Chunk<String>(lower, upper, true));
        }

        BigInteger low = encode(lower);
        BigInteger high = encode(upper);

        /*
         * 字符串映射后的值是离散空间，因此数量为 high - low + 1。
         */
        BigInteger cardinality =
                high.subtract(low).add(BigInteger.ONE);

        int actualCount =
                cardinality.min(BigInteger.valueOf(count)).intValue();

        BigInteger[] division =
                cardinality.divideAndRemainder(
                        BigInteger.valueOf(actualCount));

        BigInteger baseSize = division[0];
        int remainder = division[1].intValue();

        List<Chunk<String>> result =
                new ArrayList<Chunk<String>>(actualCount);

        BigInteger start = low;

        for (int index = 0; index < actualCount; index++) {
            BigInteger chunkSize =
                    baseSize.add(index < remainder
                            ? BigInteger.ONE
                            : BigInteger.ZERO);

            boolean last = index == actualCount - 1;
            BigInteger end = last ? high : start.add(chunkSize);

            String startValue =
                    index == 0
                            ? lower
                            : decode(start, lower.length());

            String endValue =
                    last
                            ? upper
                            : decode(end, lower.length());

            result.add(
                    new Chunk<String>(
                            startValue,
                            endValue,
                            last));

            start = end;
        }

        validateGeneratedChunks(result);
        return Collections.unmodifiableList(result);
    }
}