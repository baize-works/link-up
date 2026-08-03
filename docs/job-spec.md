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
  "mapping": {
    "columns": [
      {"source": "order_id", "target": "id"},
      {"source": "created_at", "target": "create_time"}
    ]
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

## Column mapping

`mapping.columns` is an optional fixed column contract. It is not a general transform or expression language.

- Each item copies one source column into one target column.
- Array order defines the output row and target write order.
- Unmapped source columns are not emitted to the sink.
- Source and target names must be unique inside one mapping.
- Link-Up validates source fields after metadata discovery, projects each `FluxRow` by precomputed indexes, and supplies the renamed output schema to sink preparation.
- When all source primary-key columns are retained, their names are rewritten to the corresponding target names.
- Explicit mapping currently requires exactly one source table.
- An absent mapping preserves the original schema and row order and is omitted from canonical protocol JSON for retry compatibility.

## Ownership

- Connector option names and validation rules come from each Connector Schema.
- The control plane resolves data-source references and secrets before submission.
- Link-Up compiles `JobSpec` into its internal `JobDefinition` and owns final runtime validation.
- `connectorId` is opaque to the protocol. Adding Doris, HTTP or file connectors does not require a new control-plane serialization format.
- Column mapping belongs to the Link-Up framework and is not duplicated inside individual connectors.

## Compatibility

`application/hocon` and JSON requests containing `hocon` remain available for CLI and migration use. A JSON submission must contain exactly one of `jobSpec` or `hocon`. Control planes should use `jobSpec`.
