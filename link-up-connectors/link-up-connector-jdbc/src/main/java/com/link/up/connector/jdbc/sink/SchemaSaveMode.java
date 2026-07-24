package com.link.up.connector.jdbc.sink;

public enum SchemaSaveMode {

    // Will create when the table does not exist, delete and rebuild when the table is saved
    RECREATE_SCHEMA,

    // Will Created when the table does not exist, skipped when the table is saved
    CREATE_SCHEMA_WHEN_NOT_EXIST,

    // Create when missing; when present, add source columns absent from target.
    CREATE_OR_ADD_COLUMNS,

    // Error will be reported when the table does not exist
    ERROR_WHEN_SCHEMA_NOT_EXIST,

    // Ignore creation
    IGNORE
}
