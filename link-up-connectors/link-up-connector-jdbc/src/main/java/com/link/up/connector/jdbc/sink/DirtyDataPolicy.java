package com.link.up.connector.jdbc.sink;

/**
 * Defines how the JDBC sink handles records that cannot be written.
 */
public enum DirtyDataPolicy {
    /**
     * Abort the SinkTask and roll back its transaction.
     */
    FAIL_FAST,

    /**
     * Roll back the failed batch, retain its valid rows, and record bad rows.
     */
    SKIP
}
