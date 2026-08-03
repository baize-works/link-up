package com.link.up.framework.connector;

import com.link.up.api.source.Source;
import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.framework.mapping.ColumnMappingPlan;

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

    /** Physical source schemas used by readers and split planning. */
    private final Map<TablePath, CatalogTable> tables;

    /** Schemas emitted to channels and supplied to sink preparation. */
    private final Map<TablePath, CatalogTable> outputTables;

    private final Map<TablePath, ColumnMappingPlan> columnMappingPlans;

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
        this(
                factoryIdentifier,
                source,
                tables,
                tables,
                Collections.<TablePath, ColumnMappingPlan>emptyMap(),
                classLoader);
    }

    public PreparedSource(
            String factoryIdentifier,
            Source<SplitT> source,
            Map<TablePath, CatalogTable> tables,
            Map<TablePath, CatalogTable> outputTables,
            Map<TablePath, ColumnMappingPlan> columnMappingPlans,
            ClassLoader classLoader) {

        this.factoryIdentifier =
                Objects.requireNonNull(
                        factoryIdentifier,
                        "factoryIdentifier must not be null");

        this.source =
                Objects.requireNonNull(
                        source,
                        "source must not be null");

        this.tables = immutable(tables, "tables");
        this.outputTables = immutable(outputTables, "outputTables");
        this.columnMappingPlans = immutable(columnMappingPlans, "columnMappingPlans");

        if (!this.tables.keySet().equals(this.outputTables.keySet())) {
            throw new IllegalArgumentException(
                    "source and output table paths must match");
        }

        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
    }

    private static <T> Map<TablePath, T> immutable(
            Map<TablePath, T> values,
            String name) {
        return Collections.unmodifiableMap(
                new LinkedHashMap<TablePath, T>(
                        Objects.requireNonNull(values, name + " must not be null")));
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

    public Map<TablePath, CatalogTable> getOutputTables() {
        return outputTables;
    }

    public ColumnMappingPlan getColumnMappingPlan(TablePath tablePath) {
        return columnMappingPlans.get(tablePath);
    }
}
