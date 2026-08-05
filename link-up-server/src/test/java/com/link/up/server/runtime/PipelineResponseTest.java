package com.link.up.server.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.up.api.sink.TableDdl;
import com.link.up.server.dto.PipelineResponse;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineResponseTest {

    @Test
    public void shouldSerializeTableDdlInsidePipeline() {
        JobSnapshot.Source source = new JobSnapshot.Source(
                "jdbc", "source_db.orders", 1,
                0L, 10L, 100L,
                1L, 1L, 0L, 10D);

        JobSnapshot.Sink sink = new JobSnapshot.Sink(
                "jdbc", "target_db.orders", 1,
                1L, 10L, 10L, 0L, 0L,
                100L, 10L, 10D);

        JobSnapshot.Commit commit = new JobSnapshot.Commit(
                1, 1, 1, 1, 0, 0,
                10L, 10L, 10L, 0L, 0L,
                false, false, "PIPELINE", "NONE");

        JobSnapshot.Pipeline pipeline = new JobSnapshot.Pipeline(
                "pipeline-orders",
                "source_db.orders",
                "SUCCEEDED",
                source,
                sink,
                commit,
                Collections.<JobSnapshot.Task>emptyList(),
                Collections.<JobSnapshot.Channel>emptyList(),
                null);

        TableDdl tableDdl = new TableDdl(
                "MYSQL",
                "source_db.orders",
                "target_db.orders",
                "CREATE TABLE `target_db`.`orders` (`id` BIGINT NOT NULL);",
                true,
                TableDdl.STATUS_SUCCEEDED,
                TableDdl.REASON_TARGET_TABLE_CREATED,
                8L,
                null,
                null);

        JsonNode json = new ObjectMapper().valueToTree(
                new PipelineResponse(pipeline, tableDdl));

        assertEquals("pipeline-orders", json.path("pipelineId").asText());
        assertEquals(
                "CREATE TABLE `target_db`.`orders` (`id` BIGINT NOT NULL);",
                json.path("tableDdl").path("createTableSql").asText());
        assertTrue(json.path("tableDdl").path("executed").asBoolean());
        assertEquals(
                TableDdl.REASON_TARGET_TABLE_CREATED,
                json.path("tableDdl").path("reason").asText());
    }
}
