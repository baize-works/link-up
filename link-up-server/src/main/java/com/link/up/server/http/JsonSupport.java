package com.link.up.server.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonSupport {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .setSerializationInclusion(
                            JsonInclude.Include.NON_NULL);

    private JsonSupport() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}