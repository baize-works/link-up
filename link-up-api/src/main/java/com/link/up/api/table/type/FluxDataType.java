package com.link.up.api.table.type;

import java.io.Serializable;

/**
 * Flux 逻辑数据类型。
 *
 * @param <T> Java 中对应的物理类型
 */
public interface FluxDataType<T> extends Serializable {

    /**
     * 获取该数据类型对应的 Java 类型。
     */
    Class<T> getTypeClass();

    /**
     * 获取该数据类型对应的 SQL 类型。
     */
    SqlType getSqlType();
}