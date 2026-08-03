package com.link.up.server.registration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Worker 主动注册控制面配置。
 *
 * <p>优先读取 JVM 参数，其次读取环境变量。默认关闭，避免未配置密钥时暴露注册行为。
 */
public final class ControlPlaneRegistrationConfig {

    private final boolean enabled;
    private final String controlPlaneUrl;
    private final String secret;
    private final String advertisedBaseUrl;
    private final long heartbeatMillis;
    private final int connectTimeoutMillis;
    private final int requestTimeoutMillis;
    private final Map<String, String> labels;

    private ControlPlaneRegistrationConfig(
            boolean enabled,
            String controlPlaneUrl,
            String secret,
            String advertisedBaseUrl,
            long heartbeatMillis,
            int connectTimeoutMillis,
            int requestTimeoutMillis,
            Map<String, String> labels) {

        this.enabled = enabled;
        this.controlPlaneUrl = normalizeUrl(controlPlaneUrl);
        this.secret = normalizeText(secret);
        this.advertisedBaseUrl = normalizeUrl(advertisedBaseUrl);
        this.heartbeatMillis = Math.max(5_000L, heartbeatMillis);
        this.connectTimeoutMillis = Math.max(1_000, connectTimeoutMillis);
        this.requestTimeoutMillis = Math.max(1_000, requestTimeoutMillis);
        this.labels = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(labels));

        if (enabled) {
            requireUrl(this.controlPlaneUrl, "controlPlaneUrl");
            requireUrl(this.advertisedBaseUrl, "advertisedBaseUrl");
            if (this.secret == null || this.secret.length() < 16) {
                throw new IllegalArgumentException(
                        "registration secret must contain at least 16 characters");
            }
        }
    }

    public static ControlPlaneRegistrationConfig load() {
        boolean enabled = booleanValue(
                value("link.up.registration.enabled", "LINK_UP_REGISTRATION_ENABLED"),
                false);
        String controlPlaneUrl = value(
                "link.up.registration.control-plane-url",
                "LINK_UP_CONTROL_PLANE_URL");
        String secret = value(
                "link.up.registration.secret",
                "LINK_UP_REGISTRATION_SECRET");
        String advertisedBaseUrl = value(
                "link.up.registration.advertised-base-url",
                "LINK_UP_ADVERTISED_BASE_URL");
        long heartbeatMillis = longValue(
                value("link.up.registration.heartbeat-millis",
                        "LINK_UP_REGISTRATION_HEARTBEAT_MILLIS"),
                20_000L);
        int connectTimeoutMillis = intValue(
                value("link.up.registration.connect-timeout-millis",
                        "LINK_UP_REGISTRATION_CONNECT_TIMEOUT_MILLIS"),
                5_000);
        int requestTimeoutMillis = intValue(
                value("link.up.registration.request-timeout-millis",
                        "LINK_UP_REGISTRATION_REQUEST_TIMEOUT_MILLIS"),
                10_000);
        Map<String, String> labels = labels(value(
                "link.up.registration.labels",
                "LINK_UP_REGISTRATION_LABELS"));
        return new ControlPlaneRegistrationConfig(
                enabled,
                controlPlaneUrl,
                secret,
                advertisedBaseUrl,
                heartbeatMillis,
                connectTimeoutMillis,
                requestTimeoutMillis,
                labels);
    }

    private static String value(String property, String environment) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(environment);
        }
        return value;
    }

    private static Map<String, String> labels(String value) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (value == null || value.trim().isEmpty()) {
            return result;
        }
        String[] entries = value.split(",");
        for (String entry : entries) {
            int separator = entry.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException(
                        "registration labels must use key=value syntax: " + entry);
            }
            String key = entry.substring(0, separator).trim();
            String itemValue = entry.substring(separator + 1).trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("registration label key must not be blank");
            }
            result.put(key, itemValue);
        }
        return result;
    }

    private static void requireUrl(String value, String name) {
        if (value == null
                || (!value.startsWith("http://") && !value.startsWith("https://"))) {
            throw new IllegalArgumentException(
                    name + " must start with http:// or https://");
        }
    }

    private static boolean booleanValue(String value, boolean fallback) {
        return value == null || value.trim().isEmpty()
                ? fallback
                : Boolean.parseBoolean(value.trim());
    }

    private static long longValue(String value, long fallback) {
        try {
            return value == null || value.trim().isEmpty()
                    ? fallback
                    : Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("registration long value is invalid: " + value);
        }
    }

    private static int intValue(String value, int fallback) {
        try {
            return value == null || value.trim().isEmpty()
                    ? fallback
                    : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("registration integer value is invalid: " + value);
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeUrl(String value) {
        String normalized = normalizeText(value);
        while (normalized != null && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public boolean isEnabled() { return enabled; }
    public String getControlPlaneUrl() { return controlPlaneUrl; }
    public String getSecret() { return secret; }
    public String getAdvertisedBaseUrl() { return advertisedBaseUrl; }
    public long getHeartbeatMillis() { return heartbeatMillis; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public int getRequestTimeoutMillis() { return requestTimeoutMillis; }
    public Map<String, String> getLabels() { return labels; }
}
