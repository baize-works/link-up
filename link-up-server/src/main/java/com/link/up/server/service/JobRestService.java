package com.link.up.server.service;

import com.link.up.framework.job.JobConfigParser;
import com.link.up.framework.job.JobDefinition;
import com.link.up.server.dto.JobResponse;
import com.link.up.server.dto.JobSubmitRequest;
import com.link.up.server.dto.PageResponse;
import com.link.up.server.dto.WorkerNodeResponse;
import com.link.up.server.runtime.JobExecutionMetadata;
import com.link.up.server.runtime.JobManager;
import com.link.up.server.runtime.JobSnapshot;
import com.link.up.server.runtime.JobSubmission;
import com.link.up.server.runtime.ServerJobStatus;
import com.link.up.server.runtime.WorkerIdentity;
import com.typesafe.config.ConfigException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * REST 输入与单节点离线 Worker 运行时之间的适配层。
 */
public final class JobRestService {

    private static final int MAX_PAGE_SIZE = 200;

    private final JobManager manager;
    private final JobConfigParser parser;
    private final WorkerIdentity workerIdentity;
    private final int maxConcurrentJobs;
    private final int maxQueuedJobs;

    public JobRestService(JobManager manager) {
        this(
                manager,
                new WorkerIdentity(
                        "embedded-link-up-node",
                        "Embedded Link-Up Offline Worker",
                        WorkerIdentity.implementationVersion()),
                1,
                1);
    }

    public JobRestService(
            JobManager manager,
            WorkerIdentity workerIdentity,
            int maxConcurrentJobs,
            int maxQueuedJobs) {

        this.manager = manager;
        this.parser = new JobConfigParser();
        this.workerIdentity = workerIdentity;
        this.maxConcurrentJobs = maxConcurrentJobs;
        this.maxQueuedJobs = maxQueuedJobs;
    }

    public JobSnapshot.Summary submit(String hocon) {
        return submitLegacyResponse(hocon)
                .toSummary();
    }

    public JobResponse submitLegacyResponse(String hocon) {
        String token = UUID.randomUUID().toString();

        JobSubmitRequest request = new JobSubmitRequest();
        request.setExternalExecutionId("legacy-" + token);
        request.setIdempotencyKey(token);
        request.setDefinitionVersion(1);
        request.setHocon(hocon);

        return submit(request);
    }

    public JobResponse submit(JobSubmitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Submit request must not be null");
        }

        String externalExecutionId =
                requireText(
                        request.getExternalExecutionId(),
                        "externalExecutionId");
        String idempotencyKey =
                requireText(
                        request.getIdempotencyKey(),
                        "idempotencyKey");
        Integer definitionVersion =
                request.getDefinitionVersion();

        if (definitionVersion == null || definitionVersion <= 0) {
            throw new IllegalArgumentException(
                    "definitionVersion must be greater than 0");
        }

        String hocon = requireText(request.getHocon(), "hocon");
        JobDefinition definition = parse(hocon);

        JobSubmission submission =
                new JobSubmission(
                        externalExecutionId,
                        idempotencyKey,
                        definitionVersion,
                        sha256(hocon),
                        definition);

        return response(manager.submit(submission));
    }

    public JobSnapshot job(String jobId) {
        return manager.getJob(jobId);
    }

    public JobResponse jobResponse(String jobId) {
        return response(manager.getJob(jobId));
    }

    public JobResponse jobByExternalExecutionId(
            String externalExecutionId) {

        return response(
                manager.getJobByExternalExecutionId(
                        externalExecutionId));
    }

    public List<JobSnapshot.Pipeline> pipelines(String jobId) {
        return manager.getJob(jobId).getPipelines();
    }

    public List<JobSnapshot.Task> tasks(String jobId) {
        List<JobSnapshot.Task> tasks =
                new ArrayList<JobSnapshot.Task>();

        for (JobSnapshot.Pipeline pipeline : pipelines(jobId)) {
            tasks.addAll(pipeline.getTasks());
        }

        return Collections.unmodifiableList(tasks);
    }

    public JobSnapshot.Metrics metrics(String jobId) {
        return manager.getJob(jobId).getMetrics();
    }

    public PageResponse<JobSnapshot.Summary> jobs(
            String statusValue,
            int page,
            int pageSize) {

        List<JobSnapshot.Summary> summaries =
                new ArrayList<JobSnapshot.Summary>();

        for (JobSnapshot snapshot : filtered(statusValue)) {
            summaries.add(snapshot.toSummary());
        }

        return page(summaries, page, pageSize);
    }

    public PageResponse<JobResponse> executionPage(
            String statusValue,
            int page,
            int pageSize) {

        List<JobResponse> responses =
                new ArrayList<JobResponse>();

        for (JobSnapshot snapshot : filtered(statusValue)) {
            responses.add(response(snapshot));
        }

        return page(responses, page, pageSize);
    }

    public JobSnapshot.Summary cancel(String jobId) {
        return manager.cancel(jobId).toSummary();
    }

    public JobResponse cancelResponse(String jobId) {
        return response(manager.cancel(jobId));
    }

    public WorkerNodeResponse node() {
        return new WorkerNodeResponse(
                workerIdentity,
                manager,
                maxConcurrentJobs,
                maxQueuedJobs);
    }

    private JobDefinition parse(String hocon) {
        try {
            return parser.parse(hocon);
        } catch (ConfigException exception) {
            throw new IllegalArgumentException(
                    "Invalid HOCON job configuration");
        }
    }

    private List<JobSnapshot> filtered(String statusValue) {
        ServerJobStatus status = parseStatus(statusValue);
        List<JobSnapshot> result =
                new ArrayList<JobSnapshot>();

        for (JobSnapshot snapshot : manager.listJobs()) {
            if (status == null || snapshot.getStatus() == status) {
                result.add(snapshot);
            }
        }

        return result;
    }

    private <T> PageResponse<T> page(
            List<T> values,
            int page,
            int pageSize) {

        validatePage(page, pageSize);

        long startLong = (long) (page - 1) * pageSize;
        int from =
                startLong >= values.size()
                        ? values.size()
                        : (int) startLong;
        int to = Math.min(values.size(), from + pageSize);

        return new PageResponse<T>(
                values.subList(from, to),
                page,
                pageSize,
                values.size());
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException(
                    "page must be greater than 0");
        }

        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "pageSize must be between 1 and "
                            + MAX_PAGE_SIZE);
        }
    }

    private JobResponse response(JobSnapshot snapshot) {
        JobExecutionMetadata metadata =
                manager.getMetadata(snapshot.getJobId());

        return new JobResponse(
                snapshot,
                metadata,
                workerIdentity);
    }

    private ServerJobStatus parseStatus(String statusValue) {
        if (statusValue == null || statusValue.trim().isEmpty()) {
            return null;
        }

        try {
            return ServerJobStatus.valueOf(
                    statusValue.trim()
                            .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown job status: " + statusValue);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    name + " must not be blank");
        }

        return value.trim();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] bytes =
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result =
                    new StringBuilder(bytes.length * 2);

            for (byte current : bytes) {
                result.append(
                        String.format(
                                Locale.ROOT,
                                "%02x",
                                current & 0xff));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception);
        }
    }
}
