package com.link.up.api.sink;

/**
 * Performs connector-specific validation and DDL before task startup.
 */
public interface SinkPreparer {
    PreparedSinkMetadata prepare(SinkPrepareContext context) throws Exception;
}
