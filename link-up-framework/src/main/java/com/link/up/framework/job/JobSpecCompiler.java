package com.link.up.framework.job;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.job.JobSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Compiles the public structured JobSpec protocol into Link-Up runtime definitions. */
public final class JobSpecCompiler {

    public JobDefinition compile(JobSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("jobSpec must not be null");
        }
        requireProtocol(spec.getApiVersion(), spec.getKind());
        JobSpec.Connector sourceSpec = requireConnector(spec.getSource(), "source");
        JobSpec.Connector sinkSpec = requireConnector(spec.getSink(), "sink");
        JobSpec.Runtime runtime = spec.getRuntime() == null ? new JobSpec.Runtime() : spec.getRuntime();

        SourceDefinition source = new SourceDefinition(
                requireText(sourceSpec.getConnectorId(), "source.connectorId"),
                ReadonlyConfig.fromMap(copy(sourceSpec.getOptions())));
        SinkDefinition sink = new SinkDefinition(
                requireText(sinkSpec.getConnectorId(), "sink.connectorId"),
                ReadonlyConfig.fromMap(copy(sinkSpec.getOptions())));

        ExecutionConfig execution = new ExecutionConfig(
                positive(runtime.getBatchSize(), ExecutionConfig.DEFAULT_BATCH_SIZE, "runtime.batchSize"),
                positive(runtime.getSourceParallelism(), ExecutionConfig.DEFAULT_SOURCE_PARALLELISM,
                        "runtime.sourceParallelism"),
                positive(runtime.getSinkParallelism(), ExecutionConfig.DEFAULT_SINK_PARALLELISM,
                        "runtime.sinkParallelism"),
                positive(runtime.getPipelineParallelism(), ExecutionConfig.DEFAULT_PIPELINE_PARALLELISM,
                        "runtime.pipelineParallelism"),
                positive(runtime.getMaxBufferedBatches(), ExecutionConfig.DEFAULT_CHANNEL_CAPACITY,
                        "runtime.maxBufferedBatches"),
                optional(runtime.getMaxBufferedRecords()),
                optional(runtime.getMaxBufferedBytes()),
                optional(runtime.getMaxRecordsPerSecond()),
                optional(runtime.getMaxBytesPerSecond()),
                enumValue(runtime.getSinkPartitionStrategy(), SinkPartitionStrategy.TABLE_AFFINITY,
                        SinkPartitionStrategy.class, "runtime.sinkPartitionStrategy"),
                enumValue(runtime.getSplitAssignmentMode(), SplitAssignmentMode.STATIC_ROUND_ROBIN,
                        SplitAssignmentMode.class, "runtime.splitAssignmentMode"));

        return new JobDefinition(
                requireText(spec.getName(), "name"),
                source,
                sink,
                execution,
                compileMapping(spec.getMapping()));
    }

    private ColumnMapping compileMapping(JobSpec.Mapping mapping) {
        if (mapping == null || mapping.getColumns() == null || mapping.getColumns().isEmpty()) {
            return ColumnMapping.empty();
        }

        List<ColumnMapping.Item> items = new ArrayList<ColumnMapping.Item>();
        Set<String> sources = new HashSet<String>();
        Set<String> targets = new HashSet<String>();

        for (JobSpec.Column column : mapping.getColumns()) {
            if (column == null) {
                throw new IllegalArgumentException("mapping.columns must not contain null items");
            }
            ColumnMapping.Item item = new ColumnMapping.Item(column.getSource(), column.getTarget());
            if (!sources.add(item.getSource())) {
                throw new IllegalArgumentException(
                        "Duplicate source column in mapping: " + item.getSource());
            }
            if (!targets.add(item.getTarget())) {
                throw new IllegalArgumentException(
                        "Duplicate target column in mapping: " + item.getTarget());
            }
            items.add(item);
        }

        return new ColumnMapping(items);
    }

    private void requireProtocol(String apiVersion, String kind) {
        String normalizedVersion = requireText(apiVersion, "apiVersion");
        String normalizedKind = requireText(kind, "kind");
        if (!JobSpec.CURRENT_API_VERSION.equals(normalizedVersion)) {
            throw new IllegalArgumentException("Unsupported jobSpec apiVersion: " + normalizedVersion);
        }
        if (!JobSpec.BATCH_SYNC_KIND.equals(normalizedKind)) {
            throw new IllegalArgumentException("Unsupported jobSpec kind: " + normalizedKind);
        }
    }

    private JobSpec.Connector requireConnector(JobSpec.Connector connector, String name) {
        if (connector == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return connector;
    }

    private Map<String, Object> copy(Map<String, Object> options) {
        return options == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(options);
    }

    private int positive(Integer value, int fallback, String name) {
        int resolved = value == null ? fallback : value.intValue();
        if (resolved <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
        return resolved;
    }

    private long optional(Long value) {
        return value == null ? -1L : value.longValue();
    }

    private <E extends Enum<E>> E enumValue(
            String value, E fallback, Class<E> type, String name) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown " + name + ": " + value, exception);
        }
    }

    private String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
