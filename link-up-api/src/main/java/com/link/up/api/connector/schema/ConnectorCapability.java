package com.link.up.api.connector.schema;

/**
 * Connector 可以向控制面公开的稳定能力标识。
 *
 * <p>这里只描述执行引擎实际支持的能力，不包含具体前端控件和页面布局。
 */
public enum ConnectorCapability {
    TABLE_SCHEMA_DISCOVERY,
    MULTI_TABLE,
    CUSTOM_SQL,
    PARTITION_SPLIT,
    UPSERT,
    AUTO_CREATE_TABLE,
    DIRTY_DATA_HANDLING
}
