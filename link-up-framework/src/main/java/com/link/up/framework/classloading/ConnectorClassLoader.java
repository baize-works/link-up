package com.link.up.framework.classloading;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Isolated connector loader: framework/API contracts come from the parent, connector libraries do not.
 */
public final class ConnectorClassLoader extends URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    public ConnectorClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    private static boolean parentFirst(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
                || name.startsWith("sun.") || name.startsWith("com.link.up.api.")
                || name.startsWith("org.slf4j.") || name.startsWith("org.apache.logging.");
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null && parentFirst(name)) {
                try {
                    loaded = getParent().loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (loaded == null) {
                try {
                    loaded = findClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (loaded == null) loaded = getParent().loadClass(name);
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }
}
