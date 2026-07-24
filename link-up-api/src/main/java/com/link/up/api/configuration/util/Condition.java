package com.link.up.api.configuration.util;

import com.link.up.api.configuration.Option;

import java.util.*;

/**
 * 配置条件，支持 AND 和 OR 链式组合。
 *
 * @param <T> 配置值类型
 */
public final class Condition<T> {

    private final Option<T> option;
    private final Object expectedValue;
    private final ConditionOperator operator;
    private final Option<?> compareOption;
    private final ConditionExtension<T> extension;

    private Boolean and;
    private Condition<?> next;

    Condition(
            Option<T> option,
            Object expectedValue) {

        this(
                option,
                ConditionOperator.EQUAL,
                expectedValue,
                null,
                null);
    }

    Condition(
            Option<T> option,
            ConditionOperator operator,
            Object expectedValue,
            Option<?> compareOption) {

        this(
                option,
                operator,
                expectedValue,
                compareOption,
                null);
    }

    Condition(
            Option<T> option,
            ConditionOperator operator,
            Object expectedValue,
            Option<?> compareOption,
            ConditionExtension<T> extension) {

        this.option = Objects.requireNonNull(
                option,
                "Condition option must not be null");

        this.operator = Objects.requireNonNull(
                operator,
                "Condition operator must not be null");

        if (operator.getSource() == ConditionOperator.Source.FIELD
                && compareOption == null) {
            throw new IllegalArgumentException(
                    "Field comparison requires compareOption");
        }

        if (operator.getSource() != ConditionOperator.Source.FIELD
                && compareOption != null) {
            throw new IllegalArgumentException(
                    "compareOption is only supported by field comparison");
        }

        if (operator == ConditionOperator.EXTENSION
                && extension == null) {
            throw new IllegalArgumentException(
                    "EXTENSION requires ConditionExtension");
        }

        boolean requiresValue =
                operator.getSource() == ConditionOperator.Source.LITERAL
                        && operator.getArity() == ConditionOperator.Arity.BINARY
                        && operator != ConditionOperator.EQUAL
                        && operator != ConditionOperator.NOT_EQUAL;

        if (requiresValue && expectedValue == null) {
            throw new IllegalArgumentException(
                    String.format(
                            "Operator %s requires expectedValue",
                            operator.name()));
        }

        this.expectedValue = expectedValue;
        this.compareOption = compareOption;
        this.extension = extension;
    }

    public static <T> Condition<T> of(
            Option<T> option,
            Object expectedValue) {

        return new Condition<>(option, expectedValue);
    }

    public static <T> Condition<T> of(
            Option<T> option,
            ConditionOperator operator,
            Object expectedValue) {

        return new Condition<>(
                option,
                operator,
                expectedValue,
                null);
    }

    private static String conditionToString(Condition<?> condition) {
        ConditionOperator operator = condition.operator;
        String key = "'" + condition.option.key() + "'";

        if (operator == ConditionOperator.EXTENSION) {
            return key + " " + condition.extension.description();
        }

        if (operator.getSource() == ConditionOperator.Source.FIELD) {
            return key
                    + " "
                    + operator.getSymbol()
                    + " '"
                    + condition.compareOption.key()
                    + "'";
        }

        if (operator.getArity() == ConditionOperator.Arity.UNARY) {
            return key + " " + operator.getSymbol();
        }

        return key
                + " "
                + operator.getSymbol()
                + " "
                + formatValue(condition.expectedValue);
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }

    public Option<T> getOption() {
        return option;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    /**
     * 保留旧方法名，减少原调用代码改动。
     */
    public Object getExpectValue() {
        return expectedValue;
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public Option<?> getCompareOption() {
        return compareOption;
    }

    public ConditionExtension<T> getExtension() {
        return extension;
    }

    public Condition<?> getNext() {
        return next;
    }

    public Boolean and() {
        return and;
    }

    public boolean hasNext() {
        return next != null;
    }

    public <E> Condition<T> and(
            Option<E> option,
            Object expectedValue) {

        return and(Condition.of(option, expectedValue));
    }

    public <E> Condition<T> or(
            Option<E> option,
            Object expectedValue) {

        return or(Condition.of(option, expectedValue));
    }

    public Condition<T> and(Condition<?> condition) {
        addCondition(true, condition);
        return this;
    }

    public Condition<T> or(Condition<?> condition) {
        addCondition(false, condition);
        return this;
    }

    private void addCondition(
            boolean and,
            Condition<?> condition) {

        Objects.requireNonNull(condition, "condition");

        Set<Condition<?>> currentNodes =
                Collections.newSetFromMap(
                        new IdentityHashMap<>());

        Condition<?> current = this;
        while (current != null) {
            if (!currentNodes.add(current)) {
                throw new IllegalArgumentException(
                        "Circular condition chain detected");
            }
            current = current.next;
        }

        Set<Condition<?>> newNodes =
                Collections.newSetFromMap(
                        new IdentityHashMap<>());

        current = condition;
        while (current != null) {
            if (currentNodes.contains(current)
                    || !newNodes.add(current)) {
                throw new IllegalArgumentException(
                        "Circular condition chain detected near option '"
                                + current.option.key()
                                + "'");
            }
            current = current.next;
        }

        Condition<?> tail = getTailCondition();
        tail.and = and;
        tail.next = condition;
    }

    int getCount() {
        int count = 0;
        Condition<?> current = this;

        while (current != null) {
            count++;
            current = current.next;
        }

        return count;
    }

    private Condition<?> getTailCondition() {
        Condition<?> current = this;

        while (current.next != null) {
            current = current.next;
        }

        return current;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Condition)) {
            return false;
        }

        Condition<?> that = (Condition<?>) obj;
        return Objects.equals(option, that.option)
                && Objects.equals(expectedValue, that.expectedValue)
                && operator == that.operator
                && Objects.equals(compareOption, that.compareOption)
                && Objects.equals(extension, that.extension)
                && Objects.equals(and, that.and)
                && Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                option,
                expectedValue,
                operator,
                compareOption,
                extension,
                and,
                next);
    }

    /**
     * 按 AND 优先级输出条件表达式。
     */
    @Override
    public String toString() {
        List<String> segments = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();

        Condition<?> current = this;

        while (current != null) {
            StringBuilder segment = new StringBuilder();
            int size = 0;

            while (current != null) {
                if (size > 0) {
                    segment.append(" && ");
                }

                segment.append(conditionToString(current));
                size++;

                if (!current.hasNext()) {
                    current = null;
                    break;
                }

                boolean isAnd = Boolean.TRUE.equals(current.and);
                current = current.next;

                if (!isAnd) {
                    break;
                }
            }

            segments.add(segment.toString());
            sizes.add(size);
        }

        if (segments.size() == 1) {
            return segments.get(0);
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                result.append(" || ");
            }

            if (sizes.get(i) > 1) {
                result.append("(")
                        .append(segments.get(i))
                        .append(")");
            } else {
                result.append(segments.get(i));
            }
        }

        return result.toString();
    }
}