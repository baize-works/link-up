package com.link.up.api.dirtydata;

import java.io.Serializable;

/**
 * Safe-to-serialize dirty record: no Throwable or raw record payload is retained.
 */
public final class DirtyRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String errorType, errorMessage;
    private final DirtyDataContext context;
    private final long timestampMillis;

    public DirtyRecord(String errorType, String errorMessage, DirtyDataContext context, long timestampMillis) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.context = context;
        this.timestampMillis = timestampMillis;
    }

    public static DirtyRecord from(Throwable error, String sanitizedMessage, DirtyDataContext context) {
        return new DirtyRecord(error.getClass().getName(), sanitizedMessage, context, System.currentTimeMillis());
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DirtyDataContext getContext() {
        return context;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
