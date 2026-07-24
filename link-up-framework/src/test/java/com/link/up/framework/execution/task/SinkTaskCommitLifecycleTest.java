package com.link.up.framework.execution.task;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.configuration.util.OptionRule;
import com.link.up.api.factory.SinkFactory;
import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.sink.SinkWriter;
import com.link.up.api.source.RecordBatch;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.type.FluxRow;
import com.link.up.framework.channel.ChannelReader;
import com.link.up.framework.channel.InputGate;
import com.link.up.framework.channel.RecordEnvelope;
import com.link.up.framework.connector.PreparedSink;
import com.link.up.framework.execution.CancellationToken;
import com.link.up.framework.execution.TaskContext;
import com.link.up.framework.execution.TaskId;
import com.link.up.framework.metrics.TaskMetrics;
import com.link.up.framework.planner.SinkTaskPlan;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Regression coverage for task-local sink commit and cleanup boundaries.
 */
public class SinkTaskCommitLifecycleTest {

    private static SinkTask task(final RecordingWriter writer, ChannelReader<RecordEnvelope<FluxRow>> reader) {
        SinkFactory factory = new SinkFactory() {
            @Override
            public String factoryIdentifier() {
                return "test";
            }

            @Override
            public OptionRule optionRule() {
                return OptionRule.builder().build();
            }

            @Override
            public SinkWriter<FluxRow> createSink(ReadonlyConfig config) {
                return writer;
            }
        };
        PreparedSink sink = new PreparedSink("test", factory,
                ReadonlyConfig.fromMap(Collections.<String, Object>emptyMap()),
                new PreparedSinkMetadata(Collections.emptyMap()));
        TaskId id = new TaskId("sink", 0, 1);
        return new SinkTask(new SinkTaskPlan(id, sink), new InputGate<RecordEnvelope<FluxRow>>(reader));
    }

    private static TaskContext context() {
        TaskId id = new TaskId("sink", 0, 1);
        return new TaskContext(id, new CancellationToken(), new TaskMetrics(id),
                SinkTaskCommitLifecycleTest.class.getClassLoader());
    }

    @Test
    public void commitsOneTaskAfterPrepare() throws Exception {
        RecordingWriter writer = new RecordingWriter();
        SinkTask task = task(writer, new EndReader());
        task.execute(context());
        assertEquals(1, writer.prepareCount);
        assertEquals(1, writer.commitCount);
        assertTrue(task.isCommitted());
    }

    @Test
    public void readFailureAbortsWithoutCommitting() throws Exception {
        RecordingWriter writer = new RecordingWriter();
        SinkTask task = task(writer, new FailingReader());
        try {
            task.execute(context());
            fail("expected failure");
        } catch (IllegalStateException expected) {
            assertEquals("write failed", expected.getMessage());
        }
        assertEquals(1, writer.abortCount);
        assertEquals(0, writer.commitCount);
        assertFalse(task.isCommitted());
    }

    @Test
    public void closeFailureAfterCommitDoesNotMakeTaskRetryable() throws Exception {
        RecordingWriter writer = new RecordingWriter();
        writer.closeFailure = true;
        SinkTask task = task(writer, new EndReader());
        task.execute(context());
        assertTrue(task.isCommitted());
        assertEquals(1, writer.commitCount);
    }

    @Test
    public void closeFailureDoesNotHideOriginalFailure() throws Exception {
        RecordingWriter writer = new RecordingWriter();
        writer.closeFailure = true;
        try {
            task(writer, new FailingReader()).execute(context());
            fail("expected failure");
        } catch (IllegalStateException expected) {
            assertEquals("write failed", expected.getMessage());
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("close failed", expected.getSuppressed()[0].getMessage());
        }
    }

    private static final class EndReader implements ChannelReader<RecordEnvelope<FluxRow>> {
        @Override
        public RecordEnvelope<FluxRow> read() {
            return null;
        }
    }

    private static final class FailingReader implements ChannelReader<RecordEnvelope<FluxRow>> {
        @Override
        public RecordEnvelope<FluxRow> read() {
            throw new IllegalStateException("write failed");
        }
    }

    private static final class RecordingWriter implements SinkWriter<FluxRow> {
        int prepareCount;
        int commitCount;
        int abortCount;
        boolean closeFailure;

        @Override
        public void write(RecordBatch<FluxRow> batch, CatalogTable table) {
        }

        @Override
        public void prepareCommit() {
            prepareCount++;
        }

        @Override
        public void commit() {
            commitCount++;
        }

        @Override
        public void abort() {
            abortCount++;
        }

        @Override
        public void close() {
            if (closeFailure) throw new IllegalStateException("close failed");
        }
    }
}
