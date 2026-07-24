package com.link.up.api.table.catalog;

import com.link.up.api.table.type.BasicType;
import com.link.up.api.table.type.DecimalType;
import org.junit.Test;

import static org.junit.Assert.*;

public class SchemaCompatibilityReportTest {

    @Test
    public void matchesColumnsByNameRatherThanPositionAndIgnoresCase() {
        TableSchema source = TableSchema.builder()
                .column(Column.builder("ID", BasicType.INT_TYPE).nullable(false).build())
                .column(Column.builder("payload", BasicType.STRING_TYPE).length(100L).build())
                .build();
        TableSchema target = TableSchema.builder()
                .column(Column.builder("payload", BasicType.STRING_TYPE).length(200L).build())
                .column(Column.builder("id", BasicType.LONG_TYPE).nullable(false).build())
                .build();

        SchemaCompatibilityReport report = SchemaCompatibilityReport.compare(source, target);

        assertTrue(report.isCompatible());
        assertEquals("id", report.getSourceToTarget().get("ID"));
        assertTrue(report.getWarnings().get(0).contains("case differs"));
    }

    @Test
    public void reportsDecimalBinaryAndRequiredColumnHazards() {
        TableSchema source = TableSchema.builder()
                .column(Column.builder("amount", new DecimalType(20, 6)).precision(20).scale(6).build())
                .column(Column.builder("blob", BasicType.BYTES_TYPE).length(4096L).build())
                .build();
        TableSchema target = TableSchema.builder()
                .column(Column.builder("amount", new DecimalType(10, 2)).precision(10).scale(2).build())
                .column(Column.builder("blob", BasicType.BYTES_TYPE).length(1024L).build())
                .column(Column.builder("required", BasicType.INT_TYPE).nullable(false).build())
                .build();

        SchemaCompatibilityReport report = SchemaCompatibilityReport.compare(source, target);

        assertFalse(report.isCompatible());
        assertEquals(3, report.getIncompatibleColumns().size());
    }

    @Test
    public void reportsMissingColumnsForSchemaEvolution() {
        TableSchema source = TableSchema.builder()
                .column(Column.builder("new_column", BasicType.STRING_TYPE).build()).build();
        TableSchema target = TableSchema.builder()
                .column(Column.builder("id", BasicType.INT_TYPE).build()).build();

        SchemaCompatibilityReport report = SchemaCompatibilityReport.compare(source, target);

        assertEquals(1, report.getMissingTargetColumns().size());
        assertEquals("new_column", report.getMissingTargetColumns().get(0).getName());
    }
}
