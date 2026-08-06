package com.link.up.server.dto;

/** One structured physical Link-Up job log event. */
public final class JobLogEntry {

    private final long sequence;
    private final Long timestampMillis;
    private final String source;
    private final String level;
    private final String thread;
    private final String logger;
    private final String message;

    public JobLogEntry(
            long sequence,
            Long timestampMillis,
            String level,
            String thread,
            String logger,
            String message) {

        this.sequence = sequence;
        this.timestampMillis = timestampMillis;
        this.source = "LINK_UP";
        this.level = level;
        this.thread = thread;
        this.logger = logger;
        this.message = message;
    }

    public long getSequence() {
        return sequence;
    }

    public Long getTimestampMillis() {
        return timestampMillis;
    }

    public String getSource() {
        return source;
    }

    public String getLevel() {
        return level;
    }

    public String getThread() {
        return thread;
    }

    public String getLogger() {
        return logger;
    }

    public String getMessage() {
        return message;
    }
}
