# 部署与运维指南

Link-Up 当前以**本地批处理进程**运行：一个命令执行一个同步作业，进程退出即代表作业结束。它不提供常驻 Server，因此不应使用后台 `start/stop` 脚本；应由 systemd、Kubernetes Job
或调度器负责重试、超时和退出码处理。

## 构建与安装

要求 JDK 8+、Maven 3.8.1+。在仓库根目录执行：

```bash
mvn --batch-mode clean verify

-- 跳过测试用例
mvn --batch-mode clean verify -Dmaven.test.skip=true
```

产物为 `link-up-dist/target/link-up-1.0.0.tar.gz` 和 `.zip`。解压后目录包含 `bin/`、`config/` 和运行时依赖 `lib/`：

```bash
tar -xzf link-up-dist/target/link-up-1.0.0.tar.gz
cd link-up-1.0.0
cp config/link-up.yaml config/orders-prod.yaml
# Edit URLs, table names, user and password. Do not commit the resulting file.
bin/link-up.sh --config config/orders-prod.yaml
```

配置文件扩展名可为 `.yaml`，但内容是 HOCON（与现有作业解析器一致），不是 YAML。启动器默认读取 `config/link-up.yaml`，因此 `bin/link-up.sh`
可直接执行示例配置。`--help` 和 `--version` 可用于探测安装是否完整。

## 配置与机密

`config/link-up.yaml` 包含 source/sink JDBC 示例、并行度和通道容量。复制后替换 `change-me`
，并将配置设为仅运行账户可读（例如 `chmod 600 config/orders-prod.yaml`）。推荐在 CI/CD 中由 Secret 渲染临时配置文件，或把只读 Secret 挂载到容器；不要把密码放在命令行或提交到
Git。可通过 `LINK_UP_CONF_DIR`、`LINK_UP_LOG_DIR`、`LINK_UP_HOME` 覆盖目录。

## JVM 与日志

默认 JVM 参数：`-Xms256m -Xmx1024m -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8`。使用 `LINK_UP_JAVA_OPTS` 追加容器内存比例、GC
或诊断参数，例如：

```bash
LINK_UP_JAVA_OPTS='-XX:+UseG1GC -XX:MaxRAMPercentage=70.0' bin/link-up.sh -c config/orders-prod.yaml
```

`config/log4j2.xml` 同时输出控制台和 `${LINK_UP_LOG_DIR:-logs}/link-up.log`。日志按日或 100 MB 滚动，最多保留 14 个归档。每次作业执行还会为每个
Source/Sink Task 创建独立日志，命名为 `job-<job-name>-<pipeline>-<task-type>-<subtask>-<run-id>.log`
（例如 `job-orders_sync-pipeline_source.orders-source-0-128392984.log`）。文件名中的不安全字符会替换为下划线；同一作业运行的 `run-id` 相同，便于按批次检索。Task
日志同样按 100 MB 滚动，最多保留 14 个归档。对容器运行，优先采集 stdout；需要落盘时挂载日志目录。

## Docker、上线与回滚

先构建分发包，再从仓库根目录构建镜像：

```bash
mvn -pl link-up-dist -am package
docker build -f deploy/docker/Dockerfile -t link-up:1.0.0 .
docker run --rm \
  -v /secure/orders-prod.yaml:/opt/link-up/config/job.yaml:ro \
  link-up:1.0.0 --config /opt/link-up/config/job.yaml
```

上线前应执行小表验证、确认目标端写入策略和重复运行行为、设置数据库连接/查询超时，并监控进程退出码、作业汇总指标与错误日志。批任务失败应由调度平台重试；对于非幂等 `APPEND_DATA`
作业，先修复或清理目标数据再重试。回滚使用上一版本的不可变压缩包/镜像和与之匹配的已验证配置，而不是覆盖运行中的目录。

## CI

GitHub Actions 会在 push 和 PR 上用 JDK 8 执行 `mvn verify`，并解压发行包运行 `--help` 冒烟检查。发布流水线应额外保存 tar.gz/zip、生成 SHA-512
校验和、对发布工件签名，并在部署前验证校验和。
