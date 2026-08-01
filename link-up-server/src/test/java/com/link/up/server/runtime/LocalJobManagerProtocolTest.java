package com.link.up.server.runtime;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.framework.execution.JobExecutionListener;
import com.link.up.framework.job.ExecutionConfig;
import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.JobResult;
import com.link.up.framework.job.JobStatus;
import com.link.up.framework.job.SinkDefinition;
import com.link.up.framework.job.SourceDefinition;
import com.link.up.framework.metrics.JobMetrics;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class LocalJobManagerProtocolTest {

    @Test
    public void shouldReturnSameJobForSameIdempotentSubmission()
            throws Exception {

        LocalJobManager manager = manager();

        try {
            JobSubmission submission =
                    submission(
                            "external-100",
                            "key-100",
                            "digest-100");

            JobSnapshot first = manager.submit(submission);
            waitForTerminal(manager, first.getJobId());
            JobSnapshot second = manager.submit(submission);

            assertEquals(
                    first.getJobId(),
                    second.getJobId());
            assertEquals(
                    first.getJobId(),
                    manager.getJobByExternalExecutionId(
                            "external-100")
                            .getJobId());
        } finally {
            manager.close();
        }
    }

    @Test(expected = JobSubmissionConflictException.class)
    public void shouldRejectReusedIdempotencyKeyWithDifferentContent()
            throws Exception {

        LocalJobManager manager = manager();

        try {
            JobSnapshot first =
                    manager.submit(
                            submission(
                                    "external-200",
                                    "key-200",
                                    "digest-a"));

            waitForTerminal(manager, first.getJobId());

            manager.submit(
                    submission(
                            "external-200",
                            "key-200",
                            "digest-b"));
        } finally {
            manager.close();
        }
    }

    private static LocalJobManager manager() {
        final AtomicInteger ids = new AtomicInteger();

        JobExecutor executor =
                new JobExecutor() {
                    public JobResult execute(
                            JobDefinition definition,
                            JobExecutionListener listener) {

                        long now = System.currentTimeMillis();

                        return new JobResult(
                                definition.getName(),
                                JobStatus.SUCCEEDED,
                                now,
                                now,
                                new JobMetrics(),
                                null);
                    }
                };

        JobIdGenerator idGenerator =
                new JobIdGenerator() {
                    public String nextId() {
                        return "job-" + ids.incrementAndGet();
                    }
                };

        return new LocalJobManager(
                1,
                4,
                1_000L,
                executor,
                new InMemoryJobRepository(20),
                idGenerator);
    }

    private static JobSubmission submission(
            String externalExecutionId,
            String idempotencyKey,
            String digest) {

        ReadonlyConfig options =
                ReadonlyConfig.fromMap(
                        Collections.<String, Object>emptyMap());

        JobDefinition definition =
                new JobDefinition(
                        "protocol-test",
                        new SourceDefinition("test-source", options),
                        new SinkDefinition("test-sink", options),
                        new ExecutionConfig(100, 1, 1, 1));

        return new JobSubmission(
                externalExecutionId,
                idempotencyKey,
                1,
                digest,
                definition);
    }

    private static JobSnapshot waitForTerminal(
            LocalJobManager manager,
            String jobId)
            throws InterruptedException {

        for (int attempt = 0; attempt < 100; attempt++) {
            JobSnapshot snapshot = manager.getJob(jobId);

            if (snapshot.getStatus().isTerminal()) {
                return snapshot;
            }

            Thread.sleep(10L);
        }

        throw new AssertionError(
                "Job did not reach terminal state");
    }
}
