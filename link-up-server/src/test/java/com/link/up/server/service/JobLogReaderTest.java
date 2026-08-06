package com.link.up.server.service;

import com.link.up.api.sink.TableDdl;
import com.link.up.server.dto.JobLogPageResponse;
import com.link.up.server.runtime.JobExecutionMetadata;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobLogReaderTest {

    @Test
    public void shouldReadMultilineUtf8LogsWithCursor() throws Exception {
        Path directory =
                Files.createTempDirectory(
                        "link-up-job-log-test");

        String previous =
                System.getProperty(
                        "link.up.job.log.dir");

        System.setProperty(
                "link.up.job.log.dir",
                directory.toString());

        try {
            String fileName = "job-orders-1.log";
            String content =
                    "2026-08-06 08:53:13.855 INFO  [link-up-job-1] [run=orders-1] c.l.u.f.e.JobExecution - Job started\n"
                            + "2026-08-06 08:53:13.900 INFO  [link-up-job-1] [run=orders-1] c.l.u.c.j.c.AbstractJdbcCatalog - 执行 Catalog SQL：CREATE TABLE `demo`.`orders` (\n"
                            + "    `id` BIGINT NOT NULL\n"
                            + ");\n"
                            + "2026-08-06 08:53:19.021 INFO  [link-up-job-1] [run=orders-1] c.l.u.f.e.JobExecution - Job finished: status=SUCCEEDED\n";

            Files.write(
                    directory.resolve(fileName),
                    content.getBytes(
                            StandardCharsets.UTF_8));

            JobExecutionMetadata metadata =
                    new JobExecutionMetadata(
                            "yak-offline-1",
                            "idempotency-1",
                            1,
                            "digest",
                            1L,
                            2L,
                            3L,
                            false,
                            Collections.emptyList(),
                            Collections.<String, TableDdl>emptyMap(),
                            "orders-1",
                            fileName);

            JobLogReader reader =
                    new JobLogReader();

            JobLogPageResponse first =
                    reader.read(
                            "flux-1",
                            metadata,
                            true,
                            0L,
                            2);

            assertEquals(2, first.getItems().size());
            assertEquals(
                    "yak-offline-1",
                    first.getExternalExecutionId());
            assertEquals(
                    "orders-1",
                    first.getRunId());
            assertTrue(
                    first.getItems().get(1)
                            .getMessage()
                            .contains("`id` BIGINT NOT NULL"));
            assertTrue(first.getNextCursor() > 0L);
            assertFalse(first.isCompleted());

            JobLogPageResponse second =
                    reader.read(
                            "flux-1",
                            metadata,
                            true,
                            first.getNextCursor(),
                            2);

            assertEquals(1, second.getItems().size());
            assertTrue(
                    second.getItems().get(0)
                            .getMessage()
                            .contains("SUCCEEDED"));
            assertTrue(second.isCompleted());
        } finally {
            if (previous == null) {
                System.clearProperty(
                        "link.up.job.log.dir");
            } else {
                System.setProperty(
                        "link.up.job.log.dir",
                        previous);
            }
        }
    }
}
