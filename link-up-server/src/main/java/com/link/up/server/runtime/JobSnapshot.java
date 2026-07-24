package com.link.up.server.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * REST 层使用的不可变作业快照。
 *
 * <p>不得直接暴露 JobExecution、Throwable、Future、JobMetrics 等运行时对象。
 */
public final class JobSnapshot {

    private final String jobId;
    private final String jobName;
    private final ServerJobStatus status;

    private final long createTimeMillis;
    private final long startTimeMillis;
    private final long endTimeMillis;
    private final long durationMillis;

    private final Metrics metrics;
    private final Commit commitSummary;
    private final List<Pipeline> pipelines;

    private final String errorCode;
    private final String errorMessage;

    JobSnapshot(
            String jobId,
            String jobName,
            ServerJobStatus status,
            long createTimeMillis,
            long startTimeMillis,
            long endTimeMillis,
            long durationMillis,
            Metrics metrics,
            Commit commitSummary,
            List<Pipeline> pipelines,
            String errorCode,
            String errorMessage) {

        this.jobId = jobId;
        this.jobName = jobName;
        this.status = status;
        this.createTimeMillis = createTimeMillis;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.durationMillis = durationMillis;
        this.metrics = metrics;
        this.commitSummary = commitSummary;
        this.pipelines =
                Collections.unmodifiableList(
                        new ArrayList<Pipeline>(pipelines));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Summary toSummary() {
        return new Summary(
                jobId,
                jobName,
                status,
                createTimeMillis,
                startTimeMillis,
                endTimeMillis,
                durationMillis,
                pipelines.size(),
                metrics.getSourceRecordCount(),
                metrics.getSinkSuccessRecordCount(),
                errorMessage);
    }

    public String getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public ServerJobStatus getStatus() {
        return status;
    }

    public long getCreateTimeMillis() {
        return createTimeMillis;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public Commit getCommitSummary() {
        return commitSummary;
    }

    public List<Pipeline> getPipelines() {
        return pipelines;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 列表接口使用的轻量视图。
     */
    public static final class Summary {

        private final String jobId;
        private final String jobName;
        private final ServerJobStatus status;
        private final long createTimeMillis;
        private final long startTimeMillis;
        private final long endTimeMillis;
        private final long durationMillis;
        private final int pipelineCount;
        private final long sourceRecordCount;
        private final long sinkSuccessRecordCount;
        private final String errorMessage;

        Summary(
                String jobId,
                String jobName,
                ServerJobStatus status,
                long createTimeMillis,
                long startTimeMillis,
                long endTimeMillis,
                long durationMillis,
                int pipelineCount,
                long sourceRecordCount,
                long sinkSuccessRecordCount,
                String errorMessage) {

            this.jobId = jobId;
            this.jobName = jobName;
            this.status = status;
            this.createTimeMillis = createTimeMillis;
            this.startTimeMillis = startTimeMillis;
            this.endTimeMillis = endTimeMillis;
            this.durationMillis = durationMillis;
            this.pipelineCount = pipelineCount;
            this.sourceRecordCount = sourceRecordCount;
            this.sinkSuccessRecordCount =
                    sinkSuccessRecordCount;
            this.errorMessage = errorMessage;
        }

        public String getJobId() {
            return jobId;
        }

        public String getJobName() {
            return jobName;
        }

        public ServerJobStatus getStatus() {
            return status;
        }

        public long getCreateTimeMillis() {
            return createTimeMillis;
        }

        public long getStartTimeMillis() {
            return startTimeMillis;
        }

        public long getEndTimeMillis() {
            return endTimeMillis;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public int getPipelineCount() {
            return pipelineCount;
        }

        public long getSourceRecordCount() {
            return sourceRecordCount;
        }

        public long getSinkSuccessRecordCount() {
            return sinkSuccessRecordCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static final class Metrics {

        private final long sourceBatchCount;
        private final long sourceRecordCount;
        private final long sourceReadBytes;
        private final double sourceAverageQps;

        private final long sinkReceivedBatchCount;
        private final long sinkAttemptedRecordCount;
        private final long sinkSuccessRecordCount;
        private final long sinkWrittenBytes;
        private final double sinkAverageQps;

        private final long failedRecordCount;
        private final long skippedRecordCount;
        private final long unknownStateRecordCount;
        private final long batchRetryCount;

        private final long totalSplitCount;
        private final long completedSplitCount;
        private final long pendingSplitCount;
        private final long runningSplitCount;
        private final long failedSplitCount;

        private final long databaseCommitMillis;
        private final long sqlExecutionMillis;

        private final double producerBackpressureRatio;
        private final double consumerIdleRatio;
        private final double rateLimitedRatio;

        Metrics(
                long sourceBatchCount,
                long sourceRecordCount,
                long sourceReadBytes,
                double sourceAverageQps,
                long sinkReceivedBatchCount,
                long sinkAttemptedRecordCount,
                long sinkSuccessRecordCount,
                long sinkWrittenBytes,
                double sinkAverageQps,
                long failedRecordCount,
                long skippedRecordCount,
                long unknownStateRecordCount,
                long batchRetryCount,
                long totalSplitCount,
                long completedSplitCount,
                long pendingSplitCount,
                long runningSplitCount,
                long failedSplitCount,
                long databaseCommitMillis,
                long sqlExecutionMillis,
                double producerBackpressureRatio,
                double consumerIdleRatio,
                double rateLimitedRatio) {

            this.sourceBatchCount = sourceBatchCount;
            this.sourceRecordCount = sourceRecordCount;
            this.sourceReadBytes = sourceReadBytes;
            this.sourceAverageQps = sourceAverageQps;
            this.sinkReceivedBatchCount =
                    sinkReceivedBatchCount;
            this.sinkAttemptedRecordCount =
                    sinkAttemptedRecordCount;
            this.sinkSuccessRecordCount =
                    sinkSuccessRecordCount;
            this.sinkWrittenBytes = sinkWrittenBytes;
            this.sinkAverageQps = sinkAverageQps;
            this.failedRecordCount = failedRecordCount;
            this.skippedRecordCount = skippedRecordCount;
            this.unknownStateRecordCount =
                    unknownStateRecordCount;
            this.batchRetryCount = batchRetryCount;
            this.totalSplitCount = totalSplitCount;
            this.completedSplitCount = completedSplitCount;
            this.pendingSplitCount = pendingSplitCount;
            this.runningSplitCount = runningSplitCount;
            this.failedSplitCount = failedSplitCount;
            this.databaseCommitMillis =
                    databaseCommitMillis;
            this.sqlExecutionMillis = sqlExecutionMillis;
            this.producerBackpressureRatio =
                    producerBackpressureRatio;
            this.consumerIdleRatio = consumerIdleRatio;
            this.rateLimitedRatio = rateLimitedRatio;
        }

        public long getSourceBatchCount() {
            return sourceBatchCount;
        }

        public long getSourceRecordCount() {
            return sourceRecordCount;
        }

        public long getSourceReadBytes() {
            return sourceReadBytes;
        }

        public double getSourceAverageQps() {
            return sourceAverageQps;
        }

        public long getSinkReceivedBatchCount() {
            return sinkReceivedBatchCount;
        }

        public long getSinkAttemptedRecordCount() {
            return sinkAttemptedRecordCount;
        }

        public long getSinkSuccessRecordCount() {
            return sinkSuccessRecordCount;
        }

        public long getSinkWrittenBytes() {
            return sinkWrittenBytes;
        }

        public double getSinkAverageQps() {
            return sinkAverageQps;
        }

        public long getFailedRecordCount() {
            return failedRecordCount;
        }

        public long getSkippedRecordCount() {
            return skippedRecordCount;
        }

        public long getUnknownStateRecordCount() {
            return unknownStateRecordCount;
        }

        public long getBatchRetryCount() {
            return batchRetryCount;
        }

        public long getTotalSplitCount() {
            return totalSplitCount;
        }

        public long getCompletedSplitCount() {
            return completedSplitCount;
        }

        public long getPendingSplitCount() {
            return pendingSplitCount;
        }

        public long getRunningSplitCount() {
            return runningSplitCount;
        }

        public long getFailedSplitCount() {
            return failedSplitCount;
        }

        public long getDatabaseCommitMillis() {
            return databaseCommitMillis;
        }

        public long getSqlExecutionMillis() {
            return sqlExecutionMillis;
        }

        public double getProducerBackpressureRatio() {
            return producerBackpressureRatio;
        }

        public double getConsumerIdleRatio() {
            return consumerIdleRatio;
        }

        public double getRateLimitedRatio() {
            return rateLimitedRatio;
        }
    }

    public static final class Commit {

        private final int totalTaskCount;
        private final int finishedTaskCount;
        private final int committedTaskCount;
        private final int dataCommittedTaskCount;
        private final int emptyCommittedTaskCount;
        private final int failedOrUncommittedTaskCount;

        private final long attemptedRecordCount;
        private final long successfullyWrittenRecordCount;
        private final long successfullyCommittedRecordCount;
        private final long failedRecordCount;
        private final long unknownStateRecordCount;

        private final boolean partialTaskCommit;
        private final boolean partialDataCommit;
        private final String commitScope;
        private final String retryAdvice;

        Commit(
                int totalTaskCount,
                int finishedTaskCount,
                int committedTaskCount,
                int dataCommittedTaskCount,
                int emptyCommittedTaskCount,
                int failedOrUncommittedTaskCount,
                long attemptedRecordCount,
                long successfullyWrittenRecordCount,
                long successfullyCommittedRecordCount,
                long failedRecordCount,
                long unknownStateRecordCount,
                boolean partialTaskCommit,
                boolean partialDataCommit,
                String commitScope,
                String retryAdvice) {

            this.totalTaskCount = totalTaskCount;
            this.finishedTaskCount = finishedTaskCount;
            this.committedTaskCount = committedTaskCount;
            this.dataCommittedTaskCount =
                    dataCommittedTaskCount;
            this.emptyCommittedTaskCount =
                    emptyCommittedTaskCount;
            this.failedOrUncommittedTaskCount =
                    failedOrUncommittedTaskCount;
            this.attemptedRecordCount =
                    attemptedRecordCount;
            this.successfullyWrittenRecordCount =
                    successfullyWrittenRecordCount;
            this.successfullyCommittedRecordCount =
                    successfullyCommittedRecordCount;
            this.failedRecordCount = failedRecordCount;
            this.unknownStateRecordCount =
                    unknownStateRecordCount;
            this.partialTaskCommit = partialTaskCommit;
            this.partialDataCommit = partialDataCommit;
            this.commitScope = commitScope;
            this.retryAdvice = retryAdvice;
        }

        public int getTotalTaskCount() {
            return totalTaskCount;
        }

        public int getFinishedTaskCount() {
            return finishedTaskCount;
        }

        public int getCommittedTaskCount() {
            return committedTaskCount;
        }

        public int getDataCommittedTaskCount() {
            return dataCommittedTaskCount;
        }

        public int getEmptyCommittedTaskCount() {
            return emptyCommittedTaskCount;
        }

        public int getFailedOrUncommittedTaskCount() {
            return failedOrUncommittedTaskCount;
        }

        public long getAttemptedRecordCount() {
            return attemptedRecordCount;
        }

        public long getSuccessfullyWrittenRecordCount() {
            return successfullyWrittenRecordCount;
        }

        public long getSuccessfullyCommittedRecordCount() {
            return successfullyCommittedRecordCount;
        }

        public long getFailedRecordCount() {
            return failedRecordCount;
        }

        public long getUnknownStateRecordCount() {
            return unknownStateRecordCount;
        }

        public boolean isPartialTaskCommit() {
            return partialTaskCommit;
        }

        public boolean isPartialDataCommit() {
            return partialDataCommit;
        }

        public String getCommitScope() {
            return commitScope;
        }

        public String getRetryAdvice() {
            return retryAdvice;
        }
    }

    public static final class Pipeline {

        private final String pipelineId;
        private final String dataSetId;
        private final String status;

        private final Source source;
        private final Sink sink;
        private final Commit commitSummary;

        private final List<Task> tasks;
        private final List<Channel> channels;
        private final String errorMessage;

        Pipeline(
                String pipelineId,
                String dataSetId,
                String status,
                Source source,
                Sink sink,
                Commit commitSummary,
                List<Task> tasks,
                List<Channel> channels,
                String errorMessage) {

            this.pipelineId = pipelineId;
            this.dataSetId = dataSetId;
            this.status = status;
            this.source = source;
            this.sink = sink;
            this.commitSummary = commitSummary;
            this.tasks =
                    Collections.unmodifiableList(
                            new ArrayList<Task>(tasks));
            this.channels =
                    Collections.unmodifiableList(
                            new ArrayList<Channel>(channels));
            this.errorMessage = errorMessage;
        }

        public String getPipelineId() {
            return pipelineId;
        }

        public String getDataSetId() {
            return dataSetId;
        }

        public String getStatus() {
            return status;
        }

        public Source getSource() {
            return source;
        }

        public Sink getSink() {
            return sink;
        }

        public Commit getCommitSummary() {
            return commitSummary;
        }

        public List<Task> getTasks() {
            return tasks;
        }

        public List<Channel> getChannels() {
            return channels;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static final class Source {

        private final String connector;
        private final String table;
        private final int taskCount;
        private final long batchCount;
        private final long recordCount;
        private final long readBytes;
        private final long totalSplitCount;
        private final long completedSplitCount;
        private final long failedSplitCount;
        private final double averageQps;

        Source(
                String connector,
                String table,
                int taskCount,
                long batchCount,
                long recordCount,
                long readBytes,
                long totalSplitCount,
                long completedSplitCount,
                long failedSplitCount,
                double averageQps) {

            this.connector = connector;
            this.table = table;
            this.taskCount = taskCount;
            this.batchCount = batchCount;
            this.recordCount = recordCount;
            this.readBytes = readBytes;
            this.totalSplitCount = totalSplitCount;
            this.completedSplitCount =
                    completedSplitCount;
            this.failedSplitCount = failedSplitCount;
            this.averageQps = averageQps;
        }

        public String getConnector() {
            return connector;
        }

        public String getTable() {
            return table;
        }

        public int getTaskCount() {
            return taskCount;
        }

        public long getBatchCount() {
            return batchCount;
        }

        public long getRecordCount() {
            return recordCount;
        }

        public long getReadBytes() {
            return readBytes;
        }

        public long getTotalSplitCount() {
            return totalSplitCount;
        }

        public long getCompletedSplitCount() {
            return completedSplitCount;
        }

        public long getFailedSplitCount() {
            return failedSplitCount;
        }

        public double getAverageQps() {
            return averageQps;
        }
    }

    public static final class Sink {

        private final String connector;
        private final String table;
        private final int taskCount;
        private final long receivedBatchCount;
        private final long attemptedRecordCount;
        private final long successRecordCount;
        private final long failedRecordCount;
        private final long unknownStateRecordCount;
        private final long writtenBytes;
        private final long committedRecordCount;
        private final double averageQps;

        Sink(
                String connector,
                String table,
                int taskCount,
                long receivedBatchCount,
                long attemptedRecordCount,
                long successRecordCount,
                long failedRecordCount,
                long unknownStateRecordCount,
                long writtenBytes,
                long committedRecordCount,
                double averageQps) {

            this.connector = connector;
            this.table = table;
            this.taskCount = taskCount;
            this.receivedBatchCount =
                    receivedBatchCount;
            this.attemptedRecordCount =
                    attemptedRecordCount;
            this.successRecordCount = successRecordCount;
            this.failedRecordCount = failedRecordCount;
            this.unknownStateRecordCount =
                    unknownStateRecordCount;
            this.writtenBytes = writtenBytes;
            this.committedRecordCount =
                    committedRecordCount;
            this.averageQps = averageQps;
        }

        public String getConnector() {
            return connector;
        }

        public String getTable() {
            return table;
        }

        public int getTaskCount() {
            return taskCount;
        }

        public long getReceivedBatchCount() {
            return receivedBatchCount;
        }

        public long getAttemptedRecordCount() {
            return attemptedRecordCount;
        }

        public long getSuccessRecordCount() {
            return successRecordCount;
        }

        public long getFailedRecordCount() {
            return failedRecordCount;
        }

        public long getUnknownStateRecordCount() {
            return unknownStateRecordCount;
        }

        public long getWrittenBytes() {
            return writtenBytes;
        }

        public long getCommittedRecordCount() {
            return committedRecordCount;
        }

        public double getAverageQps() {
            return averageQps;
        }
    }

    public static final class Task {

        private final String taskId;
        private final String pipelineId;
        private final String taskType;
        private final String state;

        private final int subtaskIndex;
        private final int parallelism;

        private final long batchCount;
        private final long receivedBatchCount;
        private final long attemptedRecordCount;
        private final long recordCount;
        private final long failedRecordCount;
        private final long unknownStateRecordCount;
        private final long completedSplitCount;

        private final double averageQps;
        private final long durationMillis;

        private final String currentTable;
        private final String currentSplit;

        Task(
                String taskId,
                String pipelineId,
                String taskType,
                String state,
                int subtaskIndex,
                int parallelism,
                long batchCount,
                long receivedBatchCount,
                long attemptedRecordCount,
                long recordCount,
                long failedRecordCount,
                long unknownStateRecordCount,
                long completedSplitCount,
                double averageQps,
                long durationMillis,
                String currentTable,
                String currentSplit) {

            this.taskId = taskId;
            this.pipelineId = pipelineId;
            this.taskType = taskType;
            this.state = state;
            this.subtaskIndex = subtaskIndex;
            this.parallelism = parallelism;
            this.batchCount = batchCount;
            this.receivedBatchCount =
                    receivedBatchCount;
            this.attemptedRecordCount =
                    attemptedRecordCount;
            this.recordCount = recordCount;
            this.failedRecordCount = failedRecordCount;
            this.unknownStateRecordCount =
                    unknownStateRecordCount;
            this.completedSplitCount =
                    completedSplitCount;
            this.averageQps = averageQps;
            this.durationMillis = durationMillis;
            this.currentTable = currentTable;
            this.currentSplit = currentSplit;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getPipelineId() {
            return pipelineId;
        }

        public String getTaskType() {
            return taskType;
        }

        public String getState() {
            return state;
        }

        public int getSubtaskIndex() {
            return subtaskIndex;
        }

        public int getParallelism() {
            return parallelism;
        }

        public long getBatchCount() {
            return batchCount;
        }

        public long getReceivedBatchCount() {
            return receivedBatchCount;
        }

        public long getAttemptedRecordCount() {
            return attemptedRecordCount;
        }

        public long getRecordCount() {
            return recordCount;
        }

        public long getFailedRecordCount() {
            return failedRecordCount;
        }

        public long getUnknownStateRecordCount() {
            return unknownStateRecordCount;
        }

        public long getCompletedSplitCount() {
            return completedSplitCount;
        }

        public double getAverageQps() {
            return averageQps;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public String getCurrentTable() {
            return currentTable;
        }

        public String getCurrentSplit() {
            return currentSplit;
        }
    }

    public static final class Channel {

        private final String channelId;
        private final long enqueuedCount;
        private final long dequeuedCount;
        private final long currentBatches;
        private final long currentRecords;
        private final long currentBytes;
        private final long maximumBatches;
        private final long maximumRecords;
        private final long maximumBytes;
        private final long oversizedBatches;
        private final double producerBackpressureRatio;
        private final double consumerIdleRatio;
        private final double rateLimitedRatio;

        Channel(
                String channelId,
                long enqueuedCount,
                long dequeuedCount,
                long currentBatches,
                long currentRecords,
                long currentBytes,
                long maximumBatches,
                long maximumRecords,
                long maximumBytes,
                long oversizedBatches,
                double producerBackpressureRatio,
                double consumerIdleRatio,
                double rateLimitedRatio) {

            this.channelId = channelId;
            this.enqueuedCount = enqueuedCount;
            this.dequeuedCount = dequeuedCount;
            this.currentBatches = currentBatches;
            this.currentRecords = currentRecords;
            this.currentBytes = currentBytes;
            this.maximumBatches = maximumBatches;
            this.maximumRecords = maximumRecords;
            this.maximumBytes = maximumBytes;
            this.oversizedBatches = oversizedBatches;
            this.producerBackpressureRatio =
                    producerBackpressureRatio;
            this.consumerIdleRatio = consumerIdleRatio;
            this.rateLimitedRatio = rateLimitedRatio;
        }

        public String getChannelId() {
            return channelId;
        }

        public long getEnqueuedCount() {
            return enqueuedCount;
        }

        public long getDequeuedCount() {
            return dequeuedCount;
        }

        public long getCurrentBatches() {
            return currentBatches;
        }

        public long getCurrentRecords() {
            return currentRecords;
        }

        public long getCurrentBytes() {
            return currentBytes;
        }

        public long getMaximumBatches() {
            return maximumBatches;
        }

        public long getMaximumRecords() {
            return maximumRecords;
        }

        public long getMaximumBytes() {
            return maximumBytes;
        }

        public long getOversizedBatches() {
            return oversizedBatches;
        }

        public double getProducerBackpressureRatio() {
            return producerBackpressureRatio;
        }

        public double getConsumerIdleRatio() {
            return consumerIdleRatio;
        }

        public double getRateLimitedRatio() {
            return rateLimitedRatio;
        }
    }
}