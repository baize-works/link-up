package com.link.up.framework.connector;

import com.link.up.api.source.Source;
import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 已完成配置校验和表结构发现的 Source。
 */
public final class PreparedSource<
        SplitT extends SourceSplit> {

    private final String factoryIdentifier;

    private final Source<SplitT> source;

    private final Map<TablePath, CatalogTable> tables;

    private final ClassLoader classLoader;

    public PreparedSource(
            String factoryIdentifier,
            Source<SplitT> source,
            Map<TablePath, CatalogTable> tables) {
        this(factoryIdentifier, source, tables, source.getClass().getClassLoader());
    }

    public PreparedSource(
            String factoryIdentifier,
            Source<SplitT> source,
            Map<TablePath, CatalogTable> tables,
            ClassLoader classLoader) {

        this.factoryIdentifier =
                Objects.requireNonNull(
                        factoryIdentifier,
                        "factoryIdentifier must not be null");

        this.source =
                Objects.requireNonNull(
                        source,
                        "source must not be null");

        this.tables =
                Collections.unmodifiableMap(
                        new LinkedHashMap<
                                TablePath,
                                CatalogTable>(
                                Objects.requireNonNull(
                                        tables,
                                        "tables must not be null")));

        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
    }

    public String getFactoryIdentifier() {
        return factoryIdentifier;
    }

    public Source<SplitT> getSource() {
        return source;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Map<TablePath, CatalogTable> getTables() {
        return tables;
    }
}