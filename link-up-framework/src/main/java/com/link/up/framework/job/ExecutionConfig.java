package com.link.up.framework.job;

/**
 * Framework execution configuration. A negative optional limit means disabled.
 */
public final class ExecutionConfig {
    public static final int DEFAULT_BATCH_SIZE = 1_000;
    public static final int DEFAULT_SOURCE_PARALLELISM = 1;
    public static final int DEFAULT_SINK_PARALLELISM = 1;
    public static final int DEFAULT_CHANNEL_CAPACITY = 32;
    public static final int DEFAULT_PIPELINE_PARALLELISM = Math.max(1, Runtime.getRuntime().availableProcessors());

    private final int batchSize, sourceParallelism, sinkParallelism, pipelineParallelism, maxBufferedBatches;
    private final long maxBufferedRecords, maxBufferedBytes, maxRecordsPerSecond, maxBytesPerSecond;
    private final SinkPartitionStrategy sinkPartitionStrategy;
    private final SplitAssignmentMode splitAssignmentMode;

    public ExecutionConfig(int batchSize, int sourceParallelism, int sinkParallelism, int channelCapacity) {
        this(batchSize, sourceParallelism, sinkParallelism, DEFAULT_PIPELINE_PARALLELISM, channelCapacity, -1, -1, -1, -1,
                SinkPartitionStrategy.TABLE_AFFINITY, SplitAssignmentMode.STATIC_ROUND_ROBIN);
    }

    public ExecutionConfig(int batchSize, int sourceParallelism, int sinkParallelism, int channelCapacity,
                           SinkPartitionStrategy strategy) {
        this(batchSize, sourceParallelism, sinkParallelism, DEFAULT_PIPELINE_PARALLELISM, channelCapacity, -1, -1, -1, -1,
                strategy, SplitAssignmentMode.STATIC_ROUND_ROBIN);
    }

    public ExecutionConfig(int batchSize, int sourceParallelism, int sinkParallelism, int channelCapacity,
                           SinkPartitionStrategy strategy, SplitAssignmentMode mode) {
        this(batchSize, sourceParallelism, sinkParallelism, DEFAULT_PIPELINE_PARALLELISM, channelCapacity, -1, -1, -1, -1, strategy, mode);
    }

    public ExecutionConfig(int batchSize, int sourceParallelism, int sinkParallelism, int pipelineParallelism, int maxBufferedBatches,
                           long maxBufferedRecords, long maxBufferedBytes, long maxRecordsPerSecond, long maxBytesPerSecond,
                           SinkPartitionStrategy strategy, SplitAssignmentMode mode) {
        if (batchSize <= 0 || sourceParallelism <= 0 || sinkParallelism <= 0 || pipelineParallelism <= 0)
            throw new IllegalArgumentException("batch size and parallelism must be greater than 0");
        if (maxBufferedBatches <= 0 && maxBufferedRecords <= 0 && maxBufferedBytes <= 0)
            throw new IllegalArgumentException("at least one buffer limit must be greater than 0");
        validateOptional(maxBufferedRecords, "maxBufferedRecords");
        validateOptional(maxBufferedBytes, "maxBufferedBytes");
        validateOptional(maxRecordsPerSecond, "maxRecordsPerSecond");
        validateOptional(maxBytesPerSecond, "maxBytesPerSecond");
        this.batchSize = batchSize;
        this.sourceParallelism = sourceParallelism;
        this.sinkParallelism = sinkParallelism;
        this.pipelineParallelism = pipelineParallelism;
        this.maxBufferedBatches = maxBufferedBatches;
        this.maxBufferedRecords = maxBufferedRecords;
        this.maxBufferedBytes = maxBufferedBytes;
        this.maxRecordsPerSecond = maxRecordsPerSecond;
        this.maxBytesPerSecond = maxBytesPerSecond;
        this.sinkPartitionStrategy = strategy == null ? SinkPartitionStrategy.TABLE_AFFINITY : strategy;
        this.splitAssignmentMode = mode == null ? SplitAssignmentMode.STATIC_ROUND_ROBIN : mode;
    }

    private static void validateOptional(long value, String name) {
        if (value == 0 || value < -1) throw new IllegalArgumentException(name + " must be positive or -1");
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getSourceParallelism() {
        return sourceParallelism;
    }

    public int getSinkParallelism() {
        return sinkParallelism;
    }

    public int getPipelineParallelism() {
        return pipelineParallelism;
    }

    /**
     * Compatibility accessor for the historical channel-capacity setting.
     */
    public int getChannelCapacity() {
        return maxBufferedBatches;
    }

    public int getMaxBufferedBatches() {
        return maxBufferedBatches;
    }

    public long getMaxBufferedRecords() {
        return maxBufferedRecords;
    }

    public long getMaxBufferedBytes() {
        return maxBufferedBytes;
    }

    public long getMaxRecordsPerSecond() {
        return maxRecordsPerSecond;
    }

    public long getMaxBytesPerSecond() {
        return maxBytesPerSecond;
    }

    public SplitAssignmentMode getSplitAssignmentMode() {
        return splitAssignmentMode;
    }

    public SinkPartitionStrategy getSinkPartitionStrategy() {
        return sinkPartitionStrategy;
    }
}
