package com.link.up.connector.jdbc.source;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.connector.jdbc.config.JdbcSourceConfig;
import com.link.up.connector.jdbc.config.SplitPlanningMode;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.core.dialect.JdbcDialectLoader;
import com.link.up.connector.jdbc.core.split.JdbcSplitStatistics;
import com.link.up.connector.jdbc.core.split.JdbcSplitStatisticsProvider;
import com.link.up.connector.jdbc.core.split.JdbcSplitStatisticsRequest;
import com.link.up.connector.jdbc.utils.JdbcCatalogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Creates the bounded JDBC splits for a prepared source.
 */
final class JdbcSourceSplitGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcSourceSplitGenerator.class);

    private JdbcSourceSplitGenerator() {
    }

    static List<JdbcSourceSplit> generate(
            JdbcSourceConfig config,
            Map<TablePath, CatalogTable> tables,
            int parallelism)
            throws Exception {

        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(tables, "tables must not be null");
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be greater than 0");
        }

        JdbcDialect dialect = JdbcDialectLoader.load(config.getConnectionConfig());
        Map<TablePath, JdbcSourceTable> sourceTables =
                JdbcCatalogUtils.getTables(config, dialect);

        for (JdbcSourceTable sourceTable : sourceTables.values()) {
            if (!tables.containsKey(sourceTable.getTablePath())) {
                throw new IllegalArgumentException(
                        "No prepared table metadata for "
                                + sourceTable.getTablePath());
            }
        }

        Map<TablePath, JdbcSourceTable> plannedTables = new LinkedHashMap<TablePath, JdbcSourceTable>();
        JdbcSplitStatisticsProvider statisticsProvider = new JdbcSplitStatisticsProvider(config.getConnectionConfig(), dialect);
        for (JdbcSourceTable table : sourceTables.values()) {
            JdbcSourceTable planned = table;
            if (table.getPartitionColumn() != null && config.getSplitPlanningMode() != SplitPlanningMode.MANUAL) {
                SplitPlanningMode mode = config.getSplitPlanningMode();
                try {
                    JdbcSplitStatistics statistics = statisticsProvider.collect(new JdbcSplitStatisticsRequest(table, mode, config.getStatisticsQueryTimeout(), config.getSampleSize()));
                    if (statistics.isEmpty()) {
                        plannedTables.put(table.getTablePath(), null);
                        continue;
                    }
                    if (statistics.isAllNull()) {
                        if (!config.isNullPartitionSingleSplit())
                            throw new IllegalArgumentException("Partition column is entirely NULL: " + table.getTablePath());
                    } else
                        planned = JdbcSourceTable.builder().tablePath(table.getTablePath()).query(table.getQuery()).partitionColumn(table.getPartitionColumn()).partitionNumber(table.getPartitionNumber()).partitionStart(statistics.getLowerBound().orElse(null)).partitionEnd(statistics.getUpperBound().orElse(null)).catalogTable(table.getCatalogTable()).build();
                } catch (UnsupportedOperationException e) {
                    if (mode != SplitPlanningMode.AUTO_SAMPLE || !config.isAllowStatisticsFallback()) throw e;
                    LOG.warn("AUTO_SAMPLE statistics unsupported for table {}; falling back to AUTO_MIN_MAX", table.getTablePath());
                    JdbcSplitStatistics statistics = statisticsProvider.collect(new JdbcSplitStatisticsRequest(table, SplitPlanningMode.AUTO_MIN_MAX, config.getStatisticsQueryTimeout(), config.getSampleSize()));
                    if (statistics.isEmpty()) {
                        plannedTables.put(table.getTablePath(), null);
                        continue;
                    }
                    planned = JdbcSourceTable.builder().tablePath(table.getTablePath()).query(table.getQuery()).partitionColumn(table.getPartitionColumn()).partitionNumber(table.getPartitionNumber()).partitionStart(statistics.getLowerBound().orElse(null)).partitionEnd(statistics.getUpperBound().orElse(null)).catalogTable(table.getCatalogTable()).build();
                } catch (Exception e) {
                    if (config.isMultiTable() && config.getMultiTableFailurePolicy().continueOtherTables()) {
                        LOG.warn("Split statistics failed for table {}; continuing according to multi-table policy", table.getTablePath());
                        continue;
                    }
                    throw e;
                }
            }
            plannedTables.put(planned.getTablePath(), planned);
        }
        plannedTables.values().removeIf(java.util.Objects::isNull);
        return new JdbcSourceSplitEnumerator(config, plannedTables, parallelism).enumerateSplits();
    }
}
