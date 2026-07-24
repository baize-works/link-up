package com.link.up.connector.jdbc.core.split;

import java.util.Locale;

/**
 * 字符串分片策略。
 */
public enum StringSplitStrategy {

    /**
     * 优先尝试安全的范围分片，其次 HASH，最后单分片。
     */
    AUTO("auto"),

    /**
     * 使用字符串范围分片。
     */
    RANGE("range"),

    /**
     * 使用数据库方言提供的 HASH 表达式。
     */
    HASH("hash"),

    /**
     * 禁止字符串并行分片。
     */
    NONE("none");

    private final String value;

    StringSplitStrategy(String value) {
        this.value = value;
    }

    public static StringSplitStrategy parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return AUTO;
        }

        String normalized =
                value.trim()
                        .replace('-', '_')
                        .toUpperCase(Locale.ROOT);

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "unsupported string split strategy: " + value,
                    e);
        }
    }

    public String getValue() {
        return value;
    }

    public boolean matches(String candidate) {
        return candidate != null
                && value.equalsIgnoreCase(candidate.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}