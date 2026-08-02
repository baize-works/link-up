package com.link.up.server;

import com.link.up.api.connector.schema.ConnectorRole;
import com.link.up.api.connector.schema.ConnectorSchema;
import com.link.up.framework.connector.schema.ConnectorSchemaCatalog;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Server 默认运行时 Connector 发现测试。
 *
 * <p>保证直接启动 {@link FluxServer} 时，JDBC Source/Sink 工厂已经位于应用
 * classpath，并且能够通过 ServiceLoader 导出 Connector Schema。
 */
public class BuiltInConnectorDiscoveryTest {

    @Test
    public void shouldDiscoverJdbcSourceAndSinkSchemasFromServerClasspath() {
        ConnectorSchemaCatalog catalog =
                ConnectorSchemaCatalog.discover(
                        Thread.currentThread()
                                .getContextClassLoader());

        ConnectorSchema source =
                catalog.get(
                        "jdbc",
                        ConnectorRole.SOURCE);

        ConnectorSchema sink =
                catalog.get(
                        "jdbc",
                        ConnectorRole.SINK);

        assertNotNull(source);
        assertNotNull(sink);
        assertEquals("jdbc", source.getConnectorId());
        assertEquals("jdbc", sink.getConnectorId());
        assertFalse(source.getOptions().isEmpty());
        assertFalse(sink.getOptions().isEmpty());
    }
}
