package com.link.up.api.table.catalog.exception;

/**
 * 数据库已经存在。
 */
public final class DatabaseAlreadyExistsException
        extends CatalogException {

    private static final long serialVersionUID = 1L;

    public DatabaseAlreadyExistsException(
            String catalogName,
            String databaseName) {

        this(
                catalogName,
                databaseName,
                null);
    }

    public DatabaseAlreadyExistsException(
            String catalogName,
            String databaseName,
            Throwable cause) {

        super(
                String.format(
                        "数据库已经存在，catalog=%s, database=%s",
                        catalogName,
                        databaseName),
                cause);
    }
}
