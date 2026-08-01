package com.link.up.framework.connector.schema;

import com.link.up.api.connector.schema.ConnectorRole;
import com.link.up.api.connector.schema.ConnectorSchema;
import com.link.up.api.connector.schema.ConnectorSchemaExporter;
import com.link.up.api.factory.SinkFactory;
import com.link.up.api.table.factory.TableSourceFactory;
import com.link.up.framework.connector.FactoryRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Worker 启动时生成的 Connector Schema 只读目录。
 *
 * <p>目录只保留不可变协议对象，不持有 Connector ClassLoader。
 */
public final class ConnectorSchemaCatalog {

    private final Map<ConnectorRole, Map<String, ConnectorSchema>>
            schemas;

    public ConnectorSchemaCatalog(
            List<ConnectorSchema> schemas) {

        Map<ConnectorRole, Map<String, ConnectorSchema>>
                index =
                new EnumMap<ConnectorRole, Map<String, ConnectorSchema>>(
                        ConnectorRole.class);

        for (ConnectorRole role :
                ConnectorRole.values()) {
            index.put(
                    role,
                    new LinkedHashMap<String, ConnectorSchema>());
        }

        for (ConnectorSchema schema :
                schemas) {

            String id =
                    normalize(
                            schema.getConnectorId());

            ConnectorSchema previous =
                    index.get(schema.getRole())
                            .put(
                                    id,
                                    schema);

            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicated Connector Schema: "
                                + schema.getRole()
                                + "/"
                                + id);
            }
        }

        Map<ConnectorRole, Map<String, ConnectorSchema>>
                immutable =
                new EnumMap<ConnectorRole, Map<String, ConnectorSchema>>(
                        ConnectorRole.class);

        for (Map.Entry<ConnectorRole, Map<String, ConnectorSchema>>
                entry :
                index.entrySet()) {

            immutable.put(
                    entry.getKey(),
                    Collections.unmodifiableMap(
                            new LinkedHashMap<String, ConnectorSchema>(
                                    entry.getValue())));
        }

        this.schemas =
                Collections.unmodifiableMap(
                        immutable);
    }

    public static ConnectorSchemaCatalog discover(
            ClassLoader classLoader,
            Path... pluginDirectories) {

        FactoryRegistry registry =
                FactoryRegistry.discover(
                        classLoader,
                        pluginDirectories);

        try {
            ConnectorSchemaExporter exporter =
                    new ConnectorSchemaExporter();

            List<ConnectorSchema> schemas =
                    new ArrayList<ConnectorSchema>();

            for (TableSourceFactory<?> factory :
                    registry.getSourceFactories()
                            .values()) {

                schemas.add(
                        exporter.export(
                                factory,
                                ConnectorRole.SOURCE,
                                factory.optionRule()));
            }

            for (SinkFactory factory :
                    registry.getSinkFactories()
                            .values()) {

                schemas.add(
                        exporter.export(
                                factory,
                                ConnectorRole.SINK,
                                factory.optionRule()));
            }

            return new ConnectorSchemaCatalog(
                    schemas);
        } finally {
            registry.close();
        }
    }

    public List<ConnectorSchema> list() {
        List<ConnectorSchema> result =
                new ArrayList<ConnectorSchema>();

        for (Map<String, ConnectorSchema> byId :
                schemas.values()) {
            result.addAll(
                    byId.values());
        }

        sort(result);

        return Collections.unmodifiableList(
                result);
    }

    public List<ConnectorSchema> list(
            ConnectorRole role) {

        List<ConnectorSchema> result =
                new ArrayList<ConnectorSchema>(
                        schemas.get(role)
                                .values());

        sort(result);

        return Collections.unmodifiableList(
                result);
    }

    public ConnectorSchema get(
            String connectorId,
            ConnectorRole role) {

        ConnectorSchema schema =
                schemas.get(role)
                        .get(
                                normalize(
                                        connectorId));

        if (schema == null) {
            throw new IllegalArgumentException(
                    "Connector Schema not found: "
                            + role
                            + "/"
                            + connectorId);
        }

        return schema;
    }

    private static void sort(
            List<ConnectorSchema> schemas) {

        Collections.sort(
                schemas,
                new Comparator<ConnectorSchema>() {
                    public int compare(
                            ConnectorSchema first,
                            ConnectorSchema second) {

                        int id =
                                first.getConnectorId()
                                        .compareTo(
                                                second.getConnectorId());

                        return id != 0
                                ? id
                                : first.getRole()
                                .compareTo(
                                        second.getRole());
                    }
                });
    }

    private static String normalize(
            String connectorId) {

        if (connectorId == null
                || connectorId
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "connectorId must not be blank");
        }

        return connectorId
                .trim()
                .toLowerCase(
                        Locale.ROOT);
    }
}
