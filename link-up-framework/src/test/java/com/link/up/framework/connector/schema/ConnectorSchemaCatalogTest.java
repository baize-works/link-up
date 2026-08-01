package com.link.up.framework.connector.schema;

import com.link.up.api.connector.schema.ConnectorCapability;
import com.link.up.api.connector.schema.ConnectorRole;
import com.link.up.api.connector.schema.ConnectorSchema;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConnectorSchemaCatalogTest {

    @Test
    public void shouldIndexSchemasByRoleAndIdentifier() {
        ConnectorSchema source =
                schema(
                        "jdbc",
                        ConnectorRole.SOURCE);

        ConnectorSchema sink =
                schema(
                        "jdbc",
                        ConnectorRole.SINK);

        ConnectorSchemaCatalog catalog =
                new ConnectorSchemaCatalog(
                        Arrays.asList(
                                sink,
                                source));

        assertEquals(
                2,
                catalog.list().size());

        assertEquals(
                source,
                catalog.get(
                        "JDBC",
                        ConnectorRole.SOURCE));

        assertEquals(
                1,
                catalog.list(
                                ConnectorRole.SINK)
                        .size());
    }

    @Test
    public void shouldRejectMissingSchema() {
        ConnectorSchemaCatalog catalog =
                new ConnectorSchemaCatalog(
                        Collections.singletonList(
                                schema(
                                        "jdbc",
                                        ConnectorRole.SOURCE)));

        try {
            catalog.get(
                    "doris",
                    ConnectorRole.SOURCE);
            fail("Expected missing schema failure");
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Connector Schema not found: SOURCE/doris",
                    expected.getMessage());
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
                Collections.singletonList(
                        ConnectorCapability.MULTI_TABLE));
    }
}
