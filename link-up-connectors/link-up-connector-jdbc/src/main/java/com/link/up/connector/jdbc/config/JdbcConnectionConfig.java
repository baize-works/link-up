package com.link.up.connector.jdbc.config;

import com.link.up.api.configuration.ReadonlyConfig;
import lombok.Getter;

import java.io.Serializable;
import java.util.*;

/**
 * JDBC 连接配置。
 * <p>
 * 该类只负责数据库连接，不包含：
 * <p>
 * 1. Source 分片配置；
 * 2. Sink 批次配置；
 * 3. Exactly Once；
 * 4. XA 事务；
 * 5. 写入重试策略。
 */
@Getter
public final class JdbcConnectionConfig
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String url;
    private final String driverName;
    private final String username;
    private final String password;

    private final String dialect;
    private final String compatibleMode;
    private final String schema;

    private final int connectionCheckTimeoutSeconds;
    private final int connectTimeoutMs;
    private final int socketTimeoutMs;

    private final Map<String, String> properties;

    private JdbcConnectionConfig(
            String url,
            String driverName,
            String username,
            String password,
            String dialect,
            String compatibleMode,
            String schema,
            int connectionCheckTimeoutSeconds,
            int connectTimeoutMs,
            int socketTimeoutMs,
            Map<String, String> properties) {

        this.url = requireText(url, "url");
        this.driverName = requireText(
                driverName,
                "driverName");

        this.username = normalize(username);
        this.password = password;

        this.dialect = normalize(dialect);
        this.compatibleMode =
                normalize(compatibleMode);
        this.schema = normalize(schema);

        if (connectionCheckTimeoutSeconds <= 0) {
            throw new IllegalArgumentException(
                    "connectionCheckTimeoutSeconds must be greater than 0");
        }

        if (connectTimeoutMs < 0) {
            throw new IllegalArgumentException(
                    "connectTimeoutMs must not be negative");
        }

        if (socketTimeoutMs < 0) {
            throw new IllegalArgumentException(
                    "socketTimeoutMs must not be negative");
        }

        this.connectionCheckTimeoutSeconds =
                connectionCheckTimeoutSeconds;

        this.connectTimeoutMs = connectTimeoutMs;
        this.socketTimeoutMs = socketTimeoutMs;

        Map<String, String> safeProperties =
                properties == null
                        ? Collections.emptyMap()
                        : new LinkedHashMap<>(properties);

        this.properties =
                Collections.unmodifiableMap(
                        safeProperties);
    }

    public static JdbcConnectionConfig of(
            ReadonlyConfig config) {

        Objects.requireNonNull(
                config,
                "config must not be null");

        return new JdbcConnectionConfig(
                config.get(JdbcCommonOptions.URL),
                config.get(JdbcCommonOptions.DRIVER),
                config.getOptional(
                        JdbcCommonOptions.USERNAME)
                        .orElse(null),
                config.getOptional(
                        JdbcCommonOptions.PASSWORD)
                        .orElse(null),
                config.getOptional(
                        JdbcCommonOptions.DIALECT)
                        .orElse(null),
                config.getOptional(
                        JdbcCommonOptions.COMPATIBLE_MODE)
                        .orElse(null),
                config.getOptional(
                        JdbcCommonOptions.SCHEMA)
                        .orElse(null),
                config.get(
                        JdbcCommonOptions
                                .CONNECTION_CHECK_TIMEOUT_SEC),
                config.get(
                        JdbcCommonOptions.CONNECT_TIMEOUT_MS),
                config.get(
                        JdbcCommonOptions.SOCKET_TIMEOUT_MS),
                config.getOptional(
                        JdbcCommonOptions.PROPERTIES)
                        .orElse(Collections.emptyMap()));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private static String requireText(
            String value,
            String fieldName) {

        String normalized = normalize(value);

        if (normalized == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be empty");
        }

        return normalized;
    }

    /**
     * 构造 DriverManager 使用的连接属性。
     * <p>
     * connectTimeout、socketTimeout 的具体参数名由不同数据库
     * Driver 决定，因此不在公共层自动写入。
     */
    public Properties toProperties() {
        Properties result = new Properties();

        for (Map.Entry<String, String> entry :
                properties.entrySet()) {

            result.setProperty(
                    entry.getKey(),
                    entry.getValue());
        }

        if (username != null) {
            result.setProperty(
                    "user",
                    username);
        }

        if (password != null) {
            result.setProperty(
                    "password",
                    password);
        }

        return result;
    }
}