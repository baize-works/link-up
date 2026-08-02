package com.link.up.server.service;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.connector.preflight.ConnectorPreflightSupport;
import com.link.up.api.connector.schema.ConnectorRole;
import com.link.up.api.connector.schema.ConnectorSchema;
import com.link.up.api.factory.Factory;
import com.link.up.framework.connector.FactoryRegistry;
import com.link.up.framework.connector.schema.ConnectorSchemaCatalog;
import com.link.up.server.http.RestException;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Connector Schema 查询与只读预检服务。
 */
public final class ConnectorRestService {

    private final ConnectorSchemaCatalog catalog;
    private final FactoryRegistry registry;

    /** 保留只读 Schema 查询测试和旧调用兼容。 */
    public ConnectorRestService(
            ConnectorSchemaCatalog catalog) {

        this(
                catalog,
                null);
    }

    public ConnectorRestService(
            ConnectorSchemaCatalog catalog,
            FactoryRegistry registry) {

        if (catalog == null) {
            throw new IllegalArgumentException(
                    "catalog must not be null");
        }

        this.catalog = catalog;
        this.registry = registry;
    }

    public List<ConnectorSchema> list(
            String roleValue) {

        if (roleValue == null
                || roleValue.trim().isEmpty()) {
            return catalog.list();
        }

        return catalog.list(
                role(roleValue));
    }

    public ConnectorSchema schema(
            String connectorId,
            String roleValue) {

        if (roleValue == null
                || roleValue.trim().isEmpty()) {

            throw new RestException(
                    400,
                    "FLUX-CONNECTOR-ROLE-REQUIRED",
                    "Query parameter 'role' is required");
        }

        ConnectorRole role =
                role(roleValue);

        try {
            return catalog.get(
                    connectorId,
                    role);
        } catch (IllegalArgumentException exception) {
            throw new RestException(
                    404,
                    "FLUX-CONNECTOR-SCHEMA-NOT-FOUND",
                    "Connector Schema not found: "
                            + role
                            + "/"
                            + connectorId);
        }
    }

    public ConnectorPreflightResponse preflight(
            String connectorId,
            String roleValue,
            Map<String, Object> options) {

        if (registry == null) {
            throw new RestException(
                    501,
                    "FLUX-CONNECTOR-PREFLIGHT-DISABLED",
                    "Connector preflight is not available in this server");
        }

        ConnectorRole role =
                roleRequired(
                        roleValue);

        Factory factory;

        try {
            factory =
                    role == ConnectorRole.SOURCE
                            ? registry.getSourceFactory(
                            connectorId)
                            : registry.getSinkFactory(
                            connectorId);
        } catch (RuntimeException exception) {
            throw new RestException(
                    404,
                    "FLUX-CONNECTOR-NOT-FOUND",
                    "Connector factory not found: "
                            + role
                            + "/"
                            + connectorId);
        }

        if (!(factory instanceof ConnectorPreflightSupport)) {
            throw new RestException(
                    422,
                    "FLUX-CONNECTOR-PREFLIGHT-UNSUPPORTED",
                    "Connector does not support safe preflight: "
                            + role
                            + "/"
                            + connectorId);
        }

        Map<String, Object> safeOptions =
                options == null
                        ? Collections.<String, Object>emptyMap()
                        : options;

        long started =
                System.nanoTime();

        try {
            ((ConnectorPreflightSupport) factory)
                    .preflight(
                            ReadonlyConfig.fromMap(
                                    safeOptions),
                            registry.getClassLoader(
                                    factory));
        } catch (IllegalArgumentException exception) {
            throw new RestException(
                    400,
                    "FLUX-CONNECTOR-PREFLIGHT-CONFIG-INVALID",
                    sanitize(
                            exception.getMessage(),
                            "Connector preflight configuration is invalid"));
        } catch (Exception exception) {
            throw new RestException(
                    503,
                    "FLUX-CONNECTOR-PREFLIGHT-FAILED",
                    sanitize(
                            exception.getMessage(),
                            "Connector preflight failed"));
        }

        long durationMillis =
                Math.max(
                        0L,
                        (System.nanoTime() - started)
                                / 1_000_000L);

        return new ConnectorPreflightResponse(
                factory.factoryIdentifier(),
                role.name(),
                "REACHABLE",
                durationMillis,
                System.currentTimeMillis());
    }

    private ConnectorRole roleRequired(
            String roleValue) {

        if (roleValue == null
                || roleValue.trim().isEmpty()) {
            throw new RestException(
                    400,
                    "FLUX-CONNECTOR-ROLE-REQUIRED",
                    "Query parameter 'role' is required");
        }

        return role(
                roleValue);
    }

    private ConnectorRole role(
            String value) {

        try {
            return ConnectorRole.valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new RestException(
                    400,
                    "FLUX-CONNECTOR-ROLE-INVALID",
                    "role must be SOURCE or SINK");
        }
    }

    private String sanitize(
            String message,
            String fallback) {

        if (message == null
                || message.trim().isEmpty()) {
            return fallback;
        }

        String sanitized =
                message.replaceAll(
                        "(?i)(password|passwd|pwd)\\s*[=:]\\s*[^,;\\s]+",
                        "$1=***");

        return sanitized.length() <= 1000
                ? sanitized
                : sanitized.substring(
                0,
                1000);
    }
}
