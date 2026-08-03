# Link-Up Worker 动态注册

## 概述

Link-Up Worker 可以在 HTTP 服务启动后主动注册到 Yak Ops，并持续发送签名心跳。动态注册默认关闭，不影响现有独立运行方式。

注册心跳会报告：

- nodeId、nodeName、instanceId
- 对外可访问的 Worker Base URL
- 引擎版本和启动时间
- 运行并发、等待队列和当前负载
- Connector、角色、Schema 指纹和能力集合

Yak Ops 返回 leaseId 和建议心跳间隔。Worker 在优雅关闭时会主动注销租约。

## 配置

环境变量：

```bash
export LINK_UP_REGISTRATION_ENABLED=true
export LINK_UP_CONTROL_PLANE_URL='https://yak-ops.example.com'
export LINK_UP_REGISTRATION_SECRET='replace-with-at-least-16-random-characters'
export LINK_UP_ADVERTISED_BASE_URL='http://link-up-worker-01:18080'
export LINK_UP_REGISTRATION_HEARTBEAT_MILLIS=20000
export LINK_UP_REGISTRATION_LABELS='region=south,zone=az-1'
```

等价 JVM 参数：

```text
-Dlink.up.registration.enabled=true
-Dlink.up.registration.control-plane-url=https://yak-ops.example.com
-Dlink.up.registration.secret=...
-Dlink.up.registration.advertised-base-url=http://link-up-worker-01:18080
-Dlink.up.registration.heartbeat-millis=20000
-Dlink.up.registration.labels=region=south,zone=az-1
```

可选超时：

```text
LINK_UP_REGISTRATION_CONNECT_TIMEOUT_MILLIS=5000
LINK_UP_REGISTRATION_REQUEST_TIMEOUT_MILLIS=10000
```

`LINK_UP_ADVERTISED_BASE_URL` 必须是 Yak Ops 和其他控制面实例真正能够访问的地址，不应使用容器内部不可达的 `127.0.0.1`。

## 签名

所有注册请求都使用 HMAC-SHA256：

```text
POST\n
/api/v1/offline/worker-registration/register\n
<timestampMillis>\n
<nonce>\n
<sha256(rawBody)>
```

请求头：

```text
X-Yak-Registration-Timestamp
X-Yak-Registration-Nonce
X-Yak-Registration-Signature
```

共享密钥必须与 Yak Ops 的 `YAK_OFFLINE_REGISTRATION_SECRET` 完全一致。密钥尾部字符会原样参与签名，不会被当成 URL 处理。

## 生命周期

启动：

1. Worker HTTP 服务完成监听。
2. 注册代理向 Yak Ops 发送注册请求。
3. Yak Ops 返回 leaseId。
4. 代理按照控制面建议间隔发送心跳。

网络异常：

- 使用 1 秒起步、最大 60 秒的指数退避。
- `401 / 404 / 409` 会触发重新注册。
- 已知心跳序列不会因不确定网络重试回退。
- 相同进程实例重试注册时，Yak Ops 可以复用原租约。

关闭：

1. 先向 Yak Ops 发送 deregister。
2. 再停止 Worker HTTP 服务。
3. 再关闭任务运行时和 Connector ClassLoader。

无法完成优雅注销时，Yak Ops 会在租约到期后自动将节点标记为 DOWN。

## 管理边界

Worker 拥有：

- advertised base URL
- instanceId 和引擎版本
- 容量与负载
- Connector 能力
- 租约续期

Yak Ops 拥有：

- 显示名称
- 标签的后续管理值
- 调度权重
- 启用、排空和禁用状态

首次动态注册会提交初始标签；后续心跳不会覆盖管理员在 Yak Ops 中调整后的标签。

## 安全建议

- 生产环境使用 HTTPS。
- 使用至少 32 字节随机共享密钥。
- 通过 Kubernetes Secret、Vault 或等价方案注入密钥。
- 不要把密钥写入镜像、启动日志或仓库。
- advertised URL 应限制在可信网络内，避免将 Worker 管理接口直接暴露到公网。
