package com.link.up.server.service;

import com.link.up.api.connector.schema.ConnectorRole;
import com.link.up.api.connector.schema.ConnectorSchema;
import com.link.up.framework.connector.schema.ConnectorSchemaCatalog;
import com.link.up.server.http.RestException;

import java.util.List;
import java.util.Locale;

/**
 * Connector Schema REST 查询服务。
 */
public final class ConnectorRestService {

    private final ConnectorSchemaCatalog catalog;

    public ConnectorRestService(
            ConnectorSchemaCatalog catalog) {

        if (catalog == null) {
            throw new IllegalArgumentException(
                    "catalog must not be null");
        }

        this.catalog = catalog;
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
}
