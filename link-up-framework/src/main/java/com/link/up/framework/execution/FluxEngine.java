package com.link.up.framework.execution;

import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.JobResult;

/**
 * Flux 执行引擎。
 */
public interface FluxEngine extends AutoCloseable {

    JobResult execute(
            JobDefinition definition)
            throws Exception;

    @Override
    void close();
}