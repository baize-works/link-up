package com.link.up.connector.jdbc.core.split;

import com.link.up.connector.jdbc.config.SplitPlanningMode;
import com.link.up.connector.jdbc.source.JdbcSourceTable;

import java.util.Objects;

/**
 * Everything required to collect statistics without exposing a reader connection.
 */
public final class JdbcSplitStatisticsRequest {
    private final JdbcSourceTable table;
    private final SplitPlanningMode mode;
    private final int timeoutSeconds;
    private final int sampleSize;

    public JdbcSplitStatisticsRequest(JdbcSourceTable table, SplitPlanningMode mode, int timeoutSeconds, int sampleSize) {
        this.table = Objects.requireNonNull(table, "table");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.timeoutSeconds = timeoutSeconds;
        this.sampleSize = sampleSize;
    }

    public JdbcSourceTable getTable() {
        return table;
    }

    public SplitPlanningMode getMode() {
        return mode;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getSampleSize() {
        return sampleSize;
    }
}
