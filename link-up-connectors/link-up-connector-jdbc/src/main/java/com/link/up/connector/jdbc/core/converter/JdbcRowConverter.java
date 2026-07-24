package com.link.up.connector.jdbc.core.converter;

import com.link.up.api.table.catalog.TableSchema;
import com.link.up.api.table.type.FluxRow;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC 数据行转换器。
 * <p>
 * 负责：
 * <p>
 * 1. 将 JDBC ResultSet 转换为 FluxRow；
 * 2. 将 FluxRow 写入 PreparedStatement。
 * <p>
 * 转换顺序严格按照 TableSchema 中的字段顺序执行。
 */
public interface JdbcRowConverter extends Serializable {

    /**
     * 转换器名称，通常与数据库方言名称一致。
     */
    String name();

    /**
     * 将 ResultSet 当前行转换为 FluxRow。
     *
     * @param resultSet   JDBC 查询结果，必须已经指向有效数据行
     * @param tableSchema 当前数据表结构
     * @return Flux 内部数据行
     */
    FluxRow read(
            ResultSet resultSet,
            TableSchema tableSchema)
            throws SQLException;

    /**
     * 将 FluxRow 字段绑定到 PreparedStatement。
     * <p>
     * 该方法只绑定参数，不负责执行 SQL。
     *
     * @param statement   JDBC PreparedStatement
     * @param row         Flux 数据行
     * @param tableSchema 当前数据表结构
     */
    void write(
            PreparedStatement statement,
            FluxRow row,
            TableSchema tableSchema)
            throws SQLException;
}