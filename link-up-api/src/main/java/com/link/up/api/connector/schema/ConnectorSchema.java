package com.link.up.api.connector.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Link-Up 对控制面公开的 Connector Schema。
 */
public final class ConnectorSchema {

    private final String connectorId;
    private final ConnectorRole role;
    private final String schemaVersion;
    private final String schemaFingerprint;
    private final String implementationClass;
    private final String implementationVersion;
    private final List<ConnectorOptionSchema> options;
    private final List<ConnectorRuleSchema> rules;
    private final List<ConnectorCapability> capabilities;

    public ConnectorSchema(
            String connectorId,
            ConnectorRole role,
            String schemaVersion,
            String schemaFingerprint,
            String implementationClass,
            String implementationVersion,
            List<ConnectorOptionSchema> options,
            List<ConnectorRuleSchema> rules,
            List<ConnectorCapability> capabilities) {

        this.connectorId = connectorId;
        this.role = role;
        this.schemaVersion = schemaVersion;
        this.schemaFingerprint = schemaFingerprint;
        this.implementationClass = implementationClass;
        this.implementationVersion = implementationVersion;
        this.options =
                Collections.unmodifiableList(
                        new ArrayList<ConnectorOptionSchema>(options));
        this.rules =
                Collections.unmodifiableList(
                        new ArrayList<ConnectorRuleSchema>(rules));
        this.capabilities =
                Collections.unmodifiableList(
                        new ArrayList<ConnectorCapability>(capabilities));
    }

    public String getConnectorId() {
        return connectorId;
    }

    public ConnectorRole getRole() {
        return role;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getSchemaFingerprint() {
        return schemaFingerprint;
    }

    public String getImplementationClass() {
        return implementationClass;
    }

    public String getImplementationVersion() {
        return implementationVersion;
    }

    public List<ConnectorOptionSchema> getOptions() {
        return options;
    }

    public List<ConnectorRuleSchema> getRules() {
        return rules;
    }

    public List<ConnectorCapability> getCapabilities() {
        return capabilities;
    }
}
