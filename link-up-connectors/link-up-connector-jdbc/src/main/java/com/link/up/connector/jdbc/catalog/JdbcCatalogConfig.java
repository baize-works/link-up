package com.link.up.connector.jdbc.catalog;

import java.io.Serializable;
import java.util.*;

/**
 * JDBC Catalog 连接配置。
 */
public final class JdbcCatalogConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String url;
    private final String username;
    private final String password;
    private final String driverClass;
    private final Map<String, String> properties;

    /**
     * 是否按照字段范围缩小整数类型。
     * <p>
     * 例如：
     * <p>
     * tinyint  -> Byte
     * smallint -> Short
     * <p>
     * 关闭后，tinyint/smallint 通常统一映射为 Integer，
     * 可以减少不同数据库之间的类型兼容问题。
     */
    private final boolean intTypeNarrowing;

    public JdbcCatalogConfig(
            String url,
            String username,
            String password,
            String driverClass,
            Map<String, String> properties,
            boolean intTypeNarrowing) {

        this.url = requireText(url, "url");
        this.username = normalize(username);
        this.password = password;
        this.driverClass = normalize(driverClass);

        Map<String, String> safeProperties =
                properties == null
                        ? Collections.emptyMap()
                        : new LinkedHashMap<>(properties);

        this.properties =
                Collections.unmodifiableMap(safeProperties);

        this.intTypeNarrowing = intTypeNarrowing;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean isIntTypeNarrowing() {
        return intTypeNarrowing;
    }

    /**
     * 构造 JDBC 连接属性。
     */
    public Properties toConnectionProperties() {
        Properties result = new Properties();

        for (Map.Entry<String, String> entry :
                properties.entrySet()) {

            result.setProperty(
                    entry.getKey(),
                    entry.getValue());
        }

        if (username != null) {
            result.setProperty("user", username);
        }

        if (password != null) {
            result.setProperty("password", password);
        }

        /*
         * 禁止驱动把 tinyint(1) 自动转换为 Boolean，
         * 交由 Flux 类型映射层统一决定。
         */
        result.putIfAbsent(
                "tinyInt1isBit",
                "false");

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof JdbcCatalogConfig)) {
            return false;
        }

        JdbcCatalogConfig that =
                (JdbcCatalogConfig) obj;

        return intTypeNarrowing == that.intTypeNarrowing
                && Objects.equals(url, that.url)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(
                driverClass,
                that.driverClass)
                && Objects.equals(
                properties,
                that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                url,
                username,
                password,
                driverClass,
                properties,
                intTypeNarrowing);
    }
}