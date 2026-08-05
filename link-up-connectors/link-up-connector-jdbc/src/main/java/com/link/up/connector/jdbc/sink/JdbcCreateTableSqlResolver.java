package com.link.up.connector.jdbc.sink;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.connector.jdbc.catalog.mysql.MySqlCreateTableSqlBuilder;
import com.link.up.connector.jdbc.catalog.mysql.MySqlTypeMapper;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.core.dialect.JdbcTypeMapper;

/** Generates the CREATE TABLE statement used for offline sink preparation. */
final class JdbcCreateTableSqlResolver {

    private JdbcCreateTableSqlResolver() {
    }

    static String resolve(
            JdbcDialect dialect,
            CatalogTable table) {

        if (dialect == null || table == null) {
            return null;
        }

        JdbcTypeMapper typeMapper =
                dialect.typeMapper();

        if (typeMapper instanceof MySqlTypeMapper) {
            return new MySqlCreateTableSqlBuilder(
                    table.getTablePath(),
                    table,
                    (MySqlTypeMapper) typeMapper)
                    .build();
        }

        /*
         * Future JDBC dialects can add their own CREATE TABLE builder here
         * without changing the connector-neutral protocol.
         */
        return null;
    }
}
