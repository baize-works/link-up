package com.baize.flux.server;

import com.baize.flux.server.config.FluxServerConfig;
import com.baize.flux.server.http.JettyServer;
import com.baize.flux.server.runtime.InMemoryJobRepository;
import com.baize.flux.server.runtime.JobExecutor;
import com.baize.flux.server.runtime.JobIdGenerator;
import com.baize.flux.server.runtime.JobRepository;
import com.baize.flux.server.runtime.LocalJobExecutor;
import com.baize.flux.server.runtime.LocalJobIdGenerator;
import com.baize.flux.server.runtime.LocalJobManager;
import com.baize.flux.server.service.JobRestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Baize Flux REST Server 启动入口。
 */
public final class FluxServer {

    private static final String LOG_FILE_PROPERTY =
            "baize.flux.log.file";

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
                FluxServerConfig.fromArgs(args);

        List<Path> pluginDirectories =
                config.getPluginDirectories();

        JobExecutor jobExecutor =
                new LocalJobExecutor(
                        Thread.currentThread()
                                .getContextClassLoader(),
                        pluginDirectories.toArray(
                                new Path[
                                        pluginDirectories
                                                .size()]));

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

        JobRestService service =
                new JobRestService(manager);

        final JettyServer server =
                new JettyServer(
                        config,
                        service);

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
                    }
                };

        Thread shutdownHook =
                new Thread(
                        shutdownAction,
                        "baize-flux-shutdown");

        Runtime.getRuntime()
                .addShutdownHook(
                        shutdownHook);

        try {
            server.start();

            LOG.info(
                    "Baize Flux Server started, host={}, port={}, jobThreads={}, pluginDirectories={}",
                    config.getHost(),
                    server.getLocalPort(),
                    config.getJobThreads(),
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

    /**
     * IDEA 直接启动 Server 时也提供稳定的默认日志文件。
     *
     * <p>显式 JVM 参数或 LOGFILE 环境变量优先级更高。
     */
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
                        "baize.flux.log.dir");

        if (!hasText(logDirectory)) {
            logDirectory =
                    System.getenv(
                            "BAIZE_FLUX_LOG_DIR");
        }

        if (!hasText(logDirectory)) {
            logDirectory = "logs";
        }

        System.setProperty(
                LOG_FILE_PROPERTY,
                Paths.get(
                                logDirectory,
                                "baize-flux-server.log")
                        .toString());
    }

    private static boolean hasText(String value) {
        return value != null
                && !value.trim().isEmpty();
    }
}
