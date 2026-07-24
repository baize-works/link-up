package com.link.up.framework.job;

import com.link.up.api.configuration.ReadonlyConfig;

import java.util.Objects;

/**
 * Source 配置定义。
 *
 * <p>只保存配置，不创建数据库连接等运行时资源。
 */
public final class SourceDefinition {

    private final String type;

    private final ReadonlyConfig options;

    public SourceDefinition(
            String type,
            ReadonlyConfig options) {

        this.type = requireType(type);

        this.options =
                Objects.requireNonNull(
                        options,
                        "options must not be null");
    }

    private static String requireType(String type) {
        Objects.requireNonNull(
                type,
                "type must not be null");

        String normalized = type.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "type must not be blank");
        }

        return normalized;
    }

    public String getType() {
        return type;
    }

    public ReadonlyConfig getOptions() {
        return options;
    }
}