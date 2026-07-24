package com.link.up.framework.metrics;

import com.link.up.framework.execution.TaskId;
import com.link.up.framework.execution.TaskType;
import com.link.up.framework.execution.split.SplitProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe job aggregation based on TaskType, not display strings.
 */
public final class JobMetrics {
    private final Map<TaskId, TaskMetrics> taskMetrics = new ConcurrentHashMap<TaskId, TaskMetrics>();
    private final List<SplitProvider<?>> splitProviders = new CopyOnWriteArrayList<SplitProvider<?>>();
    private final List<ChannelMetrics> channelMetrics = new CopyOnWriteArrayList<ChannelMetrics>();

    public TaskMetrics registerTask(TaskId id) {
        TaskMetrics n = new TaskMetrics(id), p = taskMetrics.putIfAbsent(id, n);
        return p == null ? n : p;
    }

    public void registerSplitProvider(SplitProvider<?> p) {
        if (p != null && !splitProviders.contains(p)) splitProviders.add(p);
    }

    public void registerChannel(ChannelMetrics m) {
        channelMetrics.add(m);
    }

    public Map<TaskId, TaskMetrics> getTaskMetrics() {
        return Collections.unmodifiableMap(new LinkedHashMap<TaskId, TaskMetrics>(taskMetrics));
    }

    public List<ChannelMetrics> getChannelMetrics() {
        return Collections.unmodifiableList(new ArrayList<ChannelMetrics>(channelMetrics));
    }

    private long sum(TaskType type, int metric) {
        long n = 0;
        for (TaskMetrics m : taskMetrics.values())
            if (type == null || m.getTaskId().getTaskType() == type) {
                switch (metric) {
                    case 0:
                        n += m.getSourceReadRecordCount();
                        break;
                    case 1:
                        n += m.getSinkWriteSuccessRecordCount();
                        break;
                    case 2:
                        n += m.getBatchCount();
                        break;
                    case 3:
                        n += m.getCompletedSplitCount();
                        break;
                    case 4:
                        n += m.getFailedRecordCount();
                        break;
                    case 5:
                        n += m.getSkippedRecordCount();
                        break;
                    case 6:
                        n += m.getSourceReadBytes();
                        break;
                    case 7:
                        n += m.getSinkWrittenBytes();
                        break;
                    case 8:
                        n += m.getAttemptedRecordCount();
                        break;
                    case 9:
                        n += m.getUnknownStateRecordCount();
                        break;
                    case 10:
                        n += m.getBatchRetryCount();
                        break;
                }
            }
        return n;
    }

    public long getSourceRecordCount() {
        return sum(TaskType.SOURCE, 0);
    }

    public long getSinkRecordCount() {
        return sum(TaskType.SINK, 1);
    }

    public long getSourceBatchCount() {
        return sum(TaskType.SOURCE, 2);
    }

    public long getSinkBatchCount() {
        return sum(TaskType.SINK, 2);
    }

    public long getSinkReceivedBatchCount() {
        long n = 0;
        for (TaskMetrics m : taskMetrics.values())
            if (m.getTaskId().getTaskType() == TaskType.SINK) n += m.getReceivedBatchCount();
        return n;
    }

    public long getSinkAttemptedRecordCount() {
        return sum(TaskType.SINK, 8);
    }

    public long getUnknownStateRecordCount() {
        return sum(TaskType.SINK, 9);
    }

    public long getSourceSinkRecordDifference() {
        return getSourceRecordCount() - getSinkRecordCount();
    }

    public long getFailedRecordCount() {
        return sum(null, 4);
    }

    public long getSkippedRecordCount() {
        return sum(null, 5);
    }

    public long getSourceReadBytes() {
        return sum(TaskType.SOURCE, 6);
    }

    public long getSinkWrittenBytes() {
        return sum(TaskType.SINK, 7);
    }

    public long getCompletedSplitCount() {
        return splitProviders.isEmpty() ? sum(TaskType.SOURCE, 3) : split(3);
    }

    private long split(int type) {
        long n = 0;
        for (SplitProvider<?> p : splitProviders) n += type == 3 ? p.getCompletedSplitCount() : 0;
        return n;
    }

    public long getTotalSplitCount() {
        return splitProviders.isEmpty() ? 0 : all(0);
    }

    public long getPendingSplitCount() {
        return splitProviders.isEmpty() ? 0 : all(1);
    }

    public long getRunningSplitCount() {
        return splitProviders.isEmpty() ? 0 : all(2);
    }

    public long getFailedSplitCount() {
        return splitProviders.isEmpty() ? 0 : all(4);
    }

    private long all(int t) {
        long n = 0;
        for (SplitProvider<?> p : splitProviders)
            n += t == 0 ? p.getTotalSplitCount() : t == 1 ? p.getPendingSplitCount() : t == 2 ? p.getRunningSplitCount() : p.getFailedSplitCount();
        return n;
    }

    public long getBatchRetryCount() {
        return sum(null, 10);
    }

    public long getDatabaseCommitMillis() {
        long n = 0;
        for (TaskMetrics m : taskMetrics.values())
            if (m.getTaskId().getTaskType() == TaskType.SINK) n += m.getDatabaseCommitMillis();
        return n;
    }

    public long getSqlExecutionMillis() {
        long n = 0;
        for (TaskMetrics m : taskMetrics.values()) n += m.getSqlExecutionMillis();
        return n;
    }

    private long duration(TaskType type) {
        long min = Long.MAX_VALUE, max = 0;
        for (TaskMetrics m : taskMetrics.values())
            if (m.getTaskId().getTaskType() == type && m.getStartTimeMillis() > 0) {
                min = Math.min(min, m.getStartTimeMillis());
                max = Math.max(max, m.getEndTimeMillis() > 0 ? m.getEndTimeMillis() : System.currentTimeMillis());
            }
        return min == Long.MAX_VALUE ? 0 : Math.max(0, max - min);
    }

    public double getSourceAverageQps() {
        long d = duration(TaskType.SOURCE);
        return d == 0 ? 0 : getSourceRecordCount() * 1000D / d;
    }

    public double getSinkAverageQps() {
        long d = duration(TaskType.SINK);
        return d == 0 ? 0 : getSinkRecordCount() * 1000D / d;
    }

    /**
     * @deprecated use source/sink-specific rates.
     */
    public double getAverageQps() {
        return getSourceAverageQps();
    }

    public double getCurrentQps() {
        return getSourceAverageQps();
    }

    public double getProducerBackpressureRatio() {
        return ratio(0);
    }

    public double getConsumerIdleRatio() {
        return ratio(1);
    }

    public double getRateLimitedRatio() {
        return ratio(2);
    }

    private double ratio(int t) {
        if (channelMetrics.isEmpty()) return 0;
        double n = 0;
        for (ChannelMetrics c : channelMetrics)
            n += t == 0 ? c.getProducerBackpressureRatio() : t == 1 ? c.getConsumerIdleRatio() : c.getRateLimitedRatio();
        return n / channelMetrics.size();
    }

    /**
     * @deprecated blocking combines incompatible wait causes.
     */
    @Deprecated
    public double getChannelBlockedRatio() {
        return getProducerBackpressureRatio();
    }
}
