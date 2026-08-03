package com.link.up.server;

import com.link.up.framework.connector.FactoryRegistry;
import com.link.up.framework.connector.schema.ConnectorSchemaCatalog;
import com.link.up.server.config.FluxServerConfig;
import com.link.up.server.http.JettyServer;
import com.link.up.server.runtime.InMemoryJobRepository;
import com.link.up.server.runtime.JobExecutor;
import com.link.up.server.runtime.JobIdGenerator;
import com.link.up.server.runtime.JobRepository;
import com.link.up.server.runtime.LocalJobExecutor;
import com.link.up.server.runtime.LocalJobIdGenerator;
import com.link.up.server.runtime.LocalJobManager;
import com.link.up.server.runtime.WorkerIdentity;
import com.link.up.server.service.ConnectorRestService;
import com.link.up.server.service.JobRestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Link-Up 单节点离线同步 Worker 启动入口。
 */
public final class FluxServer {

    private static final String LOG_FILE_PROPERTY =
            "link.up.log.file";

    static {
        configureDefaultLogFile();
    }

    private static final Logger LOG =
            LogManager.getLogger(
                    FluxServer.class);

    private FluxServer() {
    }

    public static void main(String[] args)
            throws Exception {

        final FluxServerConfig config =
                FluxServerConfig.fromArgs(
                        args);

        List<Path> pluginDirectories =
                config.getPluginDirectories();

        ClassLoader classLoader =
                Thread.currentThread()
                        .getContextClassLoader();

        Path[] pluginPaths =
                pluginDirectories.toArray(
                        new Path[pluginDirectories.size()]);

        JobExecutor jobExecutor =
                new LocalJobExecutor(
                        classLoader,
                        pluginPaths);

        JobRepository repository =
                new InMemoryJobRepository(
                        config.getHistoryLimit());

        JobIdGenerator jobIdGenerator =
                new LocalJobIdGenerator();

        final LocalJobManager manager =
                new LocalJobManager(
                        config.getJobThreads(),
                        config.getMaxQueuedJobs(),
                        config.getShutdownTimeoutMillis(),
                        jobExecutor,
                        repository,
                        jobIdGenerator);

        WorkerIdentity workerIdentity =
                new WorkerIdentity(
                        config.getNodeId(),
                        config.getNodeName(),
                        WorkerIdentity
                                .implementationVersion());

        JobRestService jobService =
                new JobRestService(
                        manager,
                        workerIdentity,
                        config.getJobThreads(),
                        config.getMaxQueuedJobs());

        final FactoryRegistry connectorRegistry =
                FactoryRegistry.discover(
                        classLoader,
                        pluginPaths);

        ConnectorSchemaCatalog connectorCatalog =
                ConnectorSchemaCatalog.fromRegistry(
                        connectorRegistry);

        ConnectorRestService connectorService =
                new ConnectorRestService(
                        connectorCatalog,
                        connectorRegistry);

        final JettyServer server =
                new JettyServer(
                        config,
                        jobService,
                        connectorService);

        final AtomicBoolean shutdown =
                new AtomicBoolean(false);

        final Runnable shutdownAction =
                new Runnable() {
                    public void run() {
                        if (!shutdown.compareAndSet(
                                false,
                                true)) {
                            return;
                        }

                        try {
                            server.stop();
                        } catch (Exception exception) {
                            LOG.warn(
                                    "Failed to stop HTTP server",
                                    exception);
                        }

                        manager.close();

                        try {
                            connectorRegistry.close();
                        } catch (RuntimeException exception) {
                            LOG.warn(
                                    "Failed to close connector registry",
                                    exception);
                        }
                    }
                };

        Thread shutdownHook =
                new Thread(
                        shutdownAction,
                        "link-up-shutdown");

        Runtime.getRuntime()
                .addShutdownHook(
                        shutdownHook);

        try {
            server.start();

            LOG.info(
                    "Link-Up Offline Worker started, nodeId={}, instanceId={}, host={}, port={}, jobThreads={}, connectorSchemas={}, pluginDirectories={}",
                    workerIdentity.getNodeId(),
                    workerIdentity.getInstanceId(),
                    config.getHost(),
                    server.getLocalPort(),
                    config.getJobThreads(),
                    connectorCatalog.list().size(),
                    config.getPluginDirectories());

            server.join();

        } finally {
            shutdownAction.run();

            try {
                Runtime.getRuntime()
                        .removeShutdownHook(
                                shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM 已经进入关闭流程。
            }
        }
    }

    private static void configureDefaultLogFile() {
        if (hasText(
                System.getProperty(
                        LOG_FILE_PROPERTY))
                || hasText(
                System.getenv(
                        "LOGFILE"))) {
            return;
        }

        String logDirectory =
                System.getProperty(
                        "link.up.log.dir");

        if (!hasText(logDirectory)) {
            logDirectory =
                    System.getenv(
                            "LINK_UP_LOG_DIR");
        }

        if (!hasText(logDirectory)) {
            logDirectory = "logs";
        }

        System.setProperty(
                LOG_FILE_PROPERTY,
                Paths.get(
                                logDirectory,
                                "link-up-server.log")
                        .toString());
    }

    private static boolean hasText(
            String value) {

        return value != null
                && !value.trim()
                .isEmpty();
    }
}
