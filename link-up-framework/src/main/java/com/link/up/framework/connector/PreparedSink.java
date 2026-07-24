package com.link.up.framework.connector;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.factory.SinkFactory;
import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.sink.SinkWriter;
import com.link.up.api.table.type.FluxRow;

import java.util.Objects;

/**
 * Immutable sink configuration and metadata; writers are runtime resources.
 */
public final class PreparedSink {
    private final String factoryIdentifier;
    private final SinkFactory factory;
    private final ReadonlyConfig options;
    private final PreparedSinkMetadata metadata;
    private final ClassLoader classLoader;

    public PreparedSink(String factoryIdentifier, SinkFactory factory, ReadonlyConfig options, PreparedSinkMetadata metadata) {
        this(factoryIdentifier, factory, options, metadata, factory.getClass().getClassLoader());
    }

    public PreparedSink(String factoryIdentifier, SinkFactory factory, ReadonlyConfig options, PreparedSinkMetadata metadata, ClassLoader classLoader) {
        this.factoryIdentifier = Objects.requireNonNull(factoryIdentifier, "factoryIdentifier must not be null");
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
    }

    public SinkWriter<FluxRow> createWriter() {
        SinkWriter<FluxRow> writer;
        try (com.link.up.framework.classloading.ClassLoaderScope ignored = com.link.up.framework.classloading.ClassLoaderScope.open(classLoader)) {
            writer = factory.createSink(options, metadata);
        }
        if (writer == null)
            throw new ConnectorException("Sink factory '" + factoryIdentifier + "' returned a null writer");
        return writer;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getFactoryIdentifier() {
        return factoryIdentifier;
    }

    public ReadonlyConfig getOptions() {
        return options;
    }

    public PreparedSinkMetadata getMetadata() {
        return metadata;
    }
}
