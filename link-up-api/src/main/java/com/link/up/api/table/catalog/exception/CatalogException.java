package com.link.up.api.table.catalog.exception;

import com.link.up.api.exception.FluxRuntimeException;
import com.link.up.api.exception.error.FluxApiErrorCode;

/**
 * Catalog 操作异常。
 */
public class CatalogException extends FluxRuntimeException {

    private static final long serialVersionUID = 1L;

    public CatalogException(String message) {
        super(FluxApiErrorCode.CATALOG_INITIALIZE_FAILED, message);
    }

    public CatalogException(
            String message,
            Throwable cause) {

        super(FluxApiErrorCode.CATALOG_INITIALIZE_FAILED, message, cause);
    }

    public CatalogException(Throwable cause) {
        super(FluxApiErrorCode.CATALOG_INITIALIZE_FAILED, cause);
    }
}
