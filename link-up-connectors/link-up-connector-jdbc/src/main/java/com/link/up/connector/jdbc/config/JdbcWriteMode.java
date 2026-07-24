package com.link.up.connector.jdbc.config;

/**
 * JDBC 离线写入模式。
 */
public enum JdbcWriteMode {

    /**
     * 直接插入。
     * <p>
     * 主键冲突时由数据库返回错误。
     */
    INSERT,

    /**
     * 根据主键执行插入或更新。
     * <p>
     * MySQL 通常生成：
     * <p>
     * INSERT ... ON DUPLICATE KEY UPDATE ...
     */
    UPSERT
}