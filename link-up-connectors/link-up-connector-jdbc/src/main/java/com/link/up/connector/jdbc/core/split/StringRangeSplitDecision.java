package com.link.up.connector.jdbc.core.split;

import java.util.Objects;

/**
 * 字符串范围分片安全性判断结果。
 */
public final class StringRangeSplitDecision {

    private final boolean safe;
    private final String reason;

    private StringRangeSplitDecision(
            boolean safe,
            String reason) {

        this.safe = safe;
        this.reason = Objects.requireNonNull(
                reason,
                "reason must not be null");
    }

    public static StringRangeSplitDecision safe(String reason) {
        return new StringRangeSplitDecision(true, reason);
    }

    public static StringRangeSplitDecision unsafe(String reason) {
        return new StringRangeSplitDecision(false, reason);
    }

    public boolean isSafe() {
        return safe;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return safe
                ? "SAFE: " + reason
                : "UNSAFE: " + reason;
    }
}