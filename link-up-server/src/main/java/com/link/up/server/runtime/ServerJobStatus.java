package com.link.up.server.runtime;

/**
 * Link-Up 离线作业对外状态。
 *
 * <p>状态机固定为：
 * CREATED -> SUBMITTED -> QUEUED -> RUNNING ->
 * SUCCEEDED / FAILED / CANCELED / LOST。
 */
public enum ServerJobStatus {

    CREATED(false),
    SUBMITTED(false),
    QUEUED(false),
    RUNNING(false),
    SUCCEEDED(true),
    FAILED(true),
    CANCELED(true),
    LOST(true);

    private final boolean terminal;

    ServerJobStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean canCancel() {
        return !terminal;
    }
}
