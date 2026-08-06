package com.link.up.api.sink;

import java.io.Serializable;

/**
 * Immutable result of preparing one offline sink table.
 *
 * <p>Link-Up only reports the CREATE TABLE statement derived during offline
 * sink preparation. It does not model realtime schema-change events.</p>
 */
public final class TableDdl implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    public static final String REASON_TARGET_TABLE_CREATED =
            "TARGET_TABLE_CREATED";
    public static final String REASON_TARGET_TABLE_ALREADY_EXISTS =
            "TARGET_TABLE_ALREADY_EXISTS";
    public static final String REASON_CREATE_TABLE_NOT_EXECUTED =
            "CREATE_TABLE_NOT_EXECUTED";

    private final String dialect;
    private final String sourceTable;
    private final String targetTable;
    private final String createTableSql;
    private final boolean executed;
    private final String status;
    private final String reason;
    private final long durationMillis;
    private final String errorCode;
    private final String errorMessage;

    public TableDdl(
            String dialect,
            String sourceTable,
            String targetTable,
            String createTableSql,
            boolean executed,
            String status,
            String reason,
            long durationMillis,
            String errorCode,
            String errorMessage) {

        this.dialect = display(dialect);
        this.sourceTable = display(sourceTable);
        this.targetTable = display(targetTable);
        this.createTableSql = normalize(createTableSql);
        this.executed = executed;
        this.status = requireText(status, "status");
        this.reason = requireText(reason, "reason");
        this.durationMillis = Math.max(0L, durationMillis);
        this.errorCode = normalize(errorCode);
        this.errorMessage = normalize(errorMessage);
    }

    public String getDialect() {
        return dialect;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public String getCreateTableSql() {
        return createTableSql;
    }

    public boolean isExecuted() {
        return executed;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static String display(String value) {
        String normalized = normalize(value);
        return normalized == null ? "-" : normalized;
    }

    private static String requireText(String value, String name) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
