package com.link.up.framework.connector;

import com.link.up.api.factory.Factory;
import com.link.up.api.factory.SinkFactory;
import com.link.up.api.table.factory.TableSourceFactory;
import com.link.up.framework.classloading.ConnectorClassLoader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Discovers application and isolated connector factories.
 */
public final class FactoryRegistry implements AutoCloseable {
    private final Map<String, TableSourceFactory<?>> sourceFactories = new LinkedHashMap<String, TableSourceFactory<?>>();
    private final Map<String, SinkFactory> sinkFactories = new LinkedHashMap<String, SinkFactory>();
    private final Map<Factory, FactoryOrigin> origins = new IdentityHashMap<Factory, FactoryOrigin>();
    private final List<ConnectorClassLoader> pluginLoaders = new ArrayList<ConnectorClassLoader>();

    public FactoryRegistry(ClassLoader classLoader) {
        this(classLoader, Collections.<Path>emptyList());
    }

    public FactoryRegistry(ClassLoader classLoader, Iterable<Path> pluginDirectories) {
        ClassLoader parent = Objects.requireNonNull(classLoader, "classLoader must not be null");
        discoverFrom(parent, "application classpath");
        for (Path directory : pluginDirectories)
            discoverPlugin(parent, Objects.requireNonNull(directory, "plugin directory must not be null"));
    }

    public static FactoryRegistry discover(ClassLoader classLoader) {
        return new FactoryRegistry(effective(classLoader));
    }

    public static FactoryRegistry discover(ClassLoader classLoader, Path... pluginDirectories) {
        List<Path> paths = new ArrayList<Path>();
        if (pluginDirectories != null) Collections.addAll(paths, pluginDirectories);
        return new FactoryRegistry(effective(classLoader), paths);
    }

    public static FactoryRegistry discover(ClassLoader classLoader, Iterable<Path> pluginDirectories) {
        return new FactoryRegistry(effective(classLoader), pluginDirectories);
    }

    private static String describe(Factory factory, FactoryOrigin origin) {
        Package p = factory.getClass().getPackage();
        String version = p == null ? null : p.getImplementationVersion();
        return factory.getClass().getName() + " (version=" + (version == null ? "unknown" : version) + ", pluginPath=" + origin.path + ")";
    }

    private static <T> T required(Map<String, T> map, String id, String kind) {
        T factory = map.get(normalize(id));
        if (factory == null)
            throw new ConnectorException("Could not find " + kind + " factory for identifier '" + id + "'. Available identifiers: " + map.keySet());
        return factory;
    }

    private static ClassLoader effective(ClassLoader loader) {
        return loader == null ? Thread.currentThread().getContextClassLoader() : loader;
    }

    private static String normalize(String id) {
        Objects.requireNonNull(id, "factory identifier must not be null");
        String value = id.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException("factory identifier must not be blank");
        return value;
    }

    public TableSourceFactory<?> getSourceFactory(String identifier) {
        return required(sourceFactories, identifier, "source");
    }

    public SinkFactory getSinkFactory(String identifier) {
        return required(sinkFactories, identifier, "sink");
    }

    public ClassLoader getClassLoader(Factory factory) {
        FactoryOrigin origin = origins.get(factory);
        return origin == null ? factory.getClass().getClassLoader() : origin.loader;
    }

    private void discoverPlugin(ClassLoader parent, Path directory) {
        final URL[] urls;
        try {
            if (!Files.isDirectory(directory))
                throw new ConnectorException("Plugin path is not a directory: " + directory);
            List<URL> result = new ArrayList<URL>();
            java.nio.file.DirectoryStream<Path> jars = Files.newDirectoryStream(directory, "*.jar");
            try {
                for (Path jar : jars) result.add(jar.toUri().toURL());
            } finally {
                jars.close();
            }
            urls = result.toArray(new URL[result.size()]);
        } catch (IOException e) {
            throw new ConnectorException("Could not read plugin path '" + directory + "'", e);
        }
        ConnectorClassLoader loader = new ConnectorClassLoader(urls, parent);
        try {
            discoverFrom(loader, directory.toAbsolutePath().toString());
            pluginLoaders.add(loader);
        } catch (RuntimeException e) {
            try {
                loader.close();
            } catch (IOException close) {
                e.addSuppressed(close);
            }
            throw e;
        }
    }

    private void discoverFrom(ClassLoader loader, String path) {
        discoverSources(loader, path);
        discoverSinks(loader, path);
    }

    private void discoverSources(ClassLoader loader, String path) {
        try {
            for (TableSourceFactory<?> factory : ServiceLoader.load(TableSourceFactory.class, loader))
                put(sourceFactories, factory, "source", loader, path);
        } catch (ServiceConfigurationError e) {
            throw new ConnectorException("Could not load source factories from plugin path '" + path + "'", e);
        }
    }

    private void discoverSinks(ClassLoader loader, String path) {
        try {
            for (SinkFactory factory : ServiceLoader.load(SinkFactory.class, loader))
                put(sinkFactories, factory, "sink", loader, path);
        } catch (ServiceConfigurationError e) {
            throw new ConnectorException("Could not load sink factories from plugin path '" + path + "'", e);
        }
    }

    private <T extends Factory> void put(Map<String, T> factories, T factory, String kind, ClassLoader loader, String path) {
        String id = normalize(factory.factoryIdentifier());
        T prior = factories.get(id);
        if (prior != null) {
            FactoryOrigin previous = origins.get(prior);
            throw new ConnectorException("Duplicated " + kind + " factory identifier '" + id + "': "
                    + describe(prior, previous) + " conflicts with " + describe(factory, new FactoryOrigin(loader, path)));
        }
        factories.put(id, factory);
        origins.put(factory, new FactoryOrigin(loader, path));
    }

    @Override
    public void close() {
        IOException failure = null;
        for (ConnectorClassLoader loader : pluginLoaders)
            try {
                loader.close();
            } catch (IOException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        pluginLoaders.clear();
        if (failure != null) throw new ConnectorException("Could not close connector class loaders", failure);
    }

    private static final class FactoryOrigin {
        private final ClassLoader loader;
        private final String path;

        private FactoryOrigin(ClassLoader loader, String path) {
            this.loader = loader;
            this.path = path;
        }
    }
}
