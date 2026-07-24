package com.baize.flux.framework.execution;

import java.util.Objects;

/**
 * 生成文件系统安全的 Job 日志标识和文件名。
 */
public final class JobLogFileName {

    private JobLogFileName() {
    }

    public static String create(
            String jobName,
            long runId) {

        return "job-"
                + createJobId(jobName, runId)
                + ".log";
    }

    public static String createJobId(
            String jobName,
            long runId) {

        Objects.requireNonNull(
                jobName,
                "jobName must not be null");

        if (runId < 0L) {
            throw new IllegalArgumentException(
                    "runId must not be negative");
        }

        return sanitize(jobName)
                + "-"
                + runId;
    }

    private static String sanitize(String value) {
        String sanitized =
                value.trim()
                        .replaceAll(
                                "[^A-Za-z0-9._-]+",
                                "_");

        return sanitized.isEmpty()
                ? "unnamed"
                : sanitized;
    }
}
