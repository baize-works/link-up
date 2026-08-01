# Link-Up

Link-Up is a local, batch-oriented data synchronization engine. It currently provides JDBC source and sink
connectors, HOCON job parsing, parallel source/sink execution, and execution metrics.

## Module architecture

The project is organized around dependency direction instead of technical catch-all modules:

```text
link-up-api          public connector, table, configuration, and error contracts
        ↑
link-up-framework    job planning, scheduling, channels, execution, and metrics
        ↑
link-up-connectors   connector implementations (for example, JDBC)
        ↑
link-up-launcher     executable assembly and local CLI entry point
```

`link-up-api` is the only extension-facing dependency. It owns the shared error contract
(`FluxRuntimeException`, `FluxErrorCode`, and API error codes), removing the former generic
`common` module. `link-up-framework` is the runtime module (the renamed and consolidated core/engine responsibility);
connectors depend on API contracts but not on framework internals. The connector aggregator contains no inherited
runtime dependencies—each connector declares the libraries it needs explicitly.

## Quick start

Build the complete project and its deployable archives with JDK 8+ and Maven 3.8.1+:

```bash
mvn --batch-mode clean verify
```

Run directly from source:

```bash
mvn -pl link-up-launcher -am compile exec:java \
  -Dexec.mainClass=com.link.up.launcher.LocalSyncLauncher \
  -Dexec.args=link-up-launcher/examples/jdbc-single-table.conf
```

Or extract `link-up-dist/target/link-up-1.0.0.tar.gz`, copy and edit `config/link-up.yaml`, then run:

```bash
bin/link-up.sh --config config/link-up.yaml
```

The configuration file has a `.yaml` deployment-friendly name but uses HOCON syntax. Do not commit real JDBC
credentials. See the [deployment and operations guide](docs/deployment.md) for packaging, CI, Docker, JVM options,
Log4j2, security, rollback, and production guidance.

## Offline Worker protocol

`link-up-server` is a single-node, offline-only execution Worker. Its lifecycle is:

```text
CREATED -> SUBMITTED -> QUEUED -> RUNNING
                                  -> SUCCEEDED / FAILED / CANCELED / LOST
```

The JSON submit protocol supports control-plane `externalExecutionId`, `idempotencyKey`, definition versioning,
auditable state transitions, worker instance identity and deterministic duplicate submission handling. The previous
HOCON body submission remains available for CLI and compatibility use.

See [the single-node offline Worker protocol](docs/worker-protocol.md) for the complete API and state ownership contract.
