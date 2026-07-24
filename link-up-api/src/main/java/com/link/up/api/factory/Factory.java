package com.link.up.api.factory;

/**
 * Flux 插件工厂基础接口。
 * <p>
 * Source、Sink 等插件工厂都需要实现该接口。
 */
public interface Factory {

    /**
     * 工厂唯一标识。
     * <p>
     * 例如：jdbc、mysql-cdc、file。
     */
    String factoryIdentifier();
}