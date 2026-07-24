package com.link.up.connector.jdbc.core.split;

import java.util.Objects;

/**
 * 一个不可变的分片边界。
 *
 * <p>默认采用左闭右开区间：
 *
 * <pre>
 * [start, end)
 * </pre>
 * <p>
 * 最后一个分片可以通过 {@code endInclusive=true} 包含右边界。
 */
public final class Chunk<T> {

    private final T start;
    private final T end;
    private final boolean endInclusive;

    public Chunk(T start, T end, boolean endInclusive) {
        this.start = Objects.requireNonNull(start, "start must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
        this.endInclusive = endInclusive;
    }

    public T getStart() {
        return start;
    }

    public T getEnd() {
        return end;
    }

    public boolean isEndInclusive() {
        return endInclusive;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Chunk)) {
            return false;
        }

        Chunk<?> that = (Chunk<?>) obj;
        return endInclusive == that.endInclusive
                && start.equals(that.start)
                && end.equals(that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, endInclusive);
    }

    @Override
    public String toString() {
        return (endInclusive ? "[" : "[")
                + start
                + ", "
                + end
                + (endInclusive ? "]" : ")");
    }
}