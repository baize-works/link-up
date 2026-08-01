package com.link.up.server.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作业协议元数据和状态机快照。
 */
public final class JobExecutionMetadata {

    private final String externalExecutionId;
    private final String idempotencyKey;
    private final int definitionVersion;
    private final String configDigest;
    private final long submittedTimeMillis;
    private final long queuedTimeMillis;
    private final long stateVersion;
    private final boolean cancellationRequested;
    private final List<JobStateTransition> transitions;

    public JobExecutionMetadata(
            String externalExecutionId,
            String idempotencyKey,
            int definitionVersion,
            String configDigest,
            long submittedTimeMillis,
            long queuedTimeMillis,
            long stateVersion,
            boolean cancellationRequested,
            List<JobStateTransition> transitions) {

        this.externalExecutionId = externalExecutionId;
        this.idempotencyKey = idempotencyKey;
        this.definitionVersion = definitionVersion;
        this.configDigest = configDigest;
        this.submittedTimeMillis = submittedTimeMillis;
        this.queuedTimeMillis = queuedTimeMillis;
        this.stateVersion = stateVersion;
        this.cancellationRequested = cancellationRequested;
        this.transitions =
                Collections.unmodifiableList(
                        new ArrayList<JobStateTransition>(
                                transitions));
    }

    public String getExternalExecutionId() {
        return externalExecutionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getDefinitionVersion() {
        return definitionVersion;
    }

    public String getConfigDigest() {
        return configDigest;
    }

    public long getSubmittedTimeMillis() {
        return submittedTimeMillis;
    }

    public long getQueuedTimeMillis() {
        return queuedTimeMillis;
    }

    public long getStateVersion() {
        return stateVersion;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested;
    }

    public List<JobStateTransition> getTransitions() {
        return transitions;
    }
}
