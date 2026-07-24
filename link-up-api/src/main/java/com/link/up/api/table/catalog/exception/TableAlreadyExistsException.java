package com.link.up.api.table.catalog.exception;

import com.link.up.api.table.catalog.TablePath;

/**
 * 数据表已经存在。
 */
public final class TableAlreadyExistsException
        extends CatalogException {

    private static final long serialVersionUID = 1L;

    public TableAlreadyExistsException(
            String catalogName,
            TablePath tablePath) {

        this(
                catalogName,
                tablePath,
                null);
    }

    public TableAlreadyExistsException(
            String catalogName,
            TablePath tablePath,
            Throwable cause) {

        super(
                String.format(
                        "数据表已经存在，catalog=%s, table=%s",
                        catalogName,
                        tablePath),
                cause);
    }
}