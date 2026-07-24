package com.link.up.framework.classloading;

import java.util.Objects;

/**
 * Temporarily installs a connector loader as the current thread context class loader.
 */
public final class ClassLoaderScope implements AutoCloseable {
    private final Thread thread;
    private final ClassLoader previous;
    private boolean closed;

    private ClassLoaderScope(ClassLoader loader) {
        this.thread = Thread.currentThread();
        this.previous = thread.getContextClassLoader();
        thread.setContextClassLoader(Objects.requireNonNull(loader, "loader must not be null"));
    }

    public static ClassLoaderScope open(ClassLoader loader) {
        return new ClassLoaderScope(loader);
    }

    @Override
    public void close() {
        if (!closed) {
            thread.setContextClassLoader(previous);
            closed = true;
        }
    }
}
