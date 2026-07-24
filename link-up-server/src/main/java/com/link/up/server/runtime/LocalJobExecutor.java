package com.link.up.server.runtime;

import com.link.up.framework.execution.JobExecutionListener;
import com.link.up.framework.execution.LocalFluxEngine;
import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.JobResult;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * 每个 Job 创建一个独立 LocalFluxEngine，避免多个作业共享 Connector 资源。
 */
public final class LocalJobExecutor
        implements JobExecutor {

    private final ClassLoader classLoader;
    private final Path[] pluginDirectories;

    public LocalJobExecutor(
            ClassLoader classLoader,
            Path[] pluginDirectories) {

        this.classLoader =
                classLoader == null
                        ? Thread.currentThread()
                        .getContextClassLoader()
                        : classLoader;

        this.pluginDirectories =
                pluginDirectories == null
                        ? new Path[0]
                        : Arrays.copyOf(
                        pluginDirectories,
                        pluginDirectories.length);
    }

    public JobResult execute(
            JobDefinition definition,
            JobExecutionListener listener)
            throws Exception {

        LocalFluxEngine engine =
                pluginDirectories.length == 0
                        ? LocalFluxEngine.create(
                        classLoader)
                        : LocalFluxEngine.create(
                        classLoader,
                        pluginDirectories);

        try {
            return engine.execute(
                    definition,
                    listener);
        } finally {
            engine.close();
        }
    }
}