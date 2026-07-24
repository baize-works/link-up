package com.link.up.connector.jdbc.options;

/**
 * Failure handling policy for multi-table jobs.
 */
public enum MultiTableFailurePolicy {
    FAIL_FAST,
    CONTINUE_OTHER_TABLES;

    public boolean continueOtherTables() {
        return this == CONTINUE_OTHER_TABLES;
    }
}
