package com.link.up.framework.util;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.configuration.util.ConfigValidator;
import com.link.up.api.factory.Factory;
import com.link.up.api.source.Source;
import com.link.up.api.source.SourceFactoryContext;
import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.factory.TableSourceFactory;
import com.link.up.framework.factory.FactoryException;
import com.link.up.framework.factory.PreparedSource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory SPI 工具。
 * <p>
 * 只负责 Factory 的发现、校验和实例创建，
 * 不负责任务调度或数据读取。
 */
public final class FactoryUtil {

    private FactoryUtil() {
    }

    /**
     * 创建并准备 Source。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <SplitT extends SourceSplit>
    PreparedSource<SplitT> createAndPrepareSource(
            String factoryIdentifier,
            ReadonlyConfig options,
            ClassLoader classLoader) {

        Objects.requireNonNull(
                factoryIdentifier,
                "factoryIdentifier must not be null");

        Objects.requireNonNull(
                options,
                "options must not be null");

        ClassLoader effectiveClassLoader =
                classLoader == null
                        ? Thread.currentThread()
                        .getContextClassLoader()
                        : classLoader;

        String normalizedIdentifier =
                factoryIdentifier.trim();

        if (normalizedIdentifier.isEmpty()) {
            throw new FactoryException(
                    "factoryIdentifier must not be blank");
        }

        try {
            TableSourceFactory<SplitT> factory =
                    (TableSourceFactory<SplitT>)
                            discoverFactory(
                                    effectiveClassLoader,
                                    TableSourceFactory.class,
                                    normalizedIdentifier);

            SourceFactoryContext context =
                    new SourceFactoryContext(
                            options,
                            effectiveClassLoader);

            ConfigValidator.of(context.getOptions())
                    .validate(factory.optionRule());

            Source<SplitT> source =
                    factory.createSource(context);

            if (source == null) {
                throw new FactoryException(
                        "Factory '" + normalizedIdentifier
                                + "' returned a null source");
            }

            List<CatalogTable> catalogTables =
                    factory.discoverTableSchemas(context);

            Map<TablePath, CatalogTable> tableMap =
                    buildTableMap(
                            normalizedIdentifier,
                            catalogTables);

            return new PreparedSource<>(
                    normalizedIdentifier,
                    source,
                    tableMap);

        } catch (FactoryException e) {
            throw e;
        } catch (Exception e) {
            throw new FactoryException(
                    "Unable to create source for identifier '"
                            + normalizedIdentifier + "'",
                    e);
        }
    }

    /**
     * 查找指定类型和标识的 Factory。
     */
    public static <T extends Factory> T discoverFactory(
            ClassLoader classLoader,
            Class<T> factoryClass,
            String factoryIdentifier) {

        Objects.requireNonNull(
                classLoader,
                "classLoader must not be null");

        Objects.requireNonNull(
                factoryClass,
                "factoryClass must not be null");

        Objects.requireNonNull(
                factoryIdentifier,
                "factoryIdentifier must not be null");

        List<T> factories =
                discoverFactories(
                        classLoader,
                        factoryClass);

        if (factories.isEmpty()) {
            throw new FactoryException(
                    "Could not find any factory implementing '"
                            + factoryClass.getName() + "'");
        }

        List<T> matchedFactories =
                factories.stream()
                        .filter(factory ->
                                factory.factoryIdentifier()
                                        .equalsIgnoreCase(
                                                factoryIdentifier))
                        .collect(Collectors.toList());

        if (matchedFactories.isEmpty()) {
            String availableIdentifiers =
                    factories.stream()
                            .map(Factory::factoryIdentifier)
                            .distinct()
                            .sorted()
                            .collect(Collectors.joining(", "));

            throw new FactoryException(
                    "Could not find factory for identifier '"
                            + factoryIdentifier
                            + "'. Available identifiers: "
                            + availableIdentifiers);
        }

        if (matchedFactories.size() > 1) {
            String factoryClasses =
                    matchedFactories.stream()
                            .map(factory ->
                                    factory.getClass().getName())
                            .sorted()
                            .collect(Collectors.joining(", "));

            throw new FactoryException(
                    "Multiple factories found for identifier '"
                            + factoryIdentifier
                            + "': "
                            + factoryClasses);
        }

        return matchedFactories.get(0);
    }

    /**
     * 加载指定 SPI 类型的所有实现。
     */
    public static <T extends Factory> List<T> discoverFactories(
            ClassLoader classLoader,
            Class<T> factoryClass) {

        try {
            ServiceLoader<T> loader =
                    ServiceLoader.load(
                            factoryClass,
                            classLoader);

            List<T> result = new ArrayList<>();

            for (T factory : loader) {
                result.add(factory);
            }

            return result;

        } catch (ServiceConfigurationError e) {
            throw new FactoryException(
                    "Could not load service providers for '"
                            + factoryClass.getName() + "'",
                    e);
        }
    }

    private static Map<TablePath, CatalogTable> buildTableMap(
            String factoryIdentifier,
            List<CatalogTable> catalogTables) {

        if (catalogTables == null) {
            throw new FactoryException(
                    "Factory '" + factoryIdentifier
                            + "' returned null catalog tables");
        }

        if (catalogTables.isEmpty()) {
            throw new FactoryException(
                    "No source tables were discovered by factory '"
                            + factoryIdentifier + "'");
        }

        Map<TablePath, CatalogTable> result =
                new LinkedHashMap<>();

        for (CatalogTable catalogTable : catalogTables) {
            if (catalogTable == null) {
                throw new FactoryException(
                        "Factory '" + factoryIdentifier
                                + "' returned a null CatalogTable");
            }

            TablePath tablePath =
                    catalogTable.getTablePath();

            if (tablePath == null) {
                throw new FactoryException(
                        "CatalogTable returned by factory '"
                                + factoryIdentifier
                                + "' has no TablePath");
            }

            CatalogTable previous =
                    result.put(tablePath, catalogTable);

            if (previous != null) {
                throw new FactoryException(
                        "Duplicated source table path: "
                                + tablePath);
            }
        }

        return result;
    }
}
