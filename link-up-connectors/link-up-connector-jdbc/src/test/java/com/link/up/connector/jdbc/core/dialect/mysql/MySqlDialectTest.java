package com.link.up.connector.jdbc.core.dialect.mysql;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.connector.jdbc.config.JdbcConnectionConfig;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MySqlDialectTest {

    @Test
    public void usesDatabaseFromJdbcUrlForUnqualifiedTable() {
        MySqlDialect dialect =
                dialect(
                        "jdbc:mysql://127.0.0.1:3306/test1?useSSL=false",
                        null);

        assertEquals(
                "`test1`.`test1234`",
                dialect.tableIdentifier(
                        TablePath.of("test1234")));
    }

    @Test
    public void explicitTableDatabaseTakesPrecedence() {
        MySqlDialect dialect =
                dialect(
                        "jdbc:mysql://127.0.0.1:3306/test1",
                        null);

        assertEquals(
                "`archive`.`test1234`",
                dialect.tableIdentifier(
                        TablePath.of(
                                "archive",
                                "test1234")));
    }

    @Test
    public void configuredSchemaTakesPrecedenceOverJdbcUrl() {
        MySqlDialect dialect =
                dialect(
                        "jdbc:mysql://127.0.0.1:3306/test1",
                        "configured_db");

        assertEquals(
                "`configured_db`.`test1234`",
                dialect.tableIdentifier(
                        TablePath.of("test1234")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnqualifiedTableWithoutDefaultDatabase() {
        MySqlDialect dialect =
                dialect(
                        "jdbc:mysql://127.0.0.1:3306",
                        null);

        dialect.tableIdentifier(
                TablePath.of("test1234"));
    }

    private static MySqlDialect dialect(
            String url,
            String schema) {

        Map<String, Object> values =
                new LinkedHashMap<String, Object>();

        values.put("url", url);
        values.put(
                "driver",
                "com.mysql.cj.jdbc.Driver");

        if (schema != null) {
            values.put("schema", schema);
        }

        JdbcConnectionConfig config =
                JdbcConnectionConfig.of(
                        ReadonlyConfig.fromMap(values));

        return new MySqlDialect(config);
    }
}
