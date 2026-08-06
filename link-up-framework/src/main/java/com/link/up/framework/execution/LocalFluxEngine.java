package com.link.up.framework.execution;

import com.link.up.framework.connector.ConnectorPreparer;
import com.link.up.framework.connector.FactoryRegistry;
import com.link.up.framework.connector.PreparedJob;
import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.JobResult;
import com.link.up.framework.planner.ExecutionPlan;
import com.link.up.framework.planner.JobPlanner;
import org.apache.logging.log4j.CloseableThreadContext;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 本地离线 Flux 执行引擎。
 */
public final class LocalFluxEngine
        implements FluxEngine {

    private final ClassLoader classLoader;

    private final ConnectorPreparer connectorPreparer;

    private final JobPlanner jobPlanner;

    private final FactoryRegistry registry;

    public LocalFluxEngine(
            ClassLoader classLoader,
            ConnectorPreparer connectorPreparer,
            JobPlanner jobPlanner) {
        this(classLoader, connectorPreparer, jobPlanner, null);
    }

    private LocalFluxEngine(
            ClassLoader classLoader,
            ConnectorPreparer connectorPreparer,
            JobPlanner jobPlanner,
            FactoryRegistry registry) {

        this.classLoader =
                Objects.requireNonNull(
                        classLoader,
                        "classLoader must not be null");

        this.connectorPreparer =
                Objects.requireNonNull(
                        connectorPreparer,
                        "connectorPreparer must not be null");

        this.jobPlanner =
                Objects.requireNonNull(
                        jobPlanner,
                        "jobPlanner must not be null");
        this.registry = registry;
    }

    public static LocalFluxEngine create(
            ClassLoader classLoader) {

        ClassLoader effectiveClassLoader =
                classLoader == null
                        ? Thread.currentThread()
                        .getContextClassLoader()
                        : classLoader;

        FactoryRegistry registry =
                FactoryRegistry.discover(
                        effectiveClassLoader);

        ConnectorPreparer preparer =
                new ConnectorPreparer(
                        registry,
                        effectiveClassLoader);

        JobPlanner planner =
                new JobPlanner();

        return new LocalFluxEngine(
                effectiveClassLoader,
                preparer,
                planner,
                registry);
    }

    public static LocalFluxEngine create(
            ClassLoader classLoader,
            Path... pluginDirectories) {

        ClassLoader effectiveClassLoader =
                classLoader == null
                        ? Thread.currentThread()
                        .getContextClassLoader()
                        : classLoader;

        FactoryRegistry registry =
                FactoryRegistry.discover(
                        effectiveClassLoader,
                        pluginDirectories);

        return new LocalFluxEngine(
                effectiveClassLoader,
                new ConnectorPreparer(
                        registry,
                        effectiveClassLoader),
                new JobPlanner(),
                registry);
    }

    @Override
    public JobResult execute(
            JobDefinition definition)
            throws Exception {

        return execute(definition, null);
    }

    /**
     * 为本地服务暴露正在执行的实例，以便取消请求能传递到引擎。
     */
    public JobResult execute(
            JobDefinition definition,
            JobExecutionListener listener)
            throws Exception {

        Objects.requireNonNull(
                definition,
                "definition must not be null");

        long startTimeMillis =
                System.currentTimeMillis();

        String runId =
                JobLogFileName.createJobId(
                        definition.getName(),
                        startTimeMillis);

        String jobLogFile =
                JobLogFileName.create(
                        definition.getName(),
                        startTimeMillis);

        try (CloseableThreadContext.Instance ignored =
                     CloseableThreadContext
                             .put("runId", runId)
                             .put("jobName", definition.getName())
                             .put("jobLogFile", jobLogFile)) {

            PreparedJob preparedJob =
                    connectorPreparer.prepare(definition);

            ExecutionPlan executionPlan =
                    jobPlanner.plan(preparedJob);

            JobExecution jobExecution =
                    new JobExecution(
                            executionPlan,
                            classLoader,
                            startTimeMillis,
                            runId,
                            jobLogFile);

            if (listener != null) {
                listener.onJobExecutionCreated(
                        jobExecution);
            }

            return jobExecution.execute();
        } finally {
            // Plugin loaders are job resources; no open jar remains after a job completes or fails.
            if (registry != null) {
                registry.close();
            }
        }
    }

    @Override
    public void close() {
        if (registry != null) {
            registry.close();
        }
        /*
         * 当前 Engine 不持有长生命周期线程池。
         * 后续支持多 Job 并发时，可在这里关闭资源。
         */
    }
}
