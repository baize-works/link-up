package com.link.up.api.source;

import com.link.up.api.table.type.FluxRow;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertTrue;

public class RecordSizeEstimatorTest {

    @Test
    public void estimatesSupportedFluxRowValuesAndUnknownValues() {
        FluxRow row = FluxRow.of(null, 42, true, "text", new byte[12],
                new BigDecimal("12.34"), new Date(), Instant.now(), new Object());

        assertTrue(row.estimatedSizeBytes() > RecordSizeEstimator.UNKNOWN_RECORD_BYTES);
        assertTrue(RecordSizeEstimator.estimateObjectSizeBytes(new Object())
                >= RecordSizeEstimator.UNKNOWN_RECORD_BYTES);
    }

    @Test
    public void batchExposesEstimatedBytes() {
        RecordBatch<FluxRow> batch = RecordBatch.of(new TestSplit(),
                Arrays.asList(FluxRow.of("one"), FluxRow.of("two")));

        assertTrue(batch.getEstimatedBytes() > 0L);
        assertTrue(batch.estimatedSizeBytes(RecordSizeEstimator.fluxRowEstimator()) > 0L);
    }

    private static final class TestSplit implements SourceSplit {
        @Override
        public String splitId() {
            return "split";
        }
    }
}
