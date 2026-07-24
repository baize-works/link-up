package com.link.up.connector.jdbc.source;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.connector.jdbc.config.JdbcSourceConfig;
import com.link.up.connector.jdbc.config.ReadConsistency;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.core.dialect.JdbcDialectLoader;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class JdbcReadConsistencyTest {

    @Test
    public void shouldDefaultToBestEffortAndAllowParallelReaders() {
        JdbcSourceConfig config = config("");

        assertEquals(ReadConsistency.BEST_EFFORT, config.getReadConsistency());
        new JdbcSource(config).validateParallelism(4);
    }

    @Test
    public void shouldAllowSingleConnectionSnapshotWithOneReader() {
        new JdbcSource(config("read_consistency = SINGLE_CONNECTION_SNAPSHOT"))
                .validateParallelism(1);
    }

    @Test
    public void shouldConfigureSingleConnectionAsReadOnlyRepeatableReadTransaction()
            throws Exception {
        JdbcSourceConfig config = config(
                "read_consistency = SINGLE_CONNECTION_SNAPSHOT");
        JdbcDialect dialect = JdbcDialectLoader.load(config.getConnectionConfig());
        final List<String> calls = new ArrayList<String>();
        Connection connection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("setReadOnly".equals(method.getName())) {
                        calls.add("readOnly=" + args[0]);
                    } else if ("setTransactionIsolation".equals(method.getName())) {
                        calls.add("isolation=" + args[0]);
                    } else if ("setAutoCommit".equals(method.getName())) {
                        calls.add("autoCommit=" + args[0]);
                    }
                    return null;
                });

        dialect.configureSnapshotConnection(
                connection, ReadConsistency.SINGLE_CONNECTION_SNAPSHOT);

        assertTrue(calls.contains("readOnly=true"));
        assertTrue(calls.contains(
                "isolation=" + Connection.TRANSACTION_REPEATABLE_READ));
        assertTrue(calls.contains("autoCommit=false"));
    }

    @Test
    public void shouldRejectSingleConnectionSnapshotWithParallelReaders() {
        assertFailureWithoutSecrets(
                "read_consistency = SINGLE_CONNECTION_SNAPSHOT",
                2,
                "SINGLE_CONNECTION_SNAPSHOT");
    }

    @Test
    public void shouldRejectUnsupportedDatabaseSnapshotBeforeReadersExist() {
        assertFailureWithoutSecrets(
                "read_consistency = DATABASE_SNAPSHOT",
                1,
                "DATABASE_SNAPSHOT");
    }

    private void assertFailureWithoutSecrets(
            String consistency,
            int parallelism,
            String expectedText) {
        try {
            new JdbcSource(config(consistency)).validateParallelism(parallelism);
            fail("Expected consistency validation to fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains(expectedText));
            assertFalse(expected.getMessage().contains("super-secret-password"));
            assertFalse(expected.getMessage().contains("super-secret-token"));
        }
    }

    private JdbcSourceConfig config(String consistency) {
        return JdbcSourceConfig.of(ReadonlyConfig.fromConfig(
                ConfigFactory.parseString(
                        "url = \"jdbc:mysql://localhost:3306/flux_test\"\n"
                                + "driver = \"com.mysql.cj.jdbc.Driver\"\n"
                                + "password = \"super-secret-password\"\n"
                                + "properties { token = \"super-secret-token\" }\n"
                                + "table_path = \"flux_test.orders\"\n"
                                + consistency)));
    }
}
