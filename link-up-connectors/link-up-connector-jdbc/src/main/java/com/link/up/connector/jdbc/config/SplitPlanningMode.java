package com.link.up.connector.jdbc.config;

/**
 * How partition boundaries are obtained before split planning.
 */
public enum SplitPlanningMode {
    /**
     * Use configured bounds only; no statistics SQL is executed.
     */
    MANUAL,
    /**
     * Obtain MIN/MAX (and COUNT) before planning.
     */
    AUTO_MIN_MAX,
    /**
     * Obtain sampled boundaries when the dialect supports it.
     */
    AUTO_SAMPLE
}
