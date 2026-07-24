package com.link.up.framework.metrics;

import com.link.up.framework.execution.TaskId;
import com.link.up.framework.execution.TaskState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TaskMetricsTest {
    @Test
    public void recordsTransferCountersAndProgress() throws Exception {
        TaskMetrics metrics = new TaskMetrics(new TaskId("source", 0, 1));
        metrics.markStarted();
        metrics.addSourceReadRecords(12);
        metrics.addSourceReadBytes(1200);
        metrics.addFailedRecords(2);
        metrics.addSkippedRecords(1);
        metrics.incrementBatchRetryCount();
        metrics.addSqlExecutionNanos(2_000_000L);
        metrics.setCurrentPosition("orders", "orders-0");
        metrics.markSplitCompleted("orders-0");
        metrics.markSplitCompleted("orders-0");
        metrics.setExpectedRecordCount(24);
        Thread.sleep(2L);

        assertEquals(12L, metrics.getSourceReadRecordCount());
        assertEquals(1200L, metrics.getSourceReadBytes());
        assertEquals(2L, metrics.getFailedRecordCount());
        assertEquals(1L, metrics.getSkippedRecordCount());
        assertEquals(1L, metrics.getBatchRetryCount());
        assertEquals(2L, metrics.getSqlExecutionMillis());
        assertEquals("orders", metrics.getCurrentTable());
        assertEquals("orders-0", metrics.getCurrentSplit());
        assertEquals(1L, metrics.getCompletedSplitCount());
        assertTrue(metrics.getAverageQps() > 0D);
        assertTrue(metrics.getEstimatedRemainingMillis() >= 0L);
        metrics.markFinished(TaskState.FINISHED);
    }

    @Test
    public void aggregatesSourceSinkAndDifference() {
        JobMetrics job = new JobMetrics();
        TaskMetrics source = job.registerTask(new TaskId("source", 0, 1));
        TaskMetrics sink = job.registerTask(new TaskId("sink", 0, 1));
        source.addSourceReadRecords(10);
        source.addSourceReadBytes(100);
        source.markSplitCompleted("one");
        sink.addSinkWriteSuccessRecords(8);
        sink.addSinkWrittenBytes(80);
        sink.addDatabaseCommitNanos(3_000_000L);
        source.incrementBatchRetryCount();
        sink.incrementBatchRetryCount();
        sink.incrementBatchRetryCount();

        assertEquals(10L, job.getSourceRecordCount());
        assertEquals(8L, job.getSinkRecordCount());
        assertEquals(2L, job.getSourceSinkRecordDifference());
        assertEquals(100L, job.getSourceReadBytes());
        assertEquals(80L, job.getSinkWrittenBytes());
        assertEquals(1L, job.getCompletedSplitCount());
        assertEquals(3L, job.getDatabaseCommitMillis());
        assertEquals(3L, job.getBatchRetryCount());
    }
}
