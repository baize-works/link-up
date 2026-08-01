# Link-Up Connector Schema 协议

Connector Schema 是 Link-Up 向 Yak Ops 等控制面公开的机器可读能力模型。它描述 Connector 实际接受的参数、类型、默认值、规则和执行能力，不描述 Ant Design 控件、页面分组、字段顺序或高级配置折叠方式。

## 职责边界

```text
Link-Up OptionRule
        ↓
ConnectorSchemaExporter
        ↓
Connector Schema REST API
        ↓
Yak Ops Schema Registry / Presentation Profile
        ↓
Yak Ops Dynamic Form
```

- Link-Up 是参数合法性、条件规则和能力的最终事实来源。
- Yak Ops 后端可以缓存 Schema，并叠加自己的 Presentation Profile。
- Yak Ops 前端只消费合并后的 Form Schema，不应直接依赖 Java 类名。
- HOCON 仍然是当前 Job 配置编码，但不属于 Connector Schema 协议。

## 接口

### 查询全部 Schema

```http
GET /api/v1/connectors
GET /api/v1/connectors?role=SOURCE
GET /api/v1/connectors?role=SINK
```

当前第一阶段直接返回完整 Schema 列表。后续 Connector 数量较多时，可以增加独立的摘要接口而不破坏单个 Schema 契约。

### 查询单个 Schema

```http
GET /api/v1/connectors/{connectorId}/schema?role=SOURCE
GET /api/v1/connectors/{connectorId}/schema?role=SINK
```

`role` 必填，因为同一个标识可以同时提供 Source 和 Sink，例如 JDBC。

## 响应示例

```json
{
  "connectorId": "jdbc",
  "role": "SOURCE",
  "schemaVersion": "1",
  "schemaFingerprint": "sha256:...",
  "implementationClass": "com.link.up.connector.jdbc.source.JdbcSourceFactory",
  "implementationVersion": "1.0.0",
  "options": [
    {
      "key": "url",
      "valueType": "STRING",
      "javaType": "java.lang.String",
      "elementJavaType": null,
      "defaultValue": null,
      "allowedValues": [],
      "description": "JDBC 连接地址",
      "fallbackKeys": [],
      "required": true,
      "sensitive": false,
      "semanticType": "JDBC_URL",
      "scope": "DATASOURCE"
    },
    {
      "key": "password",
      "valueType": "STRING",
      "javaType": "java.lang.String",
      "defaultValue": null,
      "allowedValues": [],
      "required": false,
      "sensitive": true,
      "semanticType": "PASSWORD",
      "scope": "DATASOURCE"
    }
  ],
  "rules": [
    {
      "type": "REQUIRED",
      "optionKeys": ["url", "driver"],
      "condition": null,
      "nestedRules": []
    },
    {
      "type": "CONSTRAINT",
      "optionKeys": ["url"],
      "condition": {
        "optionKey": "url",
        "operator": "EXTENSION",
        "expectedValue": null,
        "compareOptionKey": null,
        "extensionDescription": "JDBC URL 必须以 jdbc: 开头",
        "logicalOperator": null,
        "next": null
      },
      "nestedRules": []
    }
  ],
  "capabilities": [
    "CUSTOM_SQL",
    "MULTI_TABLE",
    "PARTITION_SPLIT",
    "TABLE_SCHEMA_DISCOVERY"
  ]
}
```

## Option 元数据

### valueType

`valueType` 是跨语言基础类型，不要求控制面理解 Java 泛型：

- `BOOLEAN`
- `INTEGER`
- `LONG`
- `DECIMAL`
- `FLOAT`
- `DOUBLE`
- `STRING`
- `DURATION`
- `MAP`
- `LIST`
- `ENUM`
- `OBJECT`
- `UNKNOWN`

`javaType` 和 `elementJavaType` 用于诊断、缓存兼容判断和复杂对象扩展，不应直接决定前端控件。

### semanticType

`semanticType` 是稳定业务语义，例如：

- `JDBC_URL`
- `USERNAME`
- `PASSWORD`
- `DRIVER_CLASS`
- `SCHEMA_NAME`
- `SQL`
- `TABLE_PATH`

未声明时为 `GENERIC`。Link-Up 只提供语义，最终显示名称和控件仍由 Yak Ops Presentation Profile 决定。

### scope

- `DATASOURCE`：通常由数据源中心提供，例如 URL、用户名和密码。
- `TASK`：属于任务定义，例如表、SQL 和写入模式。
- `RUNTIME`：属于执行调优，例如并行度、批次和超时。
- `SYSTEM`：由 Worker 或平台注入，不应由普通用户填写。

### sensitive

`sensitive=true` 表示该值不得出现在日志、错误详情、页面明文回显或普通配置导出中。Schema 本身只公开字段定义，不返回实际配置值。

## Rule 类型

- `REQUIRED`：字段绝对必填。
- `EXCLUSIVE`：字段组中只能选择一个。
- `BUNDLED`：字段必须同时存在或同时不存在。
- `REQUIRED_WHEN`：条件成立时字段必填。
- `CONSTRAINT`：字段值必须满足条件。
- `RULE_WHEN`：条件成立时应用一组子规则；`optionKeys` 表示该条件下启用的可选字段。

条件链通过 `logicalOperator` 和 `next` 表示 AND / OR，不把 Java `Condition` 的字符串形式当作协议。

## 版本与指纹

- `schemaVersion` 由 Connector 作者维护，不兼容变化时应提升。
- `schemaFingerprint` 根据 Connector ID、角色、Option、规则和能力生成 SHA-256。
- 控制面保存任务版本时应同时保存 Connector ID、角色、Schema 版本和指纹。
- 实现版本不会参与指纹，同一实现重新打包不会导致无意义的 Schema 变化。

## 第一阶段非目标

本阶段不包含：

- 前端分组、排序、重要程度和控件定义。
- 数据库、表和字段发现 Action API。
- Connector 配置在线校验 API。
- JSON JobSpec 或 HOCON 替换。
- Yak Ops Presentation Profile。

这些能力会在后续阶段建立在本 Schema 契约之上。
