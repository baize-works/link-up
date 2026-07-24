package com.link.up.server.http;

public final class RestException
        extends RuntimeException {

    private final int httpStatus;
    private final String code;

    public RestException(
            int httpStatus,
            String code,
            String message) {

        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}