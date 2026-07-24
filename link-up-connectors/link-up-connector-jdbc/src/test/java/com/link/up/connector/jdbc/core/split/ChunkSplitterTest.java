package com.link.up.connector.jdbc.core.split;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChunkSplitterTest {
    @Test
    public void fixedSplitterCreatesContiguousNumericRanges() {
        List<Chunk<BigDecimal>> chunks = new FixedChunkSplitter().split(new BigDecimal("0"), new BigDecimal("10"), 3);
        assertEquals(3, chunks.size());
        assertEquals(new BigDecimal("0"), chunks.get(0).getStart());
        assertEquals(chunks.get(0).getEnd(), chunks.get(1).getStart());
        assertEquals(new BigDecimal("10"), chunks.get(2).getEnd());
        assertTrue(chunks.get(2).isEndInclusive());
    }

    @Test
    public void asciiSplitterCreatesContiguousRanges() {
        List<Chunk<String>> chunks = new AsciiStringRangeSplitter().split("AA", "AZ", 4);
        assertEquals(4, chunks.size());
        assertEquals("AA", chunks.get(0).getStart());
        assertEquals(chunks.get(0).getEnd(), chunks.get(1).getStart());
        assertEquals("AZ", chunks.get(3).getEnd());
        assertTrue(chunks.get(3).isEndInclusive());
    }

    @Test
    public void asciiSplitterKeepsIntermediateBoundsPrintable() {
        List<Chunk<String>> chunks = new AsciiStringRangeSplitter().split("A~", "B~", 4);
        for (Chunk<String> chunk : chunks) {
            for (char c : (chunk.getStart() + chunk.getEnd()).toCharArray()) {
                assertTrue(c >= ' ' && c <= '~');
            }
        }
    }

    @Test
    public void dynamicSplitterCapsRequestedChunks() {
        List<Chunk<BigDecimal>> chunks = new DynamicChunkSplitter<BigDecimal>(new FixedChunkSplitter(), 2)
                .split(BigDecimal.ZERO, new BigDecimal("10"), 8);
        assertEquals(2, chunks.size());
    }

    @Test
    public void dynamicSplitterUsesParallelismAsTheHardCap() {
        assertEquals(4, DynamicChunkSplitter.effectiveChunkCount(10, 4));
        assertEquals(3, DynamicChunkSplitter.effectiveChunkCount(3, 4));
    }

    @Test
    public void hashPredicateRetainsConfiguredBounds() {
        assertEquals(
                "((MOD(CRC32(`id`), 4) = 0 AND `id` >= 'A''A' AND `id` <= 'Z') OR `id` IS NULL)",
                JdbcSplitPlanner.buildHashPredicate(
                        "`id`",
                        "MOD(CRC32(`id`), 4) = 0",
                        "A'A",
                        "Z",
                        true));
        assertEquals(
                "(MOD(CRC32(`id`), 4) = 1 AND `id` >= 'A' AND `id` <= 'Z')",
                JdbcSplitPlanner.buildHashPredicate(
                        "`id`",
                        "MOD(CRC32(`id`), 4) = 1",
                        "A",
                        "Z",
                        false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void dynamicSplitterRejectsInvalidParallelism() {
        DynamicChunkSplitter.effectiveChunkCount(1, 0);
    }
}
