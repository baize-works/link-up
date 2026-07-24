package com.link.up.connector.jdbc.core.dialect;

import com.link.up.api.table.catalog.Column;

import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JDBC 元数据与 Flux 字段类型之间的转换器。
 * <p>
 * 同一个实现同时服务于：
 * <p>
 * 1. Source 自定义查询结果结构发现；
 * 2. Catalog 字段发现；
 * 3. Sink 自动建表字段类型生成。
 */
public interface JdbcTypeMapper extends Serializable {

    /**
     * 将查询结果中的一个 JDBC 字段转换为 Flux Column。
     */
    Column map(
            ResultSetMetaData metadata,
            int columnIndex)
            throws SQLException;

    /**
     * 将查询结果中的全部字段转换为 Flux Column。
     */
    default List<Column> map(
            ResultSetMetaData metadata)
            throws SQLException {

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "metadata must not be null");
        }

        List<Column> columns =
                new ArrayList<>(
                        metadata.getColumnCount());

        for (int i = 1;
             i <= metadata.getColumnCount();
             i++) {

            columns.add(map(metadata, i));
        }

        return Collections.unmodifiableList(columns);
    }

    /**
     * 将 Flux Column 转换为目标数据库字段类型。
     */
    String toDatabaseType(Column column);
}
