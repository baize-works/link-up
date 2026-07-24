package com.link.up.framework.execution;

/**
 * 在执行计划创建后通知调用方，避免服务层只能依赖线程中断取消作业。
 */
public interface JobExecutionListener {
    void onJobExecutionCreated(JobExecution jobExecution);
}
