package com.link.up.api.dirtydata;

/**
 * Task-scoped collector. Implementations must throw on persistence errors.
 */
public interface DirtyDataCollector extends AutoCloseable {
    void open() throws Exception;

    void recordAttempt(long count);

    void collect(DirtyRecord record) throws Exception;

    DirtyDataSummary summary();

    void close(boolean successful) throws Exception;

    @Override
    default void close() throws Exception {
        close(false);
    }
}
