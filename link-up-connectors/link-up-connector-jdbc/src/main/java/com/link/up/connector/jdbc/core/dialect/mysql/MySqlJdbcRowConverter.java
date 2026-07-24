package com.link.up.connector.jdbc.core.dialect.mysql;

import com.link.up.connector.jdbc.core.converter.AbstractJdbcRowConverter;
import com.link.up.connector.jdbc.core.dialect.DatabaseIdentifier;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * MySQL JDBC 行转换器。
 */
public final class MySqlJdbcRowConverter
        extends AbstractJdbcRowConverter {

    @Override
    public String name() {
        return DatabaseIdentifier.MYSQL;
    }

    /**
     * MySQL TIME 使用 Timestamp 写入可保留毫秒精度。
     */
    @Override
    protected void writeTime(
            PreparedStatement statement,
            int index,
            LocalTime value)
            throws SQLException {

        statement.setTimestamp(
                index,
                java.sql.Timestamp.valueOf(
                        LocalDateTime.of(
                                LocalDate.now(),
                                value)));
    }
}
