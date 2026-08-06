# Link-Up 单节点离线 Worker 执行协议

Link-Up Server 是一个只执行离线批量同步任务的单节点 Worker。Yak Ops 等控制面负责任务定义、调度、执行历史、重试和告警；Link-Up 负责接收一次执行命令、排队、运行、取消并返回最终结果。

## Worker 身份

```http
GET /api/v1/node
```

响应包含稳定的 `nodeId`、每次启动变化的 `instanceId`、容量、负载和完整生命周期。控制面必须保存提交时的 `workerInstanceId`。如果同一 `nodeId` 返回了新的 `instanceId`，控制面应将旧实例中仍处于非终态的任务标记为 `LOST`。

## 状态机

```text
CREATED
   ↓
SUBMITTED
   ↓
QUEUED
   ↓
RUNNING
   ├── SUCCEEDED
   ├── FAILED
   ├── CANCELED
   └── LOST
```

终态不可再次转换。每次状态转换都会增加 `stateVersion`，并记录在 `transitions` 中。取消请求不会引入额外的 `CANCELLING` 业务状态；`cancellationRequested=true` 表示取消意图已被接收，实际状态仍保持 `QUEUED` 或 `RUNNING`，直到进入终态。

## 结构化 JSON 提交协议

```http
POST /api/v1/jobs
Content-Type: application/json
```

```json
{
  "externalExecutionId": "yak-execution-10086",
  "idempotencyKey": "60af452d-813c-4e51-87d9-4b00b5e2b53f",
  "definitionVersion": 3,
  "jobSpec": {
    "apiVersion": "link-up/v1",
    "kind": "BatchSyncJob",
    "name": "orders-sync",
    "source": {
      "connectorId": "jdbc",
      "options": {
        "url": "jdbc:mysql://source:3306/demo",
        "table_path": "orders"
      }
    },
    "sink": {
      "connectorId": "jdbc",
      "options": {
        "url": "jdbc:mysql://sink:3306/demo",
        "table_path": "orders_copy"
      }
    },
    "runtime": {
      "batchSize": 1000,
      "sourceParallelism": 1,
      "sinkParallelism": 1,
      "pipelineParallelism": 1,
      "maxBufferedBatches": 64
    }
  }
}
```

Worker 会对规范化后的 `jobSpec` 计算配置摘要。相同 `externalExecutionId`、`idempotencyKey`、`definitionVersion` 和配置摘要的重复请求返回同一个 `jobId`；复用相同标识但提交不同内容时返回 HTTP `409` 和错误码 `FLUX-JOB-IDEMPOTENCY-CONFLICT`。

JSON 请求必须且只能包含 `jobSpec` 或 `hocon` 其中一个。`hocon`、`application/hocon` 和 `text/plain` 仍保留给 CLI 与迁移场景，控制面必须使用结构化 `jobSpec`。完整字段说明见 `docs/job-spec.md`。

提交入口只记录 `externalExecutionId`、定义版本、任务名称和 Connector 类型等摘要，不记录完整 JobSpec、用户名、密码、Token 或其他 Connector options。

## 查询与取消

```http
GET /api/v1/jobs/{jobId}
GET /api/v1/jobs/external/{externalExecutionId}
GET /api/v1/jobs?externalExecutionId={externalExecutionId}
GET /api/v1/jobs?status=RUNNING&page=1&pageSize=20
GET /api/v1/jobs/{jobId}/pipelines
GET /api/v1/jobs/{jobId}/tasks
GET /api/v1/jobs/{jobId}/metrics
GET /api/v1/jobs/{jobId}/logs?cursor=0&limit=500
DELETE /api/v1/jobs/{jobId}
```

提交请求超时后，控制面应优先使用 `externalExecutionId` 查询，不能直接生成新执行实例重复提交。

## 增量运行日志

每个 Framework Run 生成一个文件系统安全的 `runId` 和独立日志文件。Connector 准备、自动建表、Split 规划、Pipeline 和 Task 执行共享同一日志上下文。

日志接口使用不透明的字节偏移游标：

```json
{
  "jobId": "flux-1785977590967-1",
  "externalExecutionId": "yak-offline-21d1f420-c9e7-4d3b-a5c7-87e52899958c",
  "runId": "MYSQL_MYSQL_-1785977593842",
  "items": [
    {
      "sequence": 0,
      "timestampMillis": 1785977593855,
      "source": "LINK_UP",
      "level": "INFO",
      "thread": "link-up-job-1",
      "logger": "c.l.u.f.e.JobExecution",
      "message": "Job started: jobName=MYSQL → MYSQL 离线同步"
    }
  ],
  "nextCursor": 226,
  "completed": false
}
```

- 首次读取使用 `cursor=0`；后续请求原样传回 `nextCursor`。
- `limit` 范围为 1～1000，按日志事件计数；多行 SQL 和异常堆栈会归入同一条事件。
- `completed=true` 表示 Job 已进入终态，并且游标已经读取到当前日志文件末尾。
- 接口只允许读取 Worker 自己记录的文件名，不接受任意文件路径。
- Job 日志仍受 Log4j RollingFile 的保留和归档策略约束；控制面需要长期审计时，应在执行完成后主动归档或采集。

## 离线建表结果

JDBC Sink 在准备每个数据集时会生成最终目标表的 `CREATE TABLE` 语句。单表任务包含一个 Pipeline，多表任务按数据集包含多个 Pipeline；两种模式使用相同结构：

```json
{
  "pipelineId": "pipeline-orders",
  "dataSetId": "source_db.orders",
  "source": {
    "table": "source_db.orders"
  },
  "sink": {
    "table": "target_db.orders"
  },
  "tableDdl": {
    "dialect": "MYSQL",
    "sourceTable": "source_db.orders",
    "targetTable": "target_db.orders",
    "createTableSql": "CREATE TABLE `target_db`.`orders` (...);",
    "executed": true,
    "status": "SUCCEEDED",
    "reason": "TARGET_TABLE_CREATED",
    "durationMillis": 18
  }
}
```

`tableDdl` 只描述离线任务准备阶段生成的建表语句，不表示 CDC 或实时 Schema Change：

- `executed=true` 表示本次确实执行了建表；
- 目标表已经存在时，`executed=false`、`status=SKIPPED`、`reason=TARGET_TABLE_ALREADY_EXISTS`；
- 即使本次未执行，仍返回按最终目标表结构生成的 `createTableSql`，供控制面查看和审计；
- DDL 按 Pipeline 返回，因此多表任务不会把不同目标表的建表语句混在一起。

## LOST 处理

`LOST` 表示最终结果未知，不等价于失败或取消：

1. Worker 优雅关闭时先请求取消。
2. 在关闭超时内完成的任务进入实际终态。
3. 关闭超时后仍未完成的任务进入 `LOST`。
4. 如果进程被直接杀死，控制面通过 `workerInstanceId` 变化或节点失联超时，将旧实例的非终态任务标记为 `LOST`。
5. 对 `LOST` 的重试必须创建新的控制面执行实例和新的 `externalExecutionId`，不能修改原执行记录。

## 控制面轮询建议

- `SUBMITTED`、`QUEUED`：每 2～3 秒查询。
- `RUNNING`：每 3～5 秒查询，长任务可退避到 15～30 秒。
- 运行日志使用 `nextCursor` 增量读取，不要每次从 0 重放完整文件。
- 终态：完成最后一次日志读取后停止轮询。
- 更新前比较 `stateVersion`，只接受更高版本的状态。
