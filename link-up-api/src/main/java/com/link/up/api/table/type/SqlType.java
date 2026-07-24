package com.link.up.api.table.type;

/**
 * Flux 支持的数据类型。
 * <p>
 * 这里只保留离线 JDBC 数据同步需要的常见类型。
 */
public enum SqlType {

    STRING,
    BOOLEAN,

    TINYINT,
    SMALLINT,
    INT,
    BIGINT,

    FLOAT,
    DOUBLE,
    DECIMAL,

    BYTES,

    DATE,
    TIME,
    TIMESTAMP,
    TIMESTAMP_TZ,

    MAP,
    ARRAY,
    ROW,

    /**
     * 用于 JDBC Types.NULL 或无法确定具体类型的空值。
     */
    NULL
}