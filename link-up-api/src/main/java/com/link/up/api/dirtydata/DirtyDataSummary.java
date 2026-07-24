package com.link.up.api.dirtydata;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregate data exposed in job results. Percentage denominator is attempted writes.
 */
public final class DirtyDataSummary implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long dirtyCount, attemptedCount;
    private final Map<String, Long> taskCounts;
    private final boolean countThresholdExceeded, percentageThresholdExceeded;
    private final int sampleCount;
    private final String outputPath;

    public DirtyDataSummary(long dirty, long attempted, Map<String, Long> tasks, boolean countExceeded, boolean percentageExceeded, int samples, String output) {
        dirtyCount = dirty;
        attemptedCount = attempted;
        taskCounts = Collections.unmodifiableMap(new LinkedHashMap<String, Long>(tasks));
        countThresholdExceeded = countExceeded;
        percentageThresholdExceeded = percentageExceeded;
        sampleCount = samples;
        outputPath = output;
    }

    public static DirtyDataSummary empty() {
        return new DirtyDataSummary(0, 0, Collections.<String, Long>emptyMap(), false, false, 0, null);
    }

    public long getDirtyCount() {
        return dirtyCount;
    }

    public long getAttemptedCount() {
        return attemptedCount;
    }

    public Map<String, Long> getTaskCounts() {
        return taskCounts;
    }

    public boolean isCountThresholdExceeded() {
        return countThresholdExceeded;
    }

    public boolean isPercentageThresholdExceeded() {
        return percentageThresholdExceeded;
    }

    public boolean isThresholdExceeded() {
        return countThresholdExceeded || percentageThresholdExceeded;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public String getOutputPath() {
        return outputPath;
    }
}
