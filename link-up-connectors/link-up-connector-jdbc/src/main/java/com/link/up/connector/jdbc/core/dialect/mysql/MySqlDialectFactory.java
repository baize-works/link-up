package com.link.up.connector.jdbc.core.dialect.mysql;

import com.link.up.connector.jdbc.config.JdbcConnectionConfig;
import com.link.up.connector.jdbc.core.dialect.DatabaseIdentifier;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.core.dialect.JdbcDialectFactory;
import com.google.auto.service.AutoService;

/**
 * MySQL 方言工厂。
 */
@AutoService(JdbcDialectFactory.class)
public final class MySqlDialectFactory
        implements JdbcDialectFactory {

    @Override
    public String identifier() {
        return DatabaseIdentifier.MYSQL;
    }

    @Override
    public boolean acceptsUrl(String url) {
        return url != null
                && url.trim()
                .toLowerCase()
                .startsWith("jdbc:mysql:");
    }

    @Override
    public JdbcDialect create(
            JdbcConnectionConfig connectionConfig) {

        return new MySqlDialect(
                connectionConfig);
    }
}
