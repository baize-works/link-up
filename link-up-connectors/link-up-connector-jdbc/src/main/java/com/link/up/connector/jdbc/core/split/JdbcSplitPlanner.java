package com.link.up.connector.jdbc.core.split;

import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.type.FluxDataType;
import com.link.up.connector.jdbc.config.JdbcSourceConfig;
import com.link.up.connector.jdbc.core.dialect.JdbcDialect;
import com.link.up.connector.jdbc.core.dialect.JdbcDialectLoader;
import com.link.up.connector.jdbc.source.JdbcSourceSplit;
import com.link.up.connector.jdbc.source.JdbcSourceTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 为有界 JDBC Source 生成启动时静态分片。
 *
 * <p>当前支持：
 *
 * <ul>
 *     <li>整数和小数范围分片</li>
 *     <li>固定长度可打印 ASCII 字符串范围分片</li>
 *     <li>数据库方言 HASH 分片</li>
 *     <li>AUTO 自动降级策略</li>
 * </ul>
 *
 * <p>该 Planner 不连接数据库，不执行 MIN/MAX、COUNT 或采样查询。
 */
public final class JdbcSplitPlanner {

    private static final Logger LOG =
            LoggerFactory.getLogger(JdbcSplitPlanner.class);

    private static final String SPLIT_QUERY_ALIAS = "flux_split";

    private JdbcSplitPlanner() {
    }

    /**
     * 默认采用 AUTO 字符串分片策略。
     */
    public static List<JdbcSourceSplit> plan(
            JdbcSourceConfig config,
            Map<TablePath, JdbcSourceTable> sourceTables,
            int parallelism) {

        return plan(
                config,
                sourceTables,
                parallelism,
                StringSplitStrategy.AUTO);
    }

    /**
     * 生成所有表的静态分片。
     */
    public static List<JdbcSourceSplit> plan(
            JdbcSourceConfig config,
            Map<TablePath, JdbcSourceTable> sourceTables,
            int parallelism,
            StringSplitStrategy stringSplitStrategy) {

        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(
                sourceTables,
                "sourceTables must not be null");
        Objects.requireNonNull(
                stringSplitStrategy,
                "stringSplitStrategy must not be null");

        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }

        if (sourceTables.isEmpty()) {
            return Collections.emptyList();
        }

        JdbcDialect dialect =
                JdbcDialectLoader.load(config.getConnectionConfig());

        /*
         * Map 的遍历顺序不一定稳定，按照表路径排序，保证计划结果可重复。
         */
        List<JdbcSourceTable> tables =
                new ArrayList<JdbcSourceTable>(sourceTables.size());

        for (JdbcSourceTable table : sourceTables.values()) {
            tables.add(
                    Objects.requireNonNull(
                            table,
                            "source table must not be null"));
        }

        Collections.sort(
                tables,
                new Comparator<JdbcSourceTable>() {
                    @Override
                    public int compare(
                            JdbcSourceTable left,
                            JdbcSourceTable right) {

                        return left.getTablePath()
                                .toString()
                                .compareTo(right.getTablePath().toString());
                    }
                });

        List<JdbcSourceSplit> result =
                new ArrayList<JdbcSourceSplit>();

        for (JdbcSourceTable table : tables) {
            List<JdbcSourceSplit> tableSplits =
                    planTable(
                            config,
                            dialect,
                            table,
                            parallelism,
                            stringSplitStrategy);

            result.addAll(tableSplits);

            LOG.info(
                    "Planned {} JDBC split(s) for table {}",
                    tableSplits.size(),
                    table.getTablePath());
        }

        return Collections.unmodifiableList(
                new ArrayList<JdbcSourceSplit>(result));
    }

    private static List<JdbcSourceSplit> planTable(
            JdbcSourceConfig config,
            JdbcDialect dialect,
            JdbcSourceTable table,
            int parallelism,
            StringSplitStrategy stringSplitStrategy) {

        String baseQuery = normalizeQuery(
                dialect.buildSelectSql(
                        table,
                        config.getWhereCondition()));

        if (!hasText(baseQuery)) {
            throw new IllegalArgumentException(
                    "generated select SQL must not be blank, table="
                            + table.getTablePath());
        }

        String partitionColumn = table.getPartitionColumn();

        if (!hasText(partitionColumn)) {
            return Collections.singletonList(
                    createSplit(
                            table,
                            baseQuery,
                            0,
                            null,
                            null,
                            null,
                            null));
        }

        Column column = findColumn(table, partitionColumn);

        validateBounds(table);

        int requestedChunks = resolveRequestedChunks(
                table,
                parallelism);

        Class<?> typeClass = wrapPrimitive(
                column.getDataType().getTypeClass());

        if (String.class.equals(typeClass)) {
            return planStringSplits(
                    dialect,
                    table,
                    column,
                    baseQuery,
                    requestedChunks,
                    parallelism,
                    stringSplitStrategy);
        }

        if (isNumericType(typeClass)) {
            return planNumericSplits(
                    dialect,
                    table,
                    column,
                    baseQuery,
                    requestedChunks,
                    parallelism,
                    typeClass);
        }

        throw new IllegalArgumentException(
                "unsupported partition column type "
                        + typeClass.getName()
                        + ", column="
                        + column.getName()
                        + ", table="
                        + table.getTablePath());
    }

    private static List<JdbcSourceSplit> planNumericSplits(
            JdbcDialect dialect,
            JdbcSourceTable table,
            Column column,
            String baseQuery,
            int requestedChunks,
            int parallelism,
            Class<?> typeClass) {

        BigDecimal lower = parseNumericBound(
                table.getPartitionStart(),
                "partition_lower_bound",
                column);

        BigDecimal upper = parseNumericBound(
                table.getPartitionEnd(),
                "partition_upper_bound",
                column);

        boolean integral = isIntegralType(typeClass);

        ChunkSplitter<BigDecimal> splitter =
                new DynamicChunkSplitter<BigDecimal>(
                        new FixedChunkSplitter(integral),
                        parallelism);

        List<Chunk<BigDecimal>> chunks =
                splitter.split(lower, upper, requestedChunks);

        return createRangeSplits(
                dialect,
                table,
                column,
                baseQuery,
                chunks);
    }

    private static List<JdbcSourceSplit> planStringSplits(
            JdbcDialect dialect,
            JdbcSourceTable table,
            Column column,
            String baseQuery,
            int requestedChunks,
            int parallelism,
            StringSplitStrategy strategy) {

        String lower = table.getPartitionStart();
        String upper = table.getPartitionEnd();

        switch (strategy) {
            case NONE:
                return Collections.singletonList(
                        createSplit(
                                table,
                                baseQuery,
                                0,
                                null,
                                null,
                                null,
                                null));

            case RANGE:
                StringRangeSplitDecision rangeDecision =
                        validateStringRangeSplit(
                                dialect,
                                table,
                                column,
                                lower,
                                upper);

                if (!rangeDecision.isSafe()) {
                    throw unsafeRangeException(
                            table,
                            column,
                            rangeDecision);
                }

                return planStringRangeSplits(
                        dialect,
                        table,
                        column,
                        baseQuery,
                        lower,
                        upper,
                        requestedChunks,
                        parallelism);

            case HASH:
                Optional<List<JdbcSourceSplit>> hashSplits =
                        tryPlanHashSplits(
                                dialect,
                                table,
                                column,
                                baseQuery,
                                lower,
                                upper,
                                requestedChunks,
                                parallelism);

                if (!hashSplits.isPresent()) {
                    throw new IllegalArgumentException(
                            "HASH string split is not supported by dialect "
                                    + dialect.getClass().getSimpleName());
                }

                return hashSplits.get();

            case AUTO:
                return planAutoStringSplits(
                        dialect,
                        table,
                        column,
                        baseQuery,
                        lower,
                        upper,
                        requestedChunks,
                        parallelism);

            default:
                throw new IllegalArgumentException(
                        "unsupported string split strategy: " + strategy);
        }
    }

    private static List<JdbcSourceSplit> planAutoStringSplits(
            JdbcDialect dialect,
            JdbcSourceTable table,
            Column column,
            String baseQuery,
            String lower,
            String upper,
            int requestedChunks,
            int parallelism) {

        StringRangeSplitDecision decision =
                validateStringRangeSplit(
                        dialect,
                        table,
                        column,
                        lower,
                        upper);

        if (decision.isSafe()) {
            try {
                return planStringRangeSplits(
                        dialect,
                        table,
                        column,
                        baseQuery,
                        lower,
                        upper,
                        requestedChunks,
                        parallelism);
            } catch (RuntimeException e) {
                LOG.warn(
                        "String RANGE split failed for table {}, column {}, "
                                + "trying HASH fallback",
                        table.getTablePath(),
                        column.getName(),
                        e);
            }
        } else {
            LOG.warn(
                    "String RANGE split is unsafe for table {}, column {}: {}",
                    table.getTablePath(),
                    column.getName(),
                    decision.getReason());
        }

        Optional<List<JdbcSourceSplit>> hashSplits =
                tryPlanHashSplits(
                        dialect,
                        table,
                        column,
                        baseQuery,
                        lower,
                        upper,
                        requestedChunks,
                        parallelism);

        if (hashSplits.isPresent()) {
            LOG.info(
                    "Use HASH string split for table {}, column {}",
                    table.getTablePath(),
                    column.getName());

            return hashSplits.get();
        }

        LOG.warn(
                "Neither RANGE nor HASH string split is available for table {}, "
                        + "fallback to a single split",
                table.getTablePath());

        return Collections.singletonList(
                createSplit(
                        table,
                        baseQuery,
                        0,
                        null,
                        null,
                        null,
                        null));
    }

    private static List<JdbcSourceSplit> planStringRangeSplits(
            JdbcDialect dialect,
            JdbcSourceTable table,
            Column column,
            String baseQuery,
            String lower,
            String upper,
            int requestedChunks,
            int parallelism) {

        ChunkSplitter<String> splitter =
                new DynamicChunkSplitter<String>(
                        new AsciiStringRangeSplitter(),
                        parallelism);

        List<Chunk<String>> chunks =
                splitter.split(lower, upper, requestedChunks);

        return createRangeSplits(
                dialect,
                table,
                column,
                baseQuery,
                chunks);
    }

    private static Optional<List<JdbcSourceSplit>> tryPlanHashSplits(
            JdbcDialect dialect,
            JdbcSourceTable table,
            Column column,
            String baseQuery,
            String lower,
            String upper,
            int requestedChunks,
            int parallelism) {

        int chunkCount =
                DynamicChunkSplitter.effectiveChunkCount(
                        requestedChunks,
                        parallelism);

        List<JdbcSourceSplit> result =
                new ArrayList<JdbcSourceSplit>(chunkCount);

        String quotedColumn =
                dialect.quoteIdentifier(column.getName());

        for (int bucket = 0; bucket < chunkCount; bucket++) {
            Optional<String> predicateOptional =
                    dialect.buildHashPartitionPredicate(
                            column,
                            bucket,
                            chunkCount);

            if (!predicateOptional.isPresent()
                    || !hasText(predicateOptional.get())) {

                return Optional.empty();
            }

            String predicate = buildHashPredicate(
                    quotedColumn,
                    predicateOptional.get(),
                    lower,
                    upper,
                    bucket == 0);

            String query = appendPredicate(
                    baseQuery,
                    predicate);

            result.add(
                    createSplit(
                            table,
                            query,
                            bucket,
                            column.getName(),
                            column.getDataType(),
                            Integer.valueOf(bucket),
                            null));
        }

        return Optional.of(
                Collections.unmodifiableList(result));
    }

    /**
     * Combines a dialect hash expression with the configured partition bounds.
     *
     * <p>HASH only assigns rows to splits; it must not expand the range selected
     * by {@code partition_lower_bound} and {@code partition_upper_bound}. NULL
     * values retain the same first-split ownership as range splitting.
     */
    static String buildHashPredicate(
            String quotedColumn,
            String hashPredicate,
            String lower,
            String upper,
            boolean firstChunk) {

        String boundedPredicate = "("
                + hashPredicate
                + " AND "
                + quotedColumn
                + " >= "
                + literal(lower)
                + " AND "
                + quotedColumn
                + " <= "
                + literal(upper)
                + ")";

        if (!firstChunk) {
            return boundedPredicate;
        }

        /* HASH(NULL) 通常返回 NULL，因此首个桶额外负责读取 NULL。 */
        return "("
                + boundedPredicate
                + " OR "
                + quotedColumn
                + " IS NULL)";
    }

    private static <T> List<JdbcSourceSplit> createRangeSplits(
            JdbcDialect dialect,
            JdbcSourceTable table,
            Column column,
            String baseQuery,
            List<Chunk<T>> chunks) {

        List<JdbcSourceSplit> result =
                new ArrayList<JdbcSourceSplit>(chunks.size());

        String quotedColumn =
                dialect.quoteIdentifier(column.getName());

        for (int index = 0; index < chunks.size(); index++) {
            Chunk<T> chunk = chunks.get(index);

            String predicate = buildRangePredicate(
                    quotedColumn,
                    chunk,
                    index == 0);

            result.add(
                    createSplit(
                            table,
                            appendPredicate(baseQuery, predicate),
                            index,
                            column.getName(),
                            column.getDataType(),
                            chunk.getStart(),
                            chunk.getEnd()));
        }

        return Collections.unmodifiableList(result);
    }

    private static String buildRangePredicate(
            String quotedColumn,
            Chunk<?> chunk,
            boolean firstChunk) {

        StringBuilder predicate = new StringBuilder();

        predicate.append(quotedColumn)
                .append(" >= ")
                .append(literal(chunk.getStart()))
                .append(" AND ")
                .append(quotedColumn)
                .append(chunk.isEndInclusive() ? " <= " : " < ")
                .append(literal(chunk.getEnd()));

        /*
         * partition_column 不是主键时可能包含 NULL。
         * 由第一个分片统一读取，避免漏数和重复。
         */
        if (firstChunk) {
            return "("
                    + predicate
                    + " OR "
                    + quotedColumn
                    + " IS NULL)";
        }

        return predicate.toString();
    }

    private static StringRangeSplitDecision validateStringRangeSplit(
            JdbcDialect dialect,
            JdbcSourceTable table,
            Column column,
            String lower,
            String upper) {

        StringRangeSplitDecision asciiDecision =
                AsciiStringRangeSplitter.assess(lower, upper);

        if (!asciiDecision.isSafe()) {
            return asciiDecision;
        }

        StringRangeSplitDecision dialectDecision =
                dialect.validateStringRangeSplit(
                        table,
                        column,
                        lower,
                        upper);

        if (dialectDecision == null) {
            return StringRangeSplitDecision.unsafe(
                    "dialect returned no string range split decision");
        }

        return dialectDecision;
    }

    private static IllegalArgumentException unsafeRangeException(
            JdbcSourceTable table,
            Column column,
            StringRangeSplitDecision decision) {

        return new IllegalArgumentException(
                "string RANGE split is unsafe, table="
                        + table.getTablePath()
                        + ", column="
                        + column.getName()
                        + ", reason="
                        + decision.getReason());
    }

    private static JdbcSourceSplit createSplit(
            JdbcSourceTable table,
            String query,
            int index,
            String key,
            FluxDataType<?> type,
            Object start,
            Object end) {

        return new JdbcSourceSplit(
                table.getTablePath(),
                createSplitId(table.getTablePath(), index),
                query,
                key,
                type,
                start,
                end);
    }

    private static String createSplitId(
            TablePath tablePath,
            int index) {

        return tablePath + "-" + index;
    }

    private static Column findColumn(
            JdbcSourceTable table,
            String name) {

        for (Column column :
                table.getCatalogTable()
                        .getTableSchema()
                        .getColumns()) {

            if (column.getName().equalsIgnoreCase(name)) {
                return column;
            }
        }

        throw new IllegalArgumentException(
                "partition_column does not exist in source table: "
                        + name
                        + ", table="
                        + table.getTablePath());
    }

    private static void validateBounds(
            JdbcSourceTable table) {

        if (!hasText(table.getPartitionStart())
                || !hasText(table.getPartitionEnd())) {

            throw new IllegalArgumentException(
                    "partition_lower_bound and partition_upper_bound "
                            + "are required for partitioned table "
                            + table.getTablePath());
        }
    }

    private static int resolveRequestedChunks(
            JdbcSourceTable table,
            int parallelism) {

        Integer configured = table.getPartitionNumber();

        if (configured == null) {
            return parallelism;
        }

        if (configured.intValue() <= 0) {
            throw new IllegalArgumentException(
                    "partition_number must be greater than 0, table="
                            + table.getTablePath());
        }

        return configured.intValue();
    }

    private static BigDecimal parseNumericBound(
            String value,
            String optionName,
            Column column) {

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    optionName
                            + " must be numeric for column "
                            + column.getName()
                            + ", value="
                            + value,
                    e);
        }
    }

    private static String appendPredicate(
            String query,
            String predicate) {

        /*
         * 不使用 AS，兼容 Oracle 子查询别名语法。
         */
        return "SELECT * FROM ("
                + normalizeQuery(query)
                + ") "
                + SPLIT_QUERY_ALIAS
                + " WHERE "
                + predicate;
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }

        String normalized = query.trim();

        while (normalized.endsWith(";")) {
            normalized = normalized
                    .substring(0, normalized.length() - 1)
                    .trim();
        }

        return normalized;
    }

    private static String literal(Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "partition boundary must not be null");
        }

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value)
                    .stripTrailingZeros()
                    .toPlainString();
        }

        if (value instanceof BigInteger) {
            return value.toString();
        }

        if (value instanceof Number) {
            return value.toString();
        }

        String text = value.toString()
                .replace("'", "''");

        return "'" + text + "'";
    }

    private static boolean isNumericType(Class<?> typeClass) {
        return Number.class.isAssignableFrom(typeClass)
                || BigInteger.class.equals(typeClass)
                || BigDecimal.class.equals(typeClass);
    }

    private static boolean isIntegralType(Class<?> typeClass) {
        return Byte.class.equals(typeClass)
                || Short.class.equals(typeClass)
                || Integer.class.equals(typeClass)
                || Long.class.equals(typeClass)
                || BigInteger.class.equals(typeClass);
    }

    private static Class<?> wrapPrimitive(Class<?> typeClass) {
        if (typeClass == null || !typeClass.isPrimitive()) {
            return typeClass;
        }

        if (byte.class.equals(typeClass)) {
            return Byte.class;
        }
        if (short.class.equals(typeClass)) {
            return Short.class;
        }
        if (int.class.equals(typeClass)) {
            return Integer.class;
        }
        if (long.class.equals(typeClass)) {
            return Long.class;
        }
        if (float.class.equals(typeClass)) {
            return Float.class;
        }
        if (double.class.equals(typeClass)) {
            return Double.class;
        }

        return typeClass;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
