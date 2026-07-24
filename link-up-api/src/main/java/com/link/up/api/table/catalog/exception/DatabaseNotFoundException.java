package com.link.up.api.table.catalog.exception;

/**
 * 数据库不存在。
 */
public final class DatabaseNotFoundException
        extends CatalogException {

    private static final long serialVersionUID = 1L;

    public DatabaseNotFoundException(
            String catalogName,
            String databaseName) {

        this(
                catalogName,
                databaseName,
                null);
    }

    public DatabaseNotFoundException(
            String catalogName,
            String databaseName,
            Throwable cause) {

        super(
                String.format(
                        "数据库不存在，catalog=%s, database=%s",
                        catalogName,
                        databaseName),
                cause);
    }
}
