package com.link.up.connector.jdbc.core.dialect;

import com.link.up.connector.jdbc.config.JdbcConnectionConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC 方言加载器。
 */
public final class JdbcDialectLoader {

    private JdbcDialectLoader() {
    }

    /**
     * 优先根据 dialect 配置加载；未配置时根据 JDBC URL 自动识别。
     */
    public static JdbcDialect load(JdbcConnectionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("connectionConfig must not be null");
        }

        ClassLoader classLoader =
                Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = JdbcDialectLoader.class.getClassLoader();
        }

        List<JdbcDialectFactory> factories =
                discoverFactories(classLoader);

        if (factories.isEmpty()) {
            throw new IllegalStateException(
                    "未发现 JdbcDialectFactory，请检查 SPI 配置："
                            + "META-INF/services/"
                            + JdbcDialectFactory.class.getName());
        }

        String configuredDialect = normalize(config.getDialect());

        List<JdbcDialectFactory> matches =
                factories.stream()
                        .filter(
                                factory ->
                                        configuredDialect != null
                                                ? factory.identifier()
                                                .equalsIgnoreCase(configuredDialect)
                                                : factory.acceptsUrl(config.getUrl()))
                        .collect(Collectors.toList());

        if (matches.isEmpty()) {
            String mode =
                    configuredDialect == null
                            ? "url=" + config.getUrl()
                            : "dialect=" + configuredDialect;

            throw new IllegalStateException(
                    "没有找到匹配的 JDBC 方言，"
                            + mode
                            + "，已加载工厂="
                            + factoryNames(factories));
        }

        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "存在多个匹配的 JDBC 方言，"
                            + "url="
                            + config.getUrl()
                            + "，dialect="
                            + configuredDialect
                            + "，匹配工厂="
                            + factoryNames(matches));
        }

        return matches.get(0).create(config);
    }

    private static List<JdbcDialectFactory> discoverFactories(
            ClassLoader classLoader) {

        try {
            List<JdbcDialectFactory> result = new ArrayList<>();

            ServiceLoader.load(
                    JdbcDialectFactory.class,
                    classLoader)
                    .forEach(result::add);

            result.sort(
                    Comparator.comparing(
                            factory ->
                                    factory.getClass().getName()));

            return result;
        } catch (ServiceConfigurationError error) {
            throw new IllegalStateException(
                    "加载 JDBC 方言 SPI 失败",
                    error);
        }
    }

    private static String factoryNames(
            List<JdbcDialectFactory> factories) {

        return factories.stream()
                .map(factory -> factory.getClass().getName())
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
