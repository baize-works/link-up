package com.link.up.framework.metrics;

import com.link.up.framework.execution.TaskId;
import com.link.up.framework.execution.TaskState;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单个 Task 的运行指标。
 */
public final class TaskMetrics {

    private final TaskId taskId;

    private final AtomicReference<TaskState> state =
            new AtomicReference<TaskState>(
                    TaskState.CREATED);

    private final AtomicLong batchCount =
            new AtomicLong();
    private final AtomicLong receivedBatchCount = new AtomicLong();
    private final AtomicLong attemptedRecordCount = new AtomicLong();
    private final AtomicLong successfulBatchCount = new AtomicLong();
    private final AtomicLong failedBatchCount = new AtomicLong();
    private final AtomicLong unknownStateRecordCount = new AtomicLong();

    private final AtomicLong recordCount =
            new AtomicLong();

    private final AtomicLong sourceReadRecordCount = new AtomicLong();
    private final AtomicLong sinkWriteSuccessRecordCount = new AtomicLong();
    private final AtomicLong failedRecordCount = new AtomicLong();
    private final AtomicLong skippedRecordCount = new AtomicLong();
    private final AtomicLong sourceReadBytes = new AtomicLong();
    private final AtomicLong sinkWrittenBytes = new AtomicLong();
    private final AtomicLong batchRetryCount = new AtomicLong();
    private final AtomicLong databaseCommitNanos = new AtomicLong();
    private final AtomicLong sqlExecutionNanos = new AtomicLong();
    private final AtomicLong completedSplitCount = new AtomicLong();
    private final AtomicLong totalSplitCount = new AtomicLong();
    private final AtomicLong runningSplitCount = new AtomicLong();
    private final AtomicLong failedSplitCount = new AtomicLong();
    private final Set<String> completedSplits = ConcurrentHashMap.newKeySet();

    private volatile String currentTable;
    private volatile String currentSplit;
    private volatile long expectedRecordCount = -1L;

    private volatile long startTimeMillis;

    private volatile long endTimeMillis;

    public TaskMetrics(TaskId taskId) {
        this.taskId =
                Objects.requireNonNull(
                        taskId,
                        "taskId must not be null");
    }

    private static void addPositive(AtomicLong counter, long count) {
        if (count > 0) counter.addAndGet(count);
    }

    public void markStarted() {
        state.set(TaskState.RUNNING);
        startTimeMillis = System.currentTimeMillis();
    }

    public void markFinished(TaskState finalState) {
        state.set(finalState);
        endTimeMillis = System.currentTimeMillis();
    }

    public void incrementBatchCount() {
        batchCount.incrementAndGet();
    }

    public void incrementReceivedBatchCount() {
        receivedBatchCount.incrementAndGet();
    }

    public void addAttemptedRecords(long count) {
        addPositive(attemptedRecordCount, count);
    }

    public void incrementSuccessfulBatchCount() {
        successfulBatchCount.incrementAndGet();
    }

    public void incrementFailedBatchCount() {
        failedBatchCount.incrementAndGet();
    }

    public void addUnknownStateRecords(long count) {
        addPositive(unknownStateRecordCount, count);
    }

    public void addRecordCount(long count) {
        if (count > 0) {
            recordCount.addAndGet(count);
        }
    }

    public void addSourceReadRecords(long count) {
        addPositive(sourceReadRecordCount, count);
        addRecordCount(count);
    }

    public void addSinkWriteSuccessRecords(long count) {
        addPositive(sinkWriteSuccessRecordCount, count);
        addRecordCount(count);
    }

    public void addFailedRecords(long count) {
        addPositive(failedRecordCount, count);
    }

    public void addSkippedRecords(long count) {
        addPositive(skippedRecordCount, count);
    }

    public void addSourceReadBytes(long count) {
        addPositive(sourceReadBytes, count);
    }

    public void addSinkWrittenBytes(long count) {
        addPositive(sinkWrittenBytes, count);
    }

    public void incrementBatchRetryCount() {
        batchRetryCount.incrementAndGet();
    }

    public void addDatabaseCommitNanos(long nanos) {
        addPositive(databaseCommitNanos, nanos);
    }

    public void addSqlExecutionNanos(long nanos) {
        addPositive(sqlExecutionNanos, nanos);
    }

    /**
     * Records the table and split that this task is currently processing.
     */
    public void setCurrentPosition(String table, String split) {
        currentTable = table;
        currentSplit = split;
    }

    public void markSplitRunning() {
        runningSplitCount.incrementAndGet();
    }

    public void markSplitFinished() {
        if (runningSplitCount.get() > 0L) runningSplitCount.decrementAndGet();
    }

    public void markSplitFailed() {
        markSplitFinished();
        failedSplitCount.incrementAndGet();
    }

    /**
     * Marks a split completed once; repeated notifications are intentionally idempotent.
     */
    public void markSplitCompleted(String splitId) {
        if (splitId != null && completedSplits.add(splitId)) {
            completedSplitCount.incrementAndGet();
        }
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public TaskState getState() {
        return state.get();
    }

    public long getBatchCount() {
        return batchCount.get();
    }

    public long getRecordCount() {
        return recordCount.get();
    }

    public long getReceivedBatchCount() {
        return receivedBatchCount.get();
    }

    public long getAttemptedRecordCount() {
        return attemptedRecordCount.get();
    }

    public long getSuccessfulBatchCount() {
        return successfulBatchCount.get();
    }

    public long getFailedBatchCount() {
        return failedBatchCount.get();
    }

    public long getUnknownStateRecordCount() {
        return unknownStateRecordCount.get();
    }

    public long getSourceReadRecordCount() {
        return sourceReadRecordCount.get();
    }

    public long getSinkWriteSuccessRecordCount() {
        return sinkWriteSuccessRecordCount.get();
    }

    public long getFailedRecordCount() {
        return failedRecordCount.get();
    }

    public long getSkippedRecordCount() {
        return skippedRecordCount.get();
    }

    public long getSourceReadBytes() {
        return sourceReadBytes.get();
    }

    public long getSinkWrittenBytes() {
        return sinkWrittenBytes.get();
    }

    public long getBatchRetryCount() {
        return batchRetryCount.get();
    }

    public long getDatabaseCommitMillis() {
        return databaseCommitNanos.get() / 1_000_000L;
    }

    public long getSqlExecutionMillis() {
        return sqlExecutionNanos.get() / 1_000_000L;
    }

    public String getCurrentTable() {
        return currentTable;
    }

    public String getCurrentSplit() {
        return currentSplit;
    }

    public long getCompletedSplitCount() {
        return completedSplitCount.get();
    }

    public long getTotalSplitCount() {
        return totalSplitCount.get();
    }

    public void setTotalSplitCount(long count) {
        totalSplitCount.set(Math.max(0L, count));
    }

    public long getPendingSplitCount() {
        return Math.max(0L, totalSplitCount.get() - runningSplitCount.get() - completedSplitCount.get() - failedSplitCount.get());
    }

    public long getRunningSplitCount() {
        return runningSplitCount.get();
    }

    public long getFailedSplitCount() {
        return failedSplitCount.get();
    }

    public long getExpectedRecordCount() {
        return expectedRecordCount;
    }

    /**
     * Sets the known total rows for ETA calculation, or a negative value when unknown.
     */
    public void setExpectedRecordCount(long count) {
        expectedRecordCount = count;
    }

    /**
     * Rows per second since start, using source rows when available and sink rows otherwise.
     */
    public double getAverageQps() {
        long duration = getDurationMillis();
        return duration == 0L ? 0D : getProgressRecordCount() * 1000D / duration;
    }

    /**
     * Current QPS is the live rate over the task's elapsed execution period.
     */
    public double getCurrentQps() {
        return getAverageQps();
    }

    /**
     * Returns -1 when the total work or a positive rate is not known yet.
     */
    public long getEstimatedRemainingMillis() {
        if (expectedRecordCount < 0L) return -1L;
        double qps = getAverageQps();
        if (qps <= 0D) return -1L;
        return Math.max(0L, Math.round((expectedRecordCount - getProgressRecordCount()) * 1000D / qps));
    }

    private long getProgressRecordCount() {
        long source = sourceReadRecordCount.get();
        return source > 0L ? source : sinkWriteSuccessRecordCount.get();
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public long getDurationMillis() {
        if (startTimeMillis <= 0) {
            return 0L;
        }

        long end =
                endTimeMillis > 0
                        ? endTimeMillis
                        : System.currentTimeMillis();

        return Math.max(0L, end - startTimeMillis);
    }
}
