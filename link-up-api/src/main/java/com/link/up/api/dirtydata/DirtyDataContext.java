package com.link.up.api.dirtydata;

import java.io.Serializable;

/**
 * Immutable, serializable location of a rejected record.
 */
public final class DirtyDataContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String jobId, taskId, connector, dataSetId, splitId;

    public DirtyDataContext(String jobId, String taskId, String connector, String dataSetId, String splitId) {
        this.jobId = jobId;
        this.taskId = taskId;
        this.connector = connector;
        this.dataSetId = dataSetId;
        this.splitId = splitId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getConnector() {
        return connector;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public String getSplitId() {
        return splitId;
    }

    public DirtyDataContext withDataSet(String dataSet, String split) {
        return new DirtyDataContext(jobId, taskId, connector, dataSet, split);
    }
}
