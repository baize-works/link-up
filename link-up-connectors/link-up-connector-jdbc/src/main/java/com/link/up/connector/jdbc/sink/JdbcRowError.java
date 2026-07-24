package com.link.up.connector.jdbc.sink;

import com.link.up.api.table.type.FluxRow;

import java.util.Objects;

/**
 * A row rejected by the JDBC sink while dirty-data skipping is enabled.
 */
public final class JdbcRowError {

    private final FluxRow row;
    private final Exception cause;

    JdbcRowError(FluxRow row, Exception cause) {
        this.row = Objects.requireNonNull(row, "row must not be null").copy();
        this.cause = Objects.requireNonNull(cause, "cause must not be null");
    }

    public FluxRow getRow() {
        return row.copy();
    }

    public Exception getCause() {
        return cause;
    }
}
