package com.link.up.connector.jdbc.core.split;

import java.io.Serializable;
import java.util.Optional;

/**
 * Immutable result of one table's split statistics query.
 */
public final class JdbcSplitStatistics implements Serializable {
    private final String lowerBound;
    private final String upperBound;
    private final Long rowCount;

    public JdbcSplitStatistics(String lowerBound, String upperBound, Long rowCount) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.rowCount = rowCount;
    }

    public Optional<String> getLowerBound() {
        return Optional.ofNullable(lowerBound);
    }

    public Optional<String> getUpperBound() {
        return Optional.ofNullable(upperBound);
    }

    public Optional<Long> getRowCount() {
        return Optional.ofNullable(rowCount);
    }

    public boolean isEmpty() {
        return rowCount != null && rowCount == 0L;
    }

    public boolean isAllNull() {
        return rowCount != null && rowCount > 0 && lowerBound == null && upperBound == null;
    }
}
