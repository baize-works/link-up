package com.link.up.framework.execution.source;

import com.link.up.api.source.SourceSplit;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SourceExecuteProcessorTest {

    @Test
    public void shouldRoundRobinSplitAssignmentsAcrossReaderTasks() {
        List<TestSplit> splits = Arrays.asList(
                new TestSplit("a"), new TestSplit("b"), new TestSplit("c"),
                new TestSplit("d"), new TestSplit("e"));

        List<List<TestSplit>> assignments =
                SourceExecuteProcessor.assignSplits(splits, 3);

        assertEquals(3, assignments.size());
        assertEquals(Arrays.asList(splits.get(0), splits.get(3)), assignments.get(0));
        assertEquals(Arrays.asList(splits.get(1), splits.get(4)), assignments.get(1));
        assertEquals(Arrays.asList(splits.get(2)), assignments.get(2));
    }

    @Test
    public void shouldNotCreateMoreReaderTasksThanSplits() {
        List<List<TestSplit>> assignments = SourceExecuteProcessor.assignSplits(
                Arrays.asList(new TestSplit("only")), 8);

        assertEquals(1, assignments.size());
        assertEquals(1, assignments.get(0).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNonPositiveParallelism() {
        SourceExecuteProcessor.assignSplits(
                Arrays.asList(new TestSplit("only")), 0);
    }

    private static final class TestSplit implements SourceSplit {
        private final String id;

        private TestSplit(String id) {
            this.id = id;
        }

        @Override
        public String splitId() {
            return id;
        }
    }
}
