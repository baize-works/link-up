package com.link.up.api.table.catalog.exception;

import com.link.up.api.table.catalog.TablePath;

/**
 * 数据表不存在。
 */
public final class TableNotFoundException
        extends CatalogException {

    private static final long serialVersionUID = 1L;

    public TableNotFoundException(
            String catalogName,
            TablePath tablePath) {

        this(
                catalogName,
                tablePath,
                null);
    }

    public TableNotFoundException(
            String catalogName,
            TablePath tablePath,
            Throwable cause) {

        super(
                String.format(
                        "数据表不存在，catalog=%s, table=%s",
                        catalogName,
                        tablePath),
                cause);
    }
}