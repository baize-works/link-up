package com.link.up.server.runtime;

import com.link.up.framework.execution.JobExecutionListener;
import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.JobResult;

/**
 * 将 JobManager 与具体执行引擎解耦，便于单元测试。
 */
public interface JobExecutor {

    JobResult execute(
            JobDefinition definition,
            JobExecutionListener listener)
            throws Exception;
}