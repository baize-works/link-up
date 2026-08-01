package com.link.up.api.connector.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.up.api.configuration.Option;
import com.link.up.api.configuration.Options;
import com.link.up.api.configuration.util.Conditions;
import com.link.up.api.configuration.util.OptionRule;
import com.link.up.api.factory.SourceFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConnectorSchemaExporterTest {

    private static final Option<String> MODE =
            Options.key("mode")
                    .singleChoice(
                            String.class,
                            Arrays.asList(
                                    "TABLE",
                                    "SQL"))
                    .defaultValue("TABLE")
                    .withDescription("读取方式")
                    .withSemanticType("READ_MODE");

    private static final Option<String> TABLE =
            Options.key("table")
                    .stringType()
                    .noDefaultValue()
                    .withSemanticType("TABLE_PATH");

    private static final Option<String> QUERY =
            Options.key("query")
                    .stringType()
                    .noDefaultValue()
                    .withSemanticType("SQL");

    private static final Option<String> USERNAME =
            Options.key("username")
                    .stringType()
                    .noDefaultValue()
                    .withScope(
                            ConnectorOptionScope.DATASOURCE);

    private static final Option<String> PASSWORD =
            Options.key("password")
                    .stringType()
                    .noDefaultValue()
                    .withScope(
                            ConnectorOptionScope.DATASOURCE)
                    .withSemanticType("PASSWORD")
                    .sensitive();

    private static final Option<Integer> PARALLELISM =
            Options.key("parallelism")
                    .intType()
                    .defaultValue(1)
                    .withScope(
                            ConnectorOptionScope.RUNTIME);

    private static final OptionRule RULE =
            OptionRule.builder()
                    .required(MODE)
                    .conditional(
                            MODE,
                            "TABLE",
                            TABLE)
                    .conditional(
                            MODE,
                            "SQL",
                            QUERY)
                    .bundled(
                            USERNAME,
                            PASSWORD)
                    .required(
                            PARALLELISM,
                            Conditions.greaterOrEqual(
                                    PARALLELISM,
                                    1))
                    .build();

    @Test
    public void shouldExportOptionsRulesCapabilitiesAndFingerprint()
            throws Exception {

        ConnectorSchema schema =
                new ConnectorSchemaExporter()
                        .export(
                                new TestSourceFactory(),
                                ConnectorRole.SOURCE,
                                RULE);

        assertEquals(
                "test",
                schema.getConnectorId());
        assertEquals(
                ConnectorRole.SOURCE,
                schema.getRole());
        assertEquals(
                "2",
                schema.getSchemaVersion());
        assertTrue(
                schema.getSchemaFingerprint()
                        .startsWith("sha256:"));
        assertEquals(
                64 + "sha256:".length(),
                schema.getSchemaFingerprint()
                        .length());

        ConnectorOptionSchema password =
                option(
                        schema,
                        "password");

        assertTrue(password.isSensitive());
        assertEquals(
                "PASSWORD",
                password.getSemanticType());
        assertEquals(
                ConnectorOptionScope.DATASOURCE,
                password.getScope());
        assertFalse(password.isRequired());

        ConnectorOptionSchema mode =
                option(
                        schema,
                        "mode");

        assertEquals(
                Arrays.<Object>asList(
                        "TABLE",
                        "SQL"),
                mode.getAllowedValues());
        assertTrue(mode.isRequired());

        assertTrue(
                hasRule(
                        schema,
                        ConnectorRuleSchema.REQUIRED_WHEN));
        assertTrue(
                hasRule(
                        schema,
                        ConnectorRuleSchema.BUNDLED));
        assertTrue(
                hasRule(
                        schema,
                        ConnectorRuleSchema.CONSTRAINT));

        assertTrue(
                schema.getCapabilities()
                        .contains(
                                ConnectorCapability.CUSTOM_SQL));

        JsonNode json =
                new ObjectMapper()
                        .valueToTree(schema);

        assertEquals(
                "SOURCE",
                json.path("role")
                        .asText());
        assertEquals(
                "READ_MODE",
                json.path("options")
                        .get(0)
                        .path("semanticType")
                        .asText());
        assertNotNull(
                json.path("rules"));
    }

    @Test
    public void shouldGenerateStableFingerprint() {
        ConnectorSchemaExporter exporter =
                new ConnectorSchemaExporter();

        String first =
                exporter.export(
                                new TestSourceFactory(),
                                ConnectorRole.SOURCE,
                                RULE)
                        .getSchemaFingerprint();

        String second =
                exporter.export(
                                new TestSourceFactory(),
                                ConnectorRole.SOURCE,
                                RULE)
                        .getSchemaFingerprint();

        assertEquals(
                first,
                second);
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

    private boolean hasRule(
            ConnectorSchema schema,
            String type) {

        for (ConnectorRuleSchema rule :
                schema.getRules()) {

            if (type.equals(
                    rule.getType())) {
                return true;
            }
        }

        return false;
    }

    private static final class TestSourceFactory
            implements SourceFactory {

        @Override
        public String factoryIdentifier() {
            return "test";
        }

        @Override
        public String schemaVersion() {
            return "2";
        }

        @Override
        public Set<ConnectorCapability> capabilities() {
            return Collections.unmodifiableSet(
                    EnumSet.of(
                            ConnectorCapability.CUSTOM_SQL,
                            ConnectorCapability.MULTI_TABLE));
        }

        @Override
        public OptionRule optionRule() {
            return RULE;
        }
    }
}
