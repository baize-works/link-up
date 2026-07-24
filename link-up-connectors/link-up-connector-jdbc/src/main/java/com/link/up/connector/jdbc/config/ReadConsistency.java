package com.link.up.connector.jdbc.config;

/**
 * Consistency guarantee requested for a bounded JDBC source read.
 */
public enum ReadConsistency {

    /**
     * Preserve independent reader connections and existing parallel behaviour.
     * A parallel read is not guaranteed to observe one database snapshot.
     */
    BEST_EFFORT,

    /**
     * Read through one read-only transaction. This requires one source reader.
     */
    SINGLE_CONNECTION_SNAPSHOT,

    /**
     * Ask the database dialect to coordinate a snapshot across readers.
     */
    DATABASE_SNAPSHOT
}
