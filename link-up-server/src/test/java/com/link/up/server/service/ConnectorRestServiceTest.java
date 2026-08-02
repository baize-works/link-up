package com.link.up.server.service;

import com.link.up.api.connector.schema.ConnectorRole;
import com.link.up.api.connector.schema.ConnectorSchema;
import com.link.up.framework.connector.schema.ConnectorSchemaCatalog;
import com.link.up.server.http.RestException;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConnectorRestServiceTest {

    @Test
    public void shouldFilterAndReadSchema() {
        ConnectorSchema source =
                schema(
                        "jdbc",
                        ConnectorRole.SOURCE);

        ConnectorRestService service =
                new ConnectorRestService(
                        new ConnectorSchemaCatalog(
                                Collections.singletonList(
                                        source)));

        assertEquals(
                1,
                service.list("source")
                        .size());

        assertEquals(
                source,
                service.schema(
                        "jdbc",
                        "SOURCE"));
    }

    @Test
    public void shouldRequireRoleForSingleSchema() {
        ConnectorRestService service =
                new ConnectorRestService(
                        new ConnectorSchemaCatalog(
                                Collections.singletonList(
                                        schema(
                                                "jdbc",
                                                ConnectorRole.SOURCE))));

        try {
            service.schema(
                    "jdbc",
                    null);
            fail("Expected role validation");
        } catch (RestException expected) {
            assertEquals(
                    400,
                    expected.getHttpStatus());
            assertEquals(
                    "FLUX-CONNECTOR-ROLE-REQUIRED",
                    expected.getCode());
        }
    }

    @Test
    public void shouldKeepLegacyConstructorButDisablePreflight() {
        ConnectorRestService service =
                new ConnectorRestService(
                        new ConnectorSchemaCatalog(
                                Collections.singletonList(
                                        schema(
                                                "jdbc",
                                                ConnectorRole.SOURCE))));

        try {
            service.preflight(
                    "jdbc",
                    "SOURCE",
                    Collections.<String, Object>emptyMap());
            fail("Expected preflight disabled error");
        } catch (RestException expected) {
            assertEquals(
                    501,
                    expected.getHttpStatus());
            assertEquals(
                    "FLUX-CONNECTOR-PREFLIGHT-DISABLED",
                    expected.getCode());
        }
    }

    private ConnectorSchema schema(
            String connectorId,
            ConnectorRole role) {

        return new ConnectorSchema(
                connectorId,
                role,
                "1",
                "sha256:test",
                "example.Factory",
                "1.0.0",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }
}
