package com.link.up.launcher;

/**
 * JDBC 多表同步示例。
 */
public final class JdbcMultiTableSyncExample {

    private static final String EXAMPLE_HOCON =
            "job {\n"
                    + "  name = \"jdbc-multi-table-sync\"\n"
                    + "}\n"
                    + "\n"
                    + "env {\n"
                    + "  pipeline-parallelism = 2\n"
                    + "  source-parallelism = 2\n"
                    + "  sink-parallelism = 2\n"
                    + "  channel-capacity = 32\n"
                    + "}\n"
                    + "\n"
                    + "source {\n"
                    + "  type = \"jdbc\"\n"
                    + "  batch-size = 1000\n"
                    + "\n"
                    + "  url = \"jdbc:mysql://127.0.0.1:3306/flux_test"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&characterEncoding=UTF-8\"\n"
                    + "\n"
                    + "  driver = \"com.mysql.cj.jdbc.Driver\"\n"
                    + "  user = \"root\"\n"
                    + "  password = \"123456\"\n"
                    + "  fetch_size = 1000\n"
                    + "\n"
                    + "  table_list = [\n"
                    + "    { table_path = \"flux_test.user_info\" },\n"
                    + "    { table_path = \"flux_test.user_info_copy\" }\n"
                    + "  ]\n"
                    + "}\n"
                    + "\n"
                    + "sink {\n"
                    + "  type = \"jdbc\"\n"
                    + "\n"
                    + "  url = \"jdbc:mysql://127.0.0.1:3306/test1"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&characterEncoding=UTF-8\"\n"
                    + "\n"
                    + "  driver = \"com.mysql.cj.jdbc.Driver\"\n"
                    + "  user = \"root\"\n"
                    + "  password = \"123456\"\n"
                    + "\n"
                    + "  table = \"test1.sink_${table_name}\"\n"
                    + "  schema_save_mode = CREATE_SCHEMA_WHEN_NOT_EXIST\n"
                    + "  data_save_mode = DROP_DATA\n"
                    + "}\n";

    private JdbcMultiTableSyncExample() {
    }

    public static void main(String[] args)
            throws Exception {

        if (args.length > 1) {
            throw new IllegalArgumentException(
                    "Usage: JdbcMultiTableSyncExample "
                            + "[\"<HOCON job configuration>\"]");
        }

        String hocon =
                args.length == 1
                        ? args[0]
                        : EXAMPLE_HOCON;

        LocalSyncLauncher.run(
                hocon,
                Thread.currentThread()
                        .getContextClassLoader());
    }
}