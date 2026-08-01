package com.link.up.server.runtime;

/**
 * 一次可审计的作业状态转换。
 */
public final class JobStateTransition {

    private final long version;
    private final ServerJobStatus fromStatus;
    private final ServerJobStatus toStatus;
    private final long transitionTimeMillis;
    private final String reason;

    public JobStateTransition(
            long version,
            ServerJobStatus fromStatus,
            ServerJobStatus toStatus,
            long transitionTimeMillis,
            String reason) {

        this.version = version;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.transitionTimeMillis = transitionTimeMillis;
        this.reason = reason;
    }

    public long getVersion() {
        return version;
    }

    public ServerJobStatus getFromStatus() {
        return fromStatus;
    }

    public ServerJobStatus getToStatus() {
        return toStatus;
    }

    public long getTransitionTimeMillis() {
        return transitionTimeMillis;
    }

    public String getReason() {
        return reason;
    }
}
