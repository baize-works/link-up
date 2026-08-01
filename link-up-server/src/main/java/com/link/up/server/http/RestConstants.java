package com.link.up.server.http;

public final class RestConstants {

    public static final String API_PREFIX =
            "/api/v1";

    public static final String HEALTH =
            API_PREFIX + "/health";

    public static final String NODE =
            API_PREFIX + "/node";

    public static final String JOBS =
            API_PREFIX + "/jobs";

    private RestConstants() {
    }
}
