package com.link.up.server.runtime;

/**
 * REST Server 对外提供的异步作业状态。
 */
public enum ServerJobStatus {

    SUBMITTED(false),
    RUNNING(false),
    CANCELLING(false),
    CANCELED(true),
    SUCCEEDED(true),
    FAILED(true);

    private final boolean terminal;

    ServerJobStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean canCancel() {
        return this == SUBMITTED
                || this == RUNNING
                || this == CANCELLING;
    }
}