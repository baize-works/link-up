# Launcher examples

These HOCON files are executable JDBC sync templates. Copy one, set the JDBC URLs, credentials, and table names, then
run it from the repository root:

```bash
mvn -pl link-up-launcher -am compile exec:java \
  -Dexec.mainClass=com.link.up.launcher.LocalSyncLauncher \
  -Dexec.args=link-up-launcher/examples/jdbc-single-table.conf
```

`jdbc-single-table.conf` copies one source table to a fixed destination table.
`jdbc-multi-table.conf` copies two tables concurrently and uses the connector's
`${table_name}` placeholder to generate a destination table per source table.

The launcher also accepts an inline HOCON string, but passing a configuration file is recommended because it keeps
credentials and table configuration out of shell history.

On completion it prints aggregate throughput, record/byte counts, failures, retries, SQL and commit timing, plus
per-task and per-channel metrics. Do not commit real credentials in copied configuration files.
