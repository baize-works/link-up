package com.link.up.framework.job;

import com.link.up.api.sink.CommitScope;

import java.util.Objects;

/**
 * Immutable distinction between task-local transactions and confirmed data commits.
 */
public final class CommitSummary {
    private final int totalTaskCount, finishedTaskCount, committedTaskCount, emptyCommittedTaskCount, failedOrUncommittedTaskCount;
    private final long attemptedRecordCount, successfullyWrittenRecordCount, successfullyCommittedRecordCount, failedRecordCount, unknownStateRecordCount;
    private final CommitScope commitScope;
    private final String retryAdvice;

    public CommitSummary(int total, int finished, int committed, int empty, int failed, long attempted, long written, long committedRecords, long failedRecords, long unknown, CommitScope scope, String advice) {
        totalTaskCount = total;
        finishedTaskCount = finished;
        committedTaskCount = committed;
        emptyCommittedTaskCount = empty;
        failedOrUncommittedTaskCount = failed;
        attemptedRecordCount = attempted;
        successfullyWrittenRecordCount = written;
        successfullyCommittedRecordCount = committedRecords;
        failedRecordCount = failedRecords;
        unknownStateRecordCount = unknown;
        commitScope = Objects.requireNonNull(scope, "commitScope");
        retryAdvice = Objects.requireNonNull(advice, "retryAdvice");
    }

    /**
     * Compatibility constructor: committed tasks are conservatively treated as empty.
     */
    public CommitSummary(int committed, int failed, CommitScope scope, String advice) {
        this(committed + failed, committed, committed, committed, failed, 0, 0, 0, 0, 0, scope, advice);
    }

    public static CommitSummary empty() {
        return new CommitSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, CommitScope.TASK_LOCAL, "No sink tasks were executed.");
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

    public int getEmptyCommittedTaskCount() {
        return emptyCommittedTaskCount;
    }

    public int getDataCommittedTaskCount() {
        return committedTaskCount - emptyCommittedTaskCount;
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
        return committedTaskCount > 0 && failedOrUncommittedTaskCount > 0;
    }

    public boolean isPartialDataCommit() {
        return successfullyCommittedRecordCount > 0 && failedOrUncommittedTaskCount > 0;
    }

    public boolean isPartialCommit() {
        return isPartialDataCommit();
    }

    public CommitScope getCommitScope() {
        return commitScope;
    }

    public String getRetryAdvice() {
        return retryAdvice;
    }

    public String getWarning() {
        return isPartialDataCommit() ? "Partial data commit detected; task-local commits cannot be rolled back globally." : "";
    }
}
