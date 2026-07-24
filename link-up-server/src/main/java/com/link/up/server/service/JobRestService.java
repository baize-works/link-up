package com.link.up.server.service;

import com.link.up.framework.job.JobConfigParser;
import com.link.up.framework.job.JobDefinition;
import com.link.up.server.dto.PageResponse;
import com.link.up.server.runtime.JobManager;
import com.link.up.server.runtime.JobSnapshot;
import com.link.up.server.runtime.ServerJobStatus;
import com.typesafe.config.ConfigException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * REST 输入与运行时领域服务之间的适配层。
 */
public final class JobRestService {

    private static final int MAX_PAGE_SIZE = 200;

    private final JobManager manager;
    private final JobConfigParser parser;

    public JobRestService(JobManager manager) {
        this.manager = manager;
        this.parser = new JobConfigParser();
    }

    public JobSnapshot.Summary submit(
            String hocon) {

        if (hocon == null
                || hocon.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Job configuration is empty");
        }

        JobDefinition definition;

        try {
            definition = parser.parse(hocon);
        } catch (ConfigException exception) {
            throw new IllegalArgumentException(
                    "Invalid HOCON job configuration");
        }

        return manager.submit(
                definition).toSummary();
    }

    public JobSnapshot job(String jobId) {
        return manager.getJob(jobId);
    }

    public List<JobSnapshot.Pipeline> pipelines(
            String jobId) {

        return manager.getJob(jobId)
                .getPipelines();
    }

    public List<JobSnapshot.Task> tasks(
            String jobId) {

        List<JobSnapshot.Task> tasks =
                new ArrayList<JobSnapshot.Task>();

        for (JobSnapshot.Pipeline pipeline :
                pipelines(jobId)) {

            tasks.addAll(
                    pipeline.getTasks());
        }

        return Collections.unmodifiableList(tasks);
    }

    public JobSnapshot.Metrics metrics(
            String jobId) {

        return manager.getJob(jobId)
                .getMetrics();
    }

    public PageResponse<JobSnapshot.Summary> jobs(
            String statusValue,
            int page,
            int pageSize) {

        if (page < 1) {
            throw new IllegalArgumentException(
                    "page must be greater than 0");
        }

        if (pageSize < 1
                || pageSize > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "pageSize must be between 1 and "
                            + MAX_PAGE_SIZE);
        }

        ServerJobStatus status =
                parseStatus(statusValue);

        List<JobSnapshot.Summary> filtered =
                new ArrayList<JobSnapshot.Summary>();

        for (JobSnapshot snapshot :
                manager.listJobs()) {

            if (status == null
                    || snapshot.getStatus() == status) {

                filtered.add(
                        snapshot.toSummary());
            }
        }

        long startLong =
                (long) (page - 1)
                        * pageSize;

        int from =
                startLong >= filtered.size()
                        ? filtered.size()
                        : (int) startLong;

        int to =
                Math.min(
                        filtered.size(),
                        from + pageSize);

        return new PageResponse<JobSnapshot.Summary>(
                filtered.subList(from, to),
                page,
                pageSize,
                filtered.size());
    }

    public JobSnapshot.Summary cancel(
            String jobId) {

        return manager.cancel(jobId)
                .toSummary();
    }

    private ServerJobStatus parseStatus(
            String statusValue) {

        if (statusValue == null
                || statusValue.trim().isEmpty()) {
            return null;
        }

        try {
            return ServerJobStatus.valueOf(
                    statusValue.trim()
                            .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown job status: "
                            + statusValue);
        }
    }
}