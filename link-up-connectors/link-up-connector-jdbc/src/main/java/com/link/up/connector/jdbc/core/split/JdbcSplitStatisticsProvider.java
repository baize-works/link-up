package com.link.up.connector.jdbc.core.split;

import com.link.up.connector.jdbc.config.JdbcConnectionConfig;
import com.link.up.connector.jdbc.config.SplitPlanningMode;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.internal.JdbcConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/**
 * Opens a short-lived connection exclusively for split statistics.
 */
public final class JdbcSplitStatisticsProvider {
    private final JdbcConnectionConfig connectionConfig;
    private final JdbcDialect dialect;

    public JdbcSplitStatisticsProvider(JdbcConnectionConfig connectionConfig, JdbcDialect dialect) {
        this.connectionConfig = connectionConfig;
        this.dialect = dialect;
    }

    public JdbcSplitStatistics collect(JdbcSplitStatisticsRequest request) throws Exception {
        String sql = sql(request);
        try (JdbcConnectionProvider provider = new JdbcConnectionProvider(connectionConfig, dialect);
             Connection connection = provider.getOrEstablishConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(request.getTimeoutSeconds());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return new JdbcSplitStatistics(null, null, 0L);
                Object min = result.getObject("flux_min"), max = result.getObject("flux_max");
                long count = result.getLong("flux_count");
                return new JdbcSplitStatistics(min == null ? null : String.valueOf(min), max == null ? null : String.valueOf(max), result.wasNull() ? null : count);
            }
        }
    }

    private String sql(JdbcSplitStatisticsRequest request) {
        if (request.getMode() == SplitPlanningMode.AUTO_SAMPLE) {
            Optional<String> sample = dialect.buildSampleSplitStatisticsSql(request.getTable(), request.getSampleSize());
            if (sample.isPresent()) return sample.get();
            throw new UnsupportedOperationException("Dialect " + dialect.name() + " does not support AUTO_SAMPLE statistics");
        }
        return dialect.buildSplitStatisticsSql(request.getTable(), true);
    }
}
