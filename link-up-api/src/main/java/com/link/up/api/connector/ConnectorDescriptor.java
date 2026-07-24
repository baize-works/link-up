package com.link.up.api.connector;

import java.util.*;

/**
 * Small, transport-neutral description of a connector artifact.
 * Capability strings deliberately remain opaque: the framework does not register or interpret
 * capabilities that it does not consume.
 */
public final class ConnectorDescriptor {
    private final String identifier;
    private final String version;
    private final String apiVersion;
    private final Set<ConnectorType> types;
    private final Set<String> requiredCapabilities;
    private final String pluginPath;

    public ConnectorDescriptor(String identifier, String version, String apiVersion,
                               Set<ConnectorType> types, Set<String> requiredCapabilities, String pluginPath) {
        this.identifier = required(identifier, "identifier");
        this.version = required(version, "version");
        this.apiVersion = required(apiVersion, "apiVersion");
        this.types = Collections.unmodifiableSet(types == null || types.isEmpty()
                ? EnumSet.noneOf(ConnectorType.class) : EnumSet.copyOf(types));
        this.requiredCapabilities = Collections.unmodifiableSet(requiredCapabilities == null
                ? new LinkedHashSet<String>() : new LinkedHashSet<String>(requiredCapabilities));
        this.pluginPath = pluginPath;
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getVersion() {
        return version;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public Set<ConnectorType> getTypes() {
        return types;
    }

    public Set<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public String getPluginPath() {
        return pluginPath;
    }
}
