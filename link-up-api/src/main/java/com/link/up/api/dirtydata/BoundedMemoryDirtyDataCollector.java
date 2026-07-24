package com.link.up.api.dirtydata;

import java.io.IOException;
import java.util.*;

public class BoundedMemoryDirtyDataCollector implements DirtyDataCollector {
    protected final int maxSamples;
    protected final long maxCount;
    protected final double maxPercentage;
    protected final String taskId;
    protected final List<DirtyRecord> samples = new ArrayList<DirtyRecord>();
    protected long attempted, dirty;

    public BoundedMemoryDirtyDataCollector(String taskId, int maxSamples, long maxCount, double maxPercentage) {
        if (maxSamples < 0 || maxCount < 0 || maxPercentage < 0 || maxPercentage > 1)
            throw new IllegalArgumentException("Invalid dirty-data limits");
        this.taskId = taskId;
        this.maxSamples = maxSamples;
        this.maxCount = maxCount;
        this.maxPercentage = maxPercentage;
    }

    public void open() throws IOException {
    }

    public void recordAttempt(long count) {
        attempted += count;
    }

    public void collect(DirtyRecord record) throws IOException {
        dirty++;
        if (samples.size() < maxSamples) samples.add(record);
    }

    public List<DirtyRecord> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    public DirtyDataSummary summary() {
        Map<String, Long> m = new LinkedHashMap<String, Long>();
        if (taskId != null) m.put(taskId, dirty);
        return new DirtyDataSummary(dirty, attempted, m, dirty > maxCount, attempted > 0 && ((double) dirty / attempted) > maxPercentage, samples.size(), null);
    }

    public void close(boolean successful) throws IOException {
    }
}
