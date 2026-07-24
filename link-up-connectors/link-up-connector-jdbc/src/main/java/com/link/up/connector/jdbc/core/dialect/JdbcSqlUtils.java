package com.link.up.connector.jdbc.core.dialect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC 离线查询辅助方法。
 */
public final class JdbcSqlUtils {

    private JdbcSqlUtils() {
    }

    /**
     * 统计物理表行数。
     */
    public static long countTable(
            Connection connection,
            String quotedTable)
            throws SQLException {

        return queryLong(
                connection,
                "SELECT COUNT(*) FROM " + quotedTable);
    }

    /**
     * 统计自定义查询结果行数。
     */
    public static long countSubquery(
            Connection connection,
            String query)
            throws SQLException {

        String normalized =
                removeTrailingSemicolon(query);

        return queryLong(
                connection,
                "SELECT COUNT(*) FROM ("
                        + normalized
                        + ") flux_count_tmp");
    }

    /**
     * 查询单个 long 结果。
     */
    public static long queryLong(
            Connection connection,
            String sql)
            throws SQLException {

        try (Statement statement =
                     connection.createStatement();
             ResultSet resultSet =
                     statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                throw new SQLException(
                        "SQL 未返回结果：" + sql);
            }

            return resultSet.getLong(1);
        }
    }

    public static String removeTrailingSemicolon(
            String sql) {

        if (sql == null) {
            throw new IllegalArgumentException(
                    "sql must not be null");
        }

        String normalized = sql.trim();

        while (normalized.endsWith(";")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1)
                            .trim();
        }

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "sql must not be empty");
        }

        return normalized;
    }
}
