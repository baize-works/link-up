package com.link.up.connector.jdbc.internal;

import com.link.up.connector.jdbc.config.JdbcConnectionConfig;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Owns one lazily-created JDBC connection and recreates it when it is no longer usable.
 *
 * <p>Dialect defaults are applied first. Explicit connector properties, including credentials,
 * always take precedence over those defaults.</p>
 */
public final class JdbcConnectionProvider implements AutoCloseable, Serializable {

    private static final long serialVersionUID = 1L;

    private final JdbcConnectionConfig config;
    private final JdbcDialect dialect;

    private transient Driver loadedDriver;
    private transient Connection connection;

    public JdbcConnectionProvider(JdbcConnectionConfig config, JdbcDialect dialect) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.dialect = Objects.requireNonNull(dialect, "dialect must not be null");
    }

    private static Driver loadDriver(String driverName) throws ClassNotFoundException, SQLException {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getName().equals(driverName)) {
                return driver;
            }
        }

        Class<?> driverClass = Class.forName(driverName, true, Thread.currentThread().getContextClassLoader());
        if (!Driver.class.isAssignableFrom(driverClass)) {
            throw new SQLException("Configured JDBC driver does not implement java.sql.Driver: " + driverName);
        }
        try {
            return (Driver) driverClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Unable to create JDBC driver: " + driverName, e);
        }
    }

    /**
     * Returns the managed connection, creating it when necessary.
     */
    public synchronized Connection getOrEstablishConnection() throws SQLException, ClassNotFoundException {
        if (isConnectionValid()) {
            return connection;
        }

        closeConnection();
        connection = getLoadedDriver().connect(config.getUrl(), createProperties());
        if (connection == null) {
            throw new SQLException("No suitable driver found for configured JDBC URL: " + config.getUrl());
        }
        return connection;
    }

    /**
     * Returns the currently managed connection, or {@code null} before it has been opened.
     */
    public synchronized Connection getConnection() {
        return connection;
    }

    public synchronized boolean isConnectionValid() {
        if (connection == null) {
            return false;
        }
        try {
            return !connection.isClosed()
                    && connection.isValid(config.getConnectionCheckTimeoutSeconds());
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Closes any managed connection and creates a replacement.
     */
    public synchronized Connection reestablishConnection() throws SQLException, ClassNotFoundException {
        closeConnection();
        return getOrEstablishConnection();
    }

    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Closing is best effort; clearing the stale reference is still essential.
            } finally {
                connection = null;
            }
        }
    }

    @Override
    public void close() {
        closeConnection();
    }

    private Properties createProperties() {
        Properties result = new Properties();
        for (Map.Entry<String, String> entry : dialect.resolveConnectionProperties(config.getProperties()).entrySet()) {
            result.setProperty(entry.getKey(), entry.getValue());
        }
        result.putAll(config.toProperties());
        return result;
    }

    private Driver getLoadedDriver() throws ClassNotFoundException, SQLException {
        if (loadedDriver == null) {
            loadedDriver = loadDriver(config.getDriverName());
        }
        return loadedDriver;
    }
}
