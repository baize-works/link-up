# Link-Up Structured JobSpec

`JobSpec` is the control-plane protocol for submitting one offline batch synchronization job. It is independent of HOCON and independent of any frontend framework.

```json
{
  "apiVersion": "link-up/v1",
  "kind": "BatchSyncJob",
  "name": "orders-to-warehouse",
  "source": {
    "connectorId": "jdbc",
    "options": {
      "url": "jdbc:mysql://db:3306/source",
      "driver": "com.mysql.cj.jdbc.Driver",
      "table_path": "sales.orders",
      "fetch_size": 1000
    }
  },
  "sink": {
    "connectorId": "doris",
    "options": {
      "fenodes": "doris-fe:8030",
      "table": "warehouse.orders"
    }
  },
  "runtime": {
    "batchSize": 1000,
    "sourceParallelism": 2,
    "sinkParallelism": 2,
    "pipelineParallelism": 2,
    "maxBufferedBatches": 64
  }
}
```

## Ownership

- Connector option names and validation rules come from each Connector Schema.
- The control plane resolves data-source references and secrets before submission.
- Link-Up compiles `JobSpec` into its internal `JobDefinition` and owns final runtime validation.
- `connectorId` is opaque to the protocol. Adding Doris, HTTP or file connectors does not require a new control-plane serialization format.

## Compatibility

`application/hocon` and JSON requests containing `hocon` remain available for CLI and migration use. A JSON submission must contain exactly one of `jobSpec` or `hocon`. Control planes should use `jobSpec`.
