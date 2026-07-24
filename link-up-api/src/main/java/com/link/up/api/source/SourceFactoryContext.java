package com.link.up.api.source;

import com.link.up.api.configuration.ReadonlyConfig;

import java.util.Objects;

/**
 * Source 工厂上下文。
 * <p>
 * 封装创建 Source 时所需的配置和类加载器。
 */
public final class SourceFactoryContext {

    /**
     * Source 原始配置。
     */
    private final ReadonlyConfig options;

    /**
     * 当前 Connector 使用的类加载器。
     * <p>
     * 可用于加载 JDBC Driver、SPI 实现和 Connector 内部资源。
     */
    private final ClassLoader classLoader;

    /**
     * 使用当前线程上下文类加载器创建上下文。
     */
    public SourceFactoryContext(
            ReadonlyConfig options) {

        this(
                options,
                Thread.currentThread()
                        .getContextClassLoader());
    }

    /**
     * 使用指定类加载器创建上下文。
     */
    public SourceFactoryContext(
            ReadonlyConfig options,
            ClassLoader classLoader) {

        this.options =
                Objects.requireNonNull(
                        options,
                        "options must not be null");

        this.classLoader =
                Objects.requireNonNull(
                        classLoader,
                        "classLoader must not be null");
    }

    public ReadonlyConfig getOptions() {
        return options;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}