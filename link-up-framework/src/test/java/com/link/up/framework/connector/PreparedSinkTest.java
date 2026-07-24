package com.link.up.framework.connector;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.configuration.util.OptionRule;
import com.link.up.api.factory.SinkFactory;
import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.sink.SinkWriter;
import com.link.up.api.source.RecordBatch;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.type.FluxRow;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class PreparedSinkTest {

    @Test
    public void createsWriterOnlyAtTaskStartup() {
        AtomicInteger createCount = new AtomicInteger();
        SinkWriter<FluxRow> writer = new NoOpSinkWriter();

        PreparedSink preparedSink =
                new PreparedSink(
                        "test",
                        new TestingSinkFactory(createCount, writer),
                        ReadonlyConfig.fromMap(Collections.<String, Object>emptyMap()),
                        new PreparedSinkMetadata(Collections.emptyMap()));

        assertEquals(0, createCount.get());
        assertNotSame(null, preparedSink.createWriter());
        assertEquals(1, createCount.get());
    }

    private static final class TestingSinkFactory implements SinkFactory {

        private final AtomicInteger createCount;
        private final SinkWriter<FluxRow> writer;

        private TestingSinkFactory(
                AtomicInteger createCount,
                SinkWriter<FluxRow> writer) {
            this.createCount = createCount;
            this.writer = writer;
        }

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
            createCount.incrementAndGet();
            return writer;
        }
    }

    private static final class NoOpSinkWriter implements SinkWriter<FluxRow> {

        @Override
        public void write(RecordBatch<FluxRow> batch, CatalogTable sourceTable) {
        }

        @Override
        public void close() {
        }
    }
}
