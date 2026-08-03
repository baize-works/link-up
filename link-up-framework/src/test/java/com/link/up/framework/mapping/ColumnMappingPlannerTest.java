package com.link.up.framework.mapping;

import com.link.up.api.job.JobSpec;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.catalog.PrimaryKey;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.catalog.TableSchema;
import com.link.up.api.table.type.BasicType;
import com.link.up.api.table.type.FluxRow;
import com.link.up.framework.job.ColumnMapping;
import com.link.up.framework.job.JobDefinition;
import com.link.up.framework.job.JobSpecCompiler;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ColumnMappingPlannerTest {

    @Test
    public void compilesMappedSchemaAndProjectsRowsInTargetOrder() {
        JobSpec spec = baseSpec();
        JobSpec.Mapping mapping = new JobSpec.Mapping();
        mapping.setColumns(Arrays.asList(
                column("name", "user_name"),
                column("id", "user_id")));
        spec.setMapping(mapping);

        JobDefinition definition = new JobSpecCompiler().compile(spec);
        TablePath path = TablePath.of("demo", "users");
        TableSchema sourceSchema = TableSchema.builder()
                .column(Column.builder("id", BasicType.LONG_TYPE).nullable(false).build())
                .column(Column.builder("name", BasicType.STRING_TYPE).build())
                .column(Column.builder("age", BasicType.INT_TYPE).build())
                .primaryKey(PrimaryKey.of("pk_users", Arrays.asList("id")))
                .build();
        Map<TablePath, CatalogTable> sourceTables =
                new LinkedHashMap<TablePath, CatalogTable>();
        sourceTables.put(path, CatalogTable.builder(path, sourceSchema).build());

        ColumnMappingPlanner.Result result =
                new ColumnMappingPlanner().plan(sourceTables, definition.getColumnMapping());
        CatalogTable output = result.getOutputTables().get(path);

        assertEquals("user_name", output.getTableSchema().getColumn(0).getName());
        assertEquals("user_id", output.getTableSchema().getColumn(1).getName());
        assertEquals(Arrays.asList("user_id"),
                output.getTableSchema().getPrimaryKey().getColumnNames());

        FluxRow projected = result.getPlans().get(path)
                .project(Arrays.asList(FluxRow.of(7L, "Alice", 30)))
                .get(0);
        assertEquals("Alice", projected.getField(0));
        assertEquals(7L, projected.getField(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDuplicateTargetColumns() {
        JobSpec spec = baseSpec();
        JobSpec.Mapping mapping = new JobSpec.Mapping();
        mapping.setColumns(Arrays.asList(
                column("id", "target_id"),
                column("name", "target_id")));
        spec.setMapping(mapping);

        new JobSpecCompiler().compile(spec);
    }

    private JobSpec baseSpec() {
        JobSpec spec = new JobSpec();
        spec.setName("mapped-users");
        spec.setSource(connector("jdbc"));
        spec.setSink(connector("jdbc"));
        return spec;
    }

    private JobSpec.Connector connector(String connectorId) {
        JobSpec.Connector connector = new JobSpec.Connector();
        connector.setConnectorId(connectorId);
        return connector;
    }

    private JobSpec.Column column(String source, String target) {
        JobSpec.Column column = new JobSpec.Column();
        column.setSource(source);
        column.setTarget(target);
        return column;
    }
}
