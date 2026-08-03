package com.link.up.connector.jdbc.utils;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.connector.jdbc.config.JdbcConnectionConfig;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * JDBC 只读连接预检。
 *
 * <p>仅加载驱动、建立连接并调用 {@link Connection#isValid(int)}，不执行 SQL，
 * 不开启事务，也不修改数据库对象。
 */
public final class JdbcConnectionPreflight {

    private JdbcConnectionPreflight() {
    }

    public static void validate(
            ReadonlyConfig options,
            ClassLoader classLoader)
            throws Exception {

        Objects.requireNonNull(
                options,
                "options must not be null");

        ClassLoader loader =
                Objects.requireNonNull(
                        classLoader,
                        "classLoader must not be null");

        JdbcConnectionConfig config =
                JdbcConnectionConfig.of(
                        options);

        Class<?> driverType =
                Class.forName(
                        config.getDriverName(),
                        true,
                        loader);

        if (!Driver.class.isAssignableFrom(
                driverType)) {
            throw new IllegalArgumentException(
                    "Configured JDBC driver does not implement java.sql.Driver: "
                            + config.getDriverName());
        }

        Driver driver =
                (Driver) driverType
                        .getDeclaredConstructor()
                        .newInstance();

        Properties properties =
                config.toProperties();

        Connection connection =
                driver.connect(
                        config.getUrl(),
                        properties);

        if (connection == null) {
            throw new SQLException(
                    "JDBC driver rejected URL: "
                            + config.getUrl());
        }

        try {
            int timeout =
                    Math.max(
                            1,
                            config.getConnectionCheckTimeoutSeconds());

            if (!connection.isValid(
                    timeout)) {
                throw new SQLException(
                        "JDBC connection validation returned false");
            }
        } finally {
            connection.close();
        }
    }
}
