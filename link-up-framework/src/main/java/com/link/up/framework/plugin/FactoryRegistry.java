package com.link.up.framework.plugin;

import com.link.up.api.factory.SinkFactory;
import com.link.up.api.factory.SourceFactory;

import java.util.*;

/**
 * Discovers connector factories through Java's ServiceLoader.
 */
public final class FactoryRegistry {
    private final Map<String, SourceFactory> sources;
    private final Map<String, SinkFactory> sinks;

    public FactoryRegistry(ClassLoader loader) {
        sources = index(ServiceLoader.load(SourceFactory.class, loader));
        sinks = index(ServiceLoader.load(SinkFactory.class, loader));
    }

    public static FactoryRegistry discover() {
        return new FactoryRegistry(Thread.currentThread().getContextClassLoader());
    }

    private static <T> Map<String, T> index(ServiceLoader<T> services) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T service : services) {
            String id = service instanceof SourceFactory ? ((SourceFactory) service).factoryIdentifier() : ((SinkFactory) service).factoryIdentifier();
            if (result.put(id.toLowerCase(Locale.ROOT), service) != null)
                throw new IllegalStateException("Duplicate factory: " + id);
        }
        return Collections.unmodifiableMap(result);
    }

    private static <T> T required(Map<String, T> factories, String type, String kind) {
        T f = factories.get(type.toLowerCase(Locale.ROOT));
        if (f == null) throw new IllegalArgumentException("No " + kind + " factory registered for type: " + type);
        return f;
    }

    public SourceFactory getSourceFactory(String type) {
        return required(sources, type, "source");
    }

    public SinkFactory getSinkFactory(String type) {
        return required(sinks, type, "sink");
    }
}
