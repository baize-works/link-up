package com.link.up.framework.connector;

import com.link.up.api.configuration.util.ConfigValidator;
import com.link.up.api.factory.SinkFactory;
import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.sink.SinkPrepareContext;
import com.link.up.api.source.Source;
import com.link.up.api.source.SourceFactoryContext;
import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.factory.TableSourceFactory;
import com.link.up.framework.classloading.ClassLoaderScope;
import com.link.up.framework.job.ColumnMapping;
import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.SinkDefinition;
import com.link.up.framework.job.SourceDefinition;
import com.link.up.framework.mapping.ColumnMappingPlanner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Connector 准备器。
 *
 * <p>负责：
 *
 * <ul>
 *     <li>查找 Factory</li>
 *     <li>校验配置</li>
 *     <li>创建 Source</li>
 *     <li>发现 Source Schema</li>
 *     <li>准备 Sink Factory</li>
 * </ul>
 */
public final class ConnectorPreparer {

    private final FactoryRegistry registry;

    private final ClassLoader classLoader;

    public ConnectorPreparer(
            FactoryRegistry registry,
            ClassLoader classLoader) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry must not be null");

        this.classLoader =
                Objects.requireNonNull(
                        classLoader,
                        "classLoader must not be null");
    }

    public PreparedJob prepare(
            JobDefinition definition) throws Exception {

        Objects.requireNonNull(
                definition,
                "definition must not be null");

        PreparedSource<?> source =
                prepareSource(
                        definition.getSource(),
                        definition.getExecutionConfig()
                                .getSourceParallelism());

        source = applyColumnMapping(source, definition.getColumnMapping());

        Map<String, List<PreparedSink>> sinks =
                prepareSinks(
                        definition.getSink(),
                        definition.getExecutionConfig().getSinkParallelism(),
                        source.getOutputTables());

        return new PreparedJob(
                definition.getName(),
                source,
                sinks,
                definition.getExecutionConfig());
    }

    private <SplitT extends SourceSplit> PreparedSource<SplitT> applyColumnMapping(
            PreparedSource<SplitT> source,
            ColumnMapping mapping) {
        ColumnMappingPlanner.Result result =
                new ColumnMappingPlanner().plan(source.getTables(), mapping);

        return new PreparedSource<SplitT>(
                source.getFactoryIdentifier(),
                source.getSource(),
                source.getTables(),
                result.getOutputTables(),
                result.getPlans(),
                source.getClassLoader());
    }

    private PreparedSource<?> prepareSource(
            SourceDefinition definition,
            int sourceParallelism) throws Exception {

        TableSourceFactory<?> factory =
                registry.getSourceFactory(
                        definition.getType());

        ConfigValidator.of(
                definition.getOptions())
                .validate(
                        factory.optionRule());

        SourceFactoryContext context =
                new SourceFactoryContext(
                        definition.getOptions(),
                        classLoader);

        return createPreparedSource(
                definition.getType(),
                factory,
                context,
                sourceParallelism);
    }

    private <SplitT extends SourceSplit>
    PreparedSource<SplitT> createPreparedSource(
            String identifier,
            TableSourceFactory<SplitT> factory,
            SourceFactoryContext context,
            int sourceParallelism) throws Exception {

        Source<SplitT> source =
                createSourceInScope(factory, context);

        if (source == null) {
            throw new ConnectorException(
                    "Source factory '"
                            + identifier
                            + "' returned a null source");
        }

        source.validateParallelism(sourceParallelism);

        List<CatalogTable> catalogTables;
        try (ClassLoaderScope ignored = ClassLoaderScope.open(registry.getClassLoader(factory))) {
            catalogTables = factory.discoverTableSchemas(context);
        }

        Map<TablePath, CatalogTable> tableMap =
                buildTableMap(
                        identifier,
                        catalogTables);

        return new PreparedSource<SplitT>(
                identifier,
                source,
                tableMap,
                registry.getClassLoader(factory));
    }

    private Map<String, List<PreparedSink>> prepareSinks(
            SinkDefinition definition,
            int parallelism,
            Map<TablePath, CatalogTable> sourceTables) throws Exception {

        SinkFactory factory =
                registry.getSinkFactory(
                        definition.getType());

        ConfigValidator.of(
                definition.getOptions())
                .validate(
                        factory.optionRule());

        Map<String, List<PreparedSink>> result = new LinkedHashMap<String, List<PreparedSink>>();
        for (Map.Entry<TablePath, CatalogTable> table : sourceTables.entrySet()) {
            // Sink preparation is deliberately per dataset: DDL/cleanup and metadata cannot leak across tables.
            Map<TablePath, CatalogTable> oneTable = new LinkedHashMap<TablePath, CatalogTable>();
            oneTable.put(table.getKey(), table.getValue());
            PreparedSinkMetadata metadata;
            try (ClassLoaderScope ignored = ClassLoaderScope.open(registry.getClassLoader(factory))) {
                metadata = factory.createPreparer(definition.getOptions())
                        .prepare(new SinkPrepareContext(definition.getOptions(), oneTable));
            }
            if (metadata == null)
                throw new ConnectorException("Sink factory '" + definition.getType() + "' returned null preparation metadata");
            List<PreparedSink> sinks = new java.util.ArrayList<PreparedSink>(parallelism);
            for (int i = 0; i < parallelism; i++)
                sinks.add(new PreparedSink(definition.getType(), factory, definition.getOptions(), metadata, registry.getClassLoader(factory)));
            result.put(table.getKey().toString(), sinks);
        }
        return result;
    }

    private <SplitT extends SourceSplit> Source<SplitT> createSourceInScope(TableSourceFactory<SplitT> factory, SourceFactoryContext context) throws Exception {
        try (ClassLoaderScope ignored = ClassLoaderScope.open(registry.getClassLoader(factory))) {
            return factory.createSource(context);
        }
    }

    private Map<TablePath, CatalogTable> buildTableMap(
            String factoryIdentifier,
            List<CatalogTable> catalogTables) {

        if (catalogTables == null) {
            throw new ConnectorException(
                    "Source factory '"
                            + factoryIdentifier
                            + "' returned null catalog tables");
        }

        if (catalogTables.isEmpty()) {
            throw new ConnectorException(
                    "No source tables were discovered by factory '"
                            + factoryIdentifier
                            + "'");
        }

        Map<TablePath, CatalogTable> result =
                new LinkedHashMap<
                        TablePath,
                        CatalogTable>();

        for (CatalogTable catalogTable : catalogTables) {
            if (catalogTable == null) {
                throw new ConnectorException(
                        "Source factory '"
                                + factoryIdentifier
                                + "' returned a null CatalogTable");
            }

            TablePath tablePath =
                    catalogTable.getTablePath();

            if (tablePath == null) {
                throw new ConnectorException(
                        "CatalogTable returned by source factory '"
                                + factoryIdentifier
                                + "' has no TablePath");
            }

            CatalogTable previous =
                    result.put(
                            tablePath,
                            catalogTable);

            if (previous != null) {
                throw new ConnectorException(
                        "Duplicated source table path: "
                                + tablePath);
            }
        }

        return result;
    }
}
