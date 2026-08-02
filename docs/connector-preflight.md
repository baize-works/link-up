# Connector 安全预检协议

## 目标

Yak Ops 进行多 Worker 调度时，需要确认数据源和目标端从实际 Link-Up Worker 所在网络可达。

预检必须满足：

- 不创建任务实例
- 不进入 Worker 任务队列
- 不读取业务表数据
- 不执行建表、更新或写入
- 请求和错误日志不得泄露密码

## REST API

```http
POST /api/v1/connectors/{connectorId}/preflight?role=SOURCE|SINK
Content-Type: application/json
```

请求：

```json
{
  "options": {
    "url": "jdbc:mysql://db.example:3306/app",
    "driver": "com.mysql.cj.jdbc.Driver",
    "user": "app_user",
    "password": "******"
  }
}
```

成功响应：

```json
{
  "connectorId": "jdbc",
  "role": "SOURCE",
  "status": "REACHABLE",
  "durationMillis": 35,
  "checkedAtMillis": 1785672000000
}
```

常见错误：

| HTTP | code | 含义 |
|---|---|---|
| 400 | `FLUX-CONNECTOR-PREFLIGHT-CONFIG-INVALID` | Connector 配置不合法 |
| 404 | `FLUX-CONNECTOR-NOT-FOUND` | Worker 未安装该 Connector |
| 422 | `FLUX-CONNECTOR-PREFLIGHT-UNSUPPORTED` | Connector 尚未实现安全预检 |
| 501 | `FLUX-CONNECTOR-PREFLIGHT-DISABLED` | 当前服务未启用预检执行器 |
| 503 | `FLUX-CONNECTOR-PREFLIGHT-FAILED` | 外部系统不可达或连接验证失败 |

## Connector SPI

Connector 可以选择实现：

```java
public interface ConnectorPreflightSupport {

    void preflight(
            ReadonlyConfig options,
            ClassLoader classLoader)
            throws Exception;
}
```

实现约束：

1. 只验证配置和外部系统连接。
2. 不得创建业务对象、表、Topic 或索引。
3. 不得写入业务数据。
4. 必须及时关闭连接、流和其他外部资源。
5. 抛出的错误不得主动包含密码、Token 或完整凭据。
6. 未实现该接口的 Connector 会明确返回 `UNSUPPORTED`，不会假装预检成功。

## JDBC 实现

JDBC Source 和 Sink 均实现安全预检：

1. 使用原 Connector 配置规则解析参数。
2. 通过 Connector 所属 ClassLoader 加载 JDBC Driver。
3. 建立一次 JDBC Connection。
4. 调用 `Connection.isValid(timeoutSeconds)`。
5. 立即关闭 Connection。

预检不会执行 SQL，也不会触发表结构发现、自动建表或 Sink Preparer。

## 生命周期

Link-Up Server 使用同一个 `FactoryRegistry`：

- 生成 Connector Schema
- 执行 Connector preflight
- 在 Worker 关闭时统一释放 Connector ClassLoader

因此不会为了每次预检重新发现插件或重复创建插件类加载器。
