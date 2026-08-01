package com.link.up.connector.jdbc;

import com.link.up.api.connector.schema.ConnectorCapability;
import com.link.up.api.connector.schema.ConnectorOptionSchema;
import com.link.up.api.connector.schema.ConnectorOptionScope;
import com.link.up.api.connector.schema.ConnectorRole;
import com.link.up.api.connector.schema.ConnectorSchema;
import com.link.up.api.connector.schema.ConnectorSchemaExporter;
import com.link.up.connector.jdbc.sink.JdbcSinkFactory;
import com.link.up.connector.jdbc.source.JdbcSourceFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JdbcConnectorSchemaTest {

    @Test
    public void shouldExportJdbcSourceMetadata() {
        JdbcSourceFactory factory =
                new JdbcSourceFactory();

        ConnectorSchema schema =
                new ConnectorSchemaExporter()
                        .export(
                                factory,
                                ConnectorRole.SOURCE,
                                factory.optionRule());

        assertEquals(
                "jdbc",
                schema.getConnectorId());

        ConnectorOptionSchema url =
                option(schema, "url");

        assertTrue(url.isRequired());
        assertEquals(
                "JDBC_URL",
                url.getSemanticType());
        assertEquals(
                ConnectorOptionScope.DATASOURCE,
                url.getScope());

        ConnectorOptionSchema password =
                option(schema, "password");

        assertTrue(
                password.isSensitive());

        assertTrue(
                schema.getCapabilities()
                        .contains(
                                ConnectorCapability
                                        .TABLE_SCHEMA_DISCOVERY));
        assertTrue(
                schema.getCapabilities()
                        .contains(
                                ConnectorCapability
                                        .PARTITION_SPLIT));
    }

    @Test
    public void shouldExportJdbcSinkMetadata() {
        JdbcSinkFactory factory =
                new JdbcSinkFactory();

        ConnectorSchema schema =
                new ConnectorSchemaExporter()
                        .export(
                                factory,
                                ConnectorRole.SINK,
                                factory.optionRule());

        assertEquals(
                "TABLE_PATH",
                option(
                        schema,
                        "table_path")
                        .getSemanticType());

        assertEquals(
                "DIRTY_DATA_OUTPUT",
                option(
                        schema,
                        "dirty_data_output_type")
                        .getSemanticType());

        assertEquals(
                "FILE_PATH",
                option(
                        schema,
                        "dirty_data_output_path")
                        .getSemanticType());

        assertTrue(
                schema.getCapabilities()
                        .contains(
                                ConnectorCapability.UPSERT));
        assertTrue(
                schema.getCapabilities()
                        .contains(
                                ConnectorCapability
                                        .DIRTY_DATA_HANDLING));
    }

    private ConnectorOptionSchema option(
            ConnectorSchema schema,
            String key) {

        for (ConnectorOptionSchema option :
                schema.getOptions()) {

            if (key.equals(
                    option.getKey())) {
                return option;
            }
        }

        throw new AssertionError(
                "Option not found: "
                        + key);
    }
}
