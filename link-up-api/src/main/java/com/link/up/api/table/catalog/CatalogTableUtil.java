package com.link.up.api.table.catalog;

import com.link.up.api.table.type.FluxRowType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CatalogTable 工具类。
 * <p>
 * 这里只提供无状态、无连接、无配置依赖的纯转换方法。
 */
public final class CatalogTableUtil {

    private CatalogTableUtil() {
    }

    /**
     * 根据 FluxRowType 创建最小表结构。
     */
    public static CatalogTable fromRowType(
            TablePath tablePath,
            FluxRowType rowType) {

        Objects.requireNonNull(
                tablePath,
                "tablePath must not be null");

        Objects.requireNonNull(
                rowType,
                "rowType must not be null");

        TableSchema.Builder schemaBuilder =
                TableSchema.builder();

        for (int i = 0;
             i < rowType.getFieldCount();
             i++) {

            schemaBuilder.column(
                    Column.builder(
                            rowType.getFieldName(i),
                            rowType.getFieldType(i))
                            .nullable(true)
                            .build());
        }

        return CatalogTable.builder(
                tablePath,
                schemaBuilder.build())
                .build();
    }

    /**
     * 使用新的 FluxRowType 更新 CatalogTable。
     * <p>
     * 同名字段保留原有长度、默认值、注释等元数据，
     * 只替换字段类型。
     */
    public static CatalogTable replaceRowType(
            CatalogTable catalogTable,
            FluxRowType rowType) {

        Objects.requireNonNull(
                catalogTable,
                "catalogTable must not be null");

        Objects.requireNonNull(
                rowType,
                "rowType must not be null");

        TableSchema oldSchema =
                catalogTable.getTableSchema();

        List<Column> finalColumns =
                new ArrayList<>(
                        rowType.getFieldCount());

        for (int i = 0;
             i < rowType.getFieldCount();
             i++) {

            String fieldName =
                    rowType.getFieldName(i);

            if (oldSchema.contains(fieldName)) {
                finalColumns.add(
                        oldSchema
                                .getColumn(fieldName)
                                .withType(
                                        rowType.getFieldType(i)));
            } else {
                finalColumns.add(
                        Column.builder(
                                fieldName,
                                rowType.getFieldType(i))
                                .nullable(true)
                                .build());
            }
        }

        PrimaryKey primaryKey =
                oldSchema.getPrimaryKey();

        if (primaryKey != null) {
            List<String> finalFieldNames =
                    new ArrayList<>(
                            rowType.getFieldCount());

            for (int i = 0;
                 i < rowType.getFieldCount();
                 i++) {

                finalFieldNames.add(
                        rowType.getFieldName(i));
            }

            if (!finalFieldNames.containsAll(
                    primaryKey.getColumnNames())) {

                primaryKey = null;
            }
        }

        TableSchema finalSchema =
                TableSchema.builder()
                        .columns(finalColumns)
                        .primaryKey(primaryKey)
                        .build();

        return catalogTable.withSchema(
                finalSchema);
    }
}
