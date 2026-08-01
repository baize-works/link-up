package com.link.up.api.job;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Framework-neutral structured offline batch job protocol. */
public final class JobSpec implements Serializable {
    public static final String CURRENT_API_VERSION = "link-up/v1";
    public static final String BATCH_SYNC_KIND = "BatchSyncJob";

    private String apiVersion = CURRENT_API_VERSION;
    private String kind = BATCH_SYNC_KIND;
    private String name;
    private Connector source;
    private Connector sink;
    private Runtime runtime = new Runtime();

    public JobSpec() {}

    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Connector getSource() { return source; }
    public void setSource(Connector source) { this.source = source; }
    public Connector getSink() { return sink; }
    public void setSink(Connector sink) { this.sink = sink; }
    public Runtime getRuntime() { return runtime; }
    public void setRuntime(Runtime runtime) { this.runtime = runtime; }

    public static final class Connector implements Serializable {
        private String connectorId;
        private Map<String, Object> options = new LinkedHashMap<String, Object>();

        public Connector() {}
        public String getConnectorId() { return connectorId; }
        public void setConnectorId(String connectorId) { this.connectorId = connectorId; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) {
            this.options = options == null
                    ? new LinkedHashMap<String, Object>()
                    : new LinkedHashMap<String, Object>(options);
        }
    }

    public static final class Runtime implements Serializable {
        private Integer batchSize;
        private Integer sourceParallelism;
        private Integer sinkParallelism;
        private Integer pipelineParallelism;
        private Integer maxBufferedBatches;
        private Long maxBufferedRecords;
        private Long maxBufferedBytes;
        private Long maxRecordsPerSecond;
        private Long maxBytesPerSecond;
        private String sinkPartitionStrategy;
        private String splitAssignmentMode;

        public Runtime() {}
        public Integer getBatchSize() { return batchSize; }
        public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }
        public Integer getSourceParallelism() { return sourceParallelism; }
        public void setSourceParallelism(Integer sourceParallelism) { this.sourceParallelism = sourceParallelism; }
        public Integer getSinkParallelism() { return sinkParallelism; }
        public void setSinkParallelism(Integer sinkParallelism) { this.sinkParallelism = sinkParallelism; }
        public Integer getPipelineParallelism() { return pipelineParallelism; }
        public void setPipelineParallelism(Integer pipelineParallelism) { this.pipelineParallelism = pipelineParallelism; }
        public Integer getMaxBufferedBatches() { return maxBufferedBatches; }
        public void setMaxBufferedBatches(Integer maxBufferedBatches) { this.maxBufferedBatches = maxBufferedBatches; }
        public Long getMaxBufferedRecords() { return maxBufferedRecords; }
        public void setMaxBufferedRecords(Long maxBufferedRecords) { this.maxBufferedRecords = maxBufferedRecords; }
        public Long getMaxBufferedBytes() { return maxBufferedBytes; }
        public void setMaxBufferedBytes(Long maxBufferedBytes) { this.maxBufferedBytes = maxBufferedBytes; }
        public Long getMaxRecordsPerSecond() { return maxRecordsPerSecond; }
        public void setMaxRecordsPerSecond(Long maxRecordsPerSecond) { this.maxRecordsPerSecond = maxRecordsPerSecond; }
        public Long getMaxBytesPerSecond() { return maxBytesPerSecond; }
        public void setMaxBytesPerSecond(Long maxBytesPerSecond) { this.maxBytesPerSecond = maxBytesPerSecond; }
        public String getSinkPartitionStrategy() { return sinkPartitionStrategy; }
        public void setSinkPartitionStrategy(String sinkPartitionStrategy) { this.sinkPartitionStrategy = sinkPartitionStrategy; }
        public String getSplitAssignmentMode() { return splitAssignmentMode; }
        public void setSplitAssignmentMode(String splitAssignmentMode) { this.splitAssignmentMode = splitAssignmentMode; }
    }
}
