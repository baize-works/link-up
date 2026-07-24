package com.link.up.connector.jdbc.core.dialect;

import com.link.up.connector.jdbc.config.JdbcConnectionConfig;

/**
 * JDBC 方言工厂。
 * <p>
 * 每一种数据库通过 SPI 提供一个实现。
 */
public interface JdbcDialectFactory {

    /**
     * 方言唯一标识，例如 mysql。
     */
    String identifier();

    /**
     * 判断当前工厂是否支持该 JDBC URL。
     */
    boolean acceptsUrl(String url);

    /**
     * 根据连接配置创建无状态方言对象。
     */
    JdbcDialect create(JdbcConnectionConfig connectionConfig);
}
