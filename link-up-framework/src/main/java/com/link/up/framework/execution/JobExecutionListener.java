package com.link.up.framework.execution;

/**
 * 通知服务层本地 Job 日志和执行对象已经创建。
 */
public interface JobExecutionListener {

    /**
     * Connector 准备开始前发布日志身份，使准备失败日志也可被查询。
     */
    default void onJobLogCreated(
            String runId,
            String jobLogFile) {
        // Backward-compatible optional callback.
    }

    /**
     * 执行计划创建后发布可取消的 JobExecution。
     */
    void onJobExecutionCreated(
            JobExecution jobExecution);
}
