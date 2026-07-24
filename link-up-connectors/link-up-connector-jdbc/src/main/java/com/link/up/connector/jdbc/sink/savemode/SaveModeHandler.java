package com.link.up.connector.jdbc.sink.savemode;

/**
 * Applies schema and data save modes before a JDBC sink writes a table.
 *
 * <p>Keeping this lifecycle separate from row writing makes table preparation reusable and keeps
 * {@code JdbcOutputFormat} focused on batching and transactions.
 */
public interface SaveModeHandler extends AutoCloseable {

    void open();

    void handleSchemaSaveMode();

    void handleDataSaveMode();

    default void handleSaveMode() {
        handleSchemaSaveMode();
        handleDataSaveMode();
    }
}
