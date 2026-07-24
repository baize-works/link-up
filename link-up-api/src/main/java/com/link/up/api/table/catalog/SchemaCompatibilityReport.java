package com.link.up.api.table.catalog;

import com.link.up.api.table.type.SqlType;
import com.link.up.api.table.type.TypeUtil;

import java.io.Serializable;
import java.util.*;

/**
 * Compares a source schema with a target schema before data is written.
 *
 * <p>The report deliberately matches names case-insensitively.  JDBC metadata
 * is not consistent about identifier casing and positional matching can write
 * values into the wrong columns when a target has extra columns.</p>
 */
public final class SchemaCompatibilityReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Column> missingTargetColumns;
    private final List<String> incompatibleColumns;
    private final List<String> warnings;
    private final Map<String, String> sourceToTarget;

    private SchemaCompatibilityReport(List<Column> missing, List<String> incompatible,
                                      List<String> warnings, Map<String, String> mapping) {
        this.missingTargetColumns = Collections.unmodifiableList(new ArrayList<>(missing));
        this.incompatibleColumns = Collections.unmodifiableList(new ArrayList<>(incompatible));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        this.sourceToTarget = Collections.unmodifiableMap(new LinkedHashMap<>(mapping));
    }

    public static SchemaCompatibilityReport compare(TableSchema source, TableSchema target) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Map<String, Column> targetByName = new HashMap<>();
        for (Column column : target.getColumns()) {
            targetByName.put(normalize(column.getName()), column);
        }
        List<Column> missing = new ArrayList<>();
        List<String> incompatible = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> mapping = new LinkedHashMap<>();
        for (Column sourceColumn : source.getColumns()) {
            Column targetColumn = targetByName.get(normalize(sourceColumn.getName()));
            if (targetColumn == null) {
                missing.add(sourceColumn);
                continue;
            }
            mapping.put(sourceColumn.getName(), targetColumn.getName());
            if (!isCompatible(sourceColumn, targetColumn)) {
                incompatible.add(describe(sourceColumn, targetColumn));
            }
            if (sourceColumn.isNullable() && !targetColumn.isNullable()) {
                warnings.add("source column '" + sourceColumn.getName()
                        + "' is nullable but target is NOT NULL");
            }
            if (!sourceColumn.getName().equals(targetColumn.getName())) {
                warnings.add("column case differs: source '" + sourceColumn.getName()
                        + "', target '" + targetColumn.getName() + "'");
            }
        }
        for (Column targetColumn : target.getColumns()) {
            if (!targetColumn.isNullable() && targetColumn.getDefaultValue() == null
                    && !targetColumn.isAutoIncrement()
                    && !targetColumn.getAttributes().containsKey("generated")) {
                if (!mapping.containsValue(targetColumn.getName())) {
                    incompatible.add("target required column '" + targetColumn.getName()
                            + "' is absent from source and has no default");
                }
            }
        }
        return new SchemaCompatibilityReport(missing, incompatible, warnings, mapping);
    }

    private static boolean isCompatible(Column source, Column target) {
        if (!TypeUtil.canConvert(source.getDataType(), target.getDataType())) return false;
        if (source.getDataType().getSqlType() == SqlType.DECIMAL) {
            if (source.getPrecision() != null && target.getPrecision() != null
                    && source.getPrecision() > target.getPrecision()) return false;
            if (source.getScale() != null && target.getScale() != null
                    && source.getScale() > target.getScale()) return false;
        }
        if ((source.getDataType().getSqlType() == SqlType.STRING || source.getDataType().getSqlType() == SqlType.BYTES)
                && source.getLength() != null && target.getLength() != null
                && source.getLength() > target.getLength()) return false;
        String sourceUnsigned = source.getAttributes().get("unsigned");
        String targetUnsigned = target.getAttributes().get("unsigned");
        return !("true".equalsIgnoreCase(sourceUnsigned) && !"true".equalsIgnoreCase(targetUnsigned));
    }

    private static String describe(Column source, Column target) {
        return "source column '" + source.getName() + "' (" + source.getSourceType()
                + ") is incompatible with target column '" + target.getName() + "' ("
                + target.getSourceType() + ")";
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public List<Column> getMissingTargetColumns() {
        return missingTargetColumns;
    }

    public List<String> getIncompatibleColumns() {
        return incompatibleColumns;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public Map<String, String> getSourceToTarget() {
        return sourceToTarget;
    }

    public boolean isCompatible() {
        return incompatibleColumns.isEmpty();
    }
}
