package com.link.up.server.registration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.link.up.api.connector.schema.ConnectorCapability;
import com.link.up.api.connector.schema.ConnectorSchema;
import com.link.up.framework.connector.schema.ConnectorSchemaCatalog;
import com.link.up.server.dto.WorkerNodeResponse;
import com.link.up.server.service.JobRestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Link-Up Worker 主动注册 Yak Ops、续租并在优雅关闭时注销。
 */
public final class ControlPlaneRegistrationAgent
        implements AutoCloseable {

    private static final Logger LOG =
            LogManager.getLogger(ControlPlaneRegistrationAgent.class);

    private static final String PROTOCOL_VERSION =
            "link-up-registration/v1";

    private final ControlPlaneRegistrationConfig config;
    private final JobRestService jobService;
    private final ConnectorSchemaCatalog connectorCatalog;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile String leaseId;
    private volatile String registeredInstanceId;
    private volatile long sequence;
    private volatile long heartbeatMillis;
    private volatile long failureDelayMillis = 1_000L;

    public ControlPlaneRegistrationAgent(
            ControlPlaneRegistrationConfig config,
            JobRestService jobService,
            ConnectorSchemaCatalog connectorCatalog) {

        this.config = config;
        this.jobService = jobService;
        this.connectorCatalog = connectorCatalog;
        this.heartbeatMillis = config.getHeartbeatMillis();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactory() {
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(
                                runnable,
                                "link-up-control-plane-registration");
                        thread.setDaemon(true);
                        return thread;
                    }
                });
    }

    public void start() {
        if (!config.isEnabled()) {
            LOG.info("Link-Up dynamic control-plane registration is disabled");
            return;
        }
        if (started.compareAndSet(false, true)) {
            schedule(0L);
        }
    }

    private void cycle() {
        if (closed.get()) {
            return;
        }
        long nextDelay;
        try {
            if (leaseId == null) {
                register();
            } else {
                heartbeat();
            }
            failureDelayMillis = 1_000L;
            nextDelay = heartbeatMillis;
        } catch (ControlPlaneException exception) {
            if (exception.requiresRegistration()) {
                leaseId = null;
                registeredInstanceId = null;
                sequence = 0L;
            }
            nextDelay = failureDelayMillis;
            failureDelayMillis = Math.min(60_000L, failureDelayMillis * 2L);
            LOG.warn(
                    "Link-Up dynamic registration failed, status={}, retryInMs={}, message={}",
                    exception.getStatusCode(),
                    nextDelay,
                    exception.getMessage());
        } catch (RuntimeException exception) {
            nextDelay = failureDelayMillis;
            failureDelayMillis = Math.min(60_000L, failureDelayMillis * 2L);
            LOG.warn(
                    "Link-Up dynamic registration failed, retryInMs={}",
                    nextDelay,
                    exception);
        }
        schedule(nextDelay);
    }

    private void register() {
        ObjectNode payload = runtimePayload();
        payload.put("protocolVersion", PROTOCOL_VERSION);
        ObjectNode labels = payload.putObject("labels");
        for (Map.Entry<String, String> entry : config.getLabels().entrySet()) {
            labels.put(entry.getKey(), entry.getValue());
        }
        JsonNode data = post(
                "/api/v1/offline/worker-registration/register",
                write(payload));
        acceptLease(data);
        LOG.info(
                "Link-Up Worker dynamically registered, nodeId={}, instanceId={}, heartbeatMs={}",
                payload.path("nodeId").asText(),
                registeredInstanceId,
                heartbeatMillis);
    }

    private void heartbeat() {
        WorkerNodeResponse node = jobService.node();
        if (!node.getInstanceId().equals(registeredInstanceId)) {
            leaseId = null;
            registeredInstanceId = null;
            sequence = 0L;
            register();
            return;
        }
        ObjectNode payload = runtimePayload();
        payload.put("protocolVersion", PROTOCOL_VERSION);
        payload.put("leaseId", leaseId);
        payload.put("sequence", ++sequence);
        JsonNode data = post(
                "/api/v1/offline/worker-registration/heartbeat",
                write(payload));
        acceptLease(data);
    }

    private ObjectNode runtimePayload() {
        WorkerNodeResponse node = jobService.node();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("nodeId", node.getNodeId());
        payload.put("nodeName", node.getNodeName());
        payload.put("instanceId", node.getInstanceId());
        payload.put("baseUrl", config.getAdvertisedBaseUrl());
        payload.put("engineVersion", node.getVersion());
        payload.put("startedAtMillis", node.getStartedAtMillis());
        payload.put("offlineOnly", node.isOfflineOnly());
        payload.put("maxConcurrentJobs", node.getMaxConcurrentJobs());
        payload.put("maxQueuedJobs", node.getMaxQueuedJobs());
        payload.put("runningJobs", node.getRunningJobs());
        payload.put("queuedJobs", node.getQueuedJobs());
        ArrayNode connectors = payload.putArray("connectors");
        for (ConnectorSchema schema : connectorCatalog.list()) {
            ObjectNode connector = mapper.createObjectNode();
            connector.put("connectorId", schema.getConnectorId());
            connector.put("role", schema.getRole().name());
            connector.put("schemaVersion", schema.getSchemaVersion());
            connector.put("schemaFingerprint", schema.getSchemaFingerprint());
            connector.put("implementationVersion", schema.getImplementationVersion());
            ArrayNode capabilities = connector.putArray("capabilities");
            for (ConnectorCapability capability : schema.getCapabilities()) {
                capabilities.add(capability.name());
            }
            connectors.add(connector);
        }
        return payload;
    }

    private void acceptLease(JsonNode response) {
        JsonNode data = response.has("data") ? response.path("data") : response;
        String receivedLease = data.path("leaseId").asText(null);
        String instanceId = data.path("instanceId").asText(null);
        if (receivedLease == null || receivedLease.trim().isEmpty()) {
            throw new ControlPlaneException(502, "Control plane response did not contain leaseId");
        }
        if (instanceId == null || instanceId.trim().isEmpty()) {
            throw new ControlPlaneException(502, "Control plane response did not contain instanceId");
        }
        leaseId = receivedLease;
        registeredInstanceId = instanceId;
        long suggested = data.path("heartbeatIntervalMillis")
                .asLong(config.getHeartbeatMillis());
        heartbeatMillis = Math.max(5_000L, Math.min(60_000L, suggested));
    }

    private JsonNode post(String endpointPath, String body) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(config.getControlPlaneUrl() + endpointPath);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(config.getConnectTimeoutMillis());
            connection.setReadTimeout(config.getRequestTimeoutMillis());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");

            long timestamp = System.currentTimeMillis();
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String signature = RegistrationRequestSigner.sign(
                    "POST",
                    url.getPath(),
                    timestamp,
                    nonce,
                    body,
                    config.getSecret());
            connection.setRequestProperty(
                    "X-Yak-Registration-Timestamp",
                    String.valueOf(timestamp));
            connection.setRequestProperty("X-Yak-Registration-Nonce", nonce);
            connection.setRequestProperty("X-Yak-Registration-Signature", signature);

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            OutputStream output = connection.getOutputStream();
            try {
                output.write(bytes);
            } finally {
                output.close();
            }

            int status = connection.getResponseCode();
            InputStream input = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = input == null ? "" : read(input);
            if (status < 200 || status >= 300) {
                throw new ControlPlaneException(status, concise(response));
            }
            return response.isEmpty()
                    ? mapper.createObjectNode()
                    : mapper.readTree(response);
        } catch (ControlPlaneException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ControlPlaneException(0, exception.getMessage(), exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String write(JsonNode payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not serialize dynamic registration payload",
                    exception);
        }
    }

    private String read(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }

    private String concise(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Control plane returned an empty error response";
        }
        String normalized = value.replaceAll(
                "(?i)(password|passwd|pwd|secret)\\s*[=:]\\s*[^,;\\s\"}]+",
                "$1=***");
        return normalized.length() <= 1000
                ? normalized
                : normalized.substring(0, 1000);
    }

    private void schedule(long delayMillis) {
        if (!closed.get()) {
            scheduler.schedule(
                    new Runnable() {
                        public void run() {
                            cycle();
                        }
                    },
                    Math.max(0L, delayMillis),
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void close() {
        if (!started.get() || closed.get()) {
            return;
        }
        try {
            if (leaseId != null && registeredInstanceId != null) {
                ObjectNode payload = mapper.createObjectNode();
                WorkerNodeResponse node = jobService.node();
                payload.put("protocolVersion", PROTOCOL_VERSION);
                payload.put("leaseId", leaseId);
                payload.put("nodeId", node.getNodeId());
                payload.put("instanceId", registeredInstanceId);
                payload.put("sequence", ++sequence);
                post(
                        "/api/v1/offline/worker-registration/deregister",
                        write(payload));
            }
        } catch (RuntimeException exception) {
            LOG.warn("Could not deregister Link-Up Worker during shutdown", exception);
        } finally {
            closed.set(true);
            scheduler.shutdownNow();
        }
    }

    static final class ControlPlaneException extends RuntimeException {
        private final int statusCode;

        ControlPlaneException(int statusCode, String message) {
            super(message == null ? "Control plane request failed" : message);
            this.statusCode = statusCode;
        }

        ControlPlaneException(int statusCode, String message, Throwable cause) {
            super(message == null ? "Control plane request failed" : message, cause);
            this.statusCode = statusCode;
        }

        int getStatusCode() {
            return statusCode;
        }

        boolean requiresRegistration() {
            return statusCode == 401 || statusCode == 404 || statusCode == 409;
        }
    }
}
