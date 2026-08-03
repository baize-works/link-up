package com.link.up.server.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Connector 预检请求，只携带实际运行 options。 */
public final class ConnectorPreflightRequest {

    private Map<String, Object> options;

    public ConnectorPreflightRequest() {
    }

    public Map<String, Object> getOptions() {
        return options == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(
                        new LinkedHashMap<String, Object>(
                                options));
    }

    public void setOptions(
            Map<String, Object> options) {

        this.options =
                options == null
                        ? Collections.<String, Object>emptyMap()
                        : new LinkedHashMap<String, Object>(
                                options);
    }
}
