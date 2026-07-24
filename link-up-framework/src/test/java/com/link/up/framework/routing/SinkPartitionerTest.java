package com.link.up.framework.routing;

import com.link.up.api.source.RecordBatch;
import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.catalog.TableSchema;
import com.link.up.api.table.type.BasicType;
import com.link.up.framework.channel.RecordEnvelope;
import com.link.up.framework.job.SinkPartitionStrategy;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SinkPartitionerTest {
    private static RecordEnvelope<String> envelope(final String splitId) {
        TablePath path = TablePath.of("orders");
        CatalogTable table = CatalogTable.builder(path, TableSchema.builder()
                .column(Column.builder("id", BasicType.STRING_TYPE).build()).build()).build();
        SourceSplit split = new SourceSplit() {
            @Override
            public String splitId() {
                return splitId;
            }
        };
        return new RecordEnvelope<String>(path, table, RecordBatch.of(split, Collections.singletonList("row")));
    }

    @Test
    public void tableAffinityKeepsAllSplitsTogether() {
        SinkPartitioner<String> partitioner = new SinkPartitioner<String>(SinkPartitionStrategy.TABLE_AFFINITY);
        assertEquals(partitioner.selectChannel(envelope("a"), 17), partitioner.selectChannel(envelope("b"), 17));
    }

    @Test
    public void splitHashIsStableAndUsesSplit() {
        SinkPartitioner<String> partitioner = new SinkPartitioner<String>(SinkPartitionStrategy.SPLIT_HASH);
        int first = partitioner.selectChannel(envelope("a"), 97);
        assertEquals(first, partitioner.selectChannel(envelope("a"), 97));
        assertNotEquals(first, partitioner.selectChannel(envelope("b"), 97));
    }
}
