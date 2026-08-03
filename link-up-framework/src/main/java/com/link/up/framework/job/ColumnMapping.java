package com.link.up.framework.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Fixed column selection, ordering and rename contract.
 *
 * <p>This is deliberately not a general transform model. Each item copies one
 * source column into one target column, and item order defines the output row
 * order.</p>
 */
public final class ColumnMapping {

    private static final ColumnMapping EMPTY =
            new ColumnMapping(Collections.<Item>emptyList());

    private final List<Item> columns;

    public ColumnMapping(List<Item> columns) {
        List<Item> safe = new ArrayList<Item>();
        if (columns != null) {
            for (Item item : columns) {
                safe.add(Objects.requireNonNull(item, "mapping item must not be null"));
            }
        }
        this.columns = Collections.unmodifiableList(safe);
    }

    public static ColumnMapping empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return columns.isEmpty();
    }

    public List<Item> getColumns() {
        return columns;
    }

    public static final class Item {
        private final String source;
        private final String target;

        public Item(String source, String target) {
            this.source = requireText(source, "source");
            this.target = requireText(target, "target");
        }

        public String getSource() {
            return source;
        }

        public String getTarget() {
            return target;
        }

        private static String requireText(String value, String name) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("mapping." + name + " must not be blank");
            }
            return value.trim();
        }
    }
}
