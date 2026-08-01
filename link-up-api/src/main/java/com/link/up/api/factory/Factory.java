package com.link.up.api.factory;

import com.link.up.api.connector.schema.ConnectorCapability;

import java.util.Collections;
import java.util.Set;

/**
 * Link-Up 插件工厂基础接口。
 *
 * <p>Source、Sink 等插件工厂都需要实现该接口。
 */
public interface Factory {

    /**
     * 工厂唯一标识。
     *
     * <p>例如：jdbc、doris、http、file。
     */
    String factoryIdentifier();

    /**
     * Connector Schema 的人工版本。
     *
     * <p>字段和规则发生不兼容变化时应提升版本；控制面还应结合
     * schemaFingerprint 判断精确变化。
     */
    default String schemaVersion() {
        return "1";
    }

    /**
     * Connector 实际支持的稳定能力集合。
     *
     * <p>这里不返回前端控件或页面布局信息。
     */
    default Set<ConnectorCapability> capabilities() {
        return Collections.emptySet();
    }
}
