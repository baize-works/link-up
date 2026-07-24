package com.link.up.api.configuration.util;

import com.link.up.api.configuration.ReadonlyConfig;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.PatternSyntaxException;

/**
 * 条件执行器。
 */
public final class ConditionEvaluators {

    private static final Map<ConditionOperator, Evaluator> REGISTRY =
            createRegistry();

    private ConditionEvaluators() {
    }

    static boolean evaluate(
            Condition<?> condition,
            ReadonlyConfig config) {

        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(config, "config");

        ConditionOperator operator = condition.getOperator();
        Evaluator evaluator = REGISTRY.get(operator);

        if (evaluator == null) {
            throw new OptionValidationException(
                    "Missing evaluator for operator %s",
                    operator);
        }

        Object value = config.get(condition.getOption());
        return evaluator.evaluate(value, condition, config);
    }

    @SuppressWarnings("unchecked")
    private static Map<ConditionOperator, Evaluator> createRegistry() {
        Map<ConditionOperator, Evaluator> registry =
                new EnumMap<>(ConditionOperator.class);

        registry.put(
                ConditionOperator.EQUAL,
                (value, condition, config) ->
                        Objects.equals(
                                condition.getExpectedValue(),
                                value));

        registry.put(
                ConditionOperator.NOT_EQUAL,
                (value, condition, config) ->
                        !Objects.equals(
                                condition.getExpectedValue(),
                                value));

        registry.put(
                ConditionOperator.GREATER_THAN,
                (value, condition, config) ->
                        value != null
                                && compareValues(
                                value,
                                condition.getExpectedValue()) > 0);

        registry.put(
                ConditionOperator.GREATER_OR_EQUAL,
                (value, condition, config) ->
                        value != null
                                && compareValues(
                                value,
                                condition.getExpectedValue()) >= 0);

        registry.put(
                ConditionOperator.LESS_THAN,
                (value, condition, config) ->
                        value != null
                                && compareValues(
                                value,
                                condition.getExpectedValue()) < 0);

        registry.put(
                ConditionOperator.LESS_OR_EQUAL,
                (value, condition, config) ->
                        value != null
                                && compareValues(
                                value,
                                condition.getExpectedValue()) <= 0);

        registry.put(
                ConditionOperator.NOT_BLANK,
                (value, condition, config) ->
                        value instanceof String
                                && !((String) value).trim().isEmpty());

        registry.put(
                ConditionOperator.STARTS_WITH,
                (value, condition, config) ->
                        value instanceof String
                                && ((String) value).startsWith(
                                String.valueOf(
                                        condition.getExpectedValue())));

        registry.put(
                ConditionOperator.CONTAINS,
                (value, condition, config) ->
                        value instanceof String
                                && ((String) value).contains(
                                String.valueOf(
                                        condition.getExpectedValue())));

        registry.put(
                ConditionOperator.MATCHES,
                (value, condition, config) -> {
                    if (!(value instanceof String)) {
                        return false;
                    }

                    try {
                        return ((String) value).matches(
                                String.valueOf(
                                        condition.getExpectedValue()));
                    } catch (PatternSyntaxException e) {
                        throw new OptionValidationException(
                                "Invalid regular expression: %s",
                                condition.getExpectedValue());
                    }
                });

        registry.put(
                ConditionOperator.UPPER_CASE,
                (value, condition, config) ->
                        value instanceof String
                                && value.equals(
                                ((String) value)
                                        .toUpperCase(Locale.ROOT)));

        registry.put(
                ConditionOperator.LOWER_CASE,
                (value, condition, config) ->
                        value instanceof String
                                && value.equals(
                                ((String) value)
                                        .toLowerCase(Locale.ROOT)));

        registry.put(
                ConditionOperator.NOT_EMPTY,
                (value, condition, config) ->
                        value instanceof Collection
                                && !((Collection<?>) value).isEmpty());

        registry.put(
                ConditionOperator.COLLECTION_UNIQUE,
                (value, condition, config) -> {
                    if (!(value instanceof Collection)) {
                        return false;
                    }

                    Collection<?> collection =
                            (Collection<?>) value;

                    return collection.size()
                            == new HashSet<>(collection).size();
                });

        registry.put(
                ConditionOperator.MAP_NOT_EMPTY,
                (value, condition, config) ->
                        value instanceof Map
                                && !((Map<?, ?>) value).isEmpty());

        registry.put(
                ConditionOperator.MAP_CONTAINS_KEY,
                (value, condition, config) ->
                        value instanceof Map
                                && ((Map<?, ?>) value).containsKey(
                                condition.getExpectedValue()));

        registry.put(
                ConditionOperator.MAP_CONTAINS_KEYS,
                (value, condition, config) -> {
                    if (!(value instanceof Map)
                            || !(condition.getExpectedValue()
                            instanceof Collection)) {
                        return false;
                    }

                    Map<?, ?> map = (Map<?, ?>) value;
                    Collection<?> keys =
                            (Collection<?>) condition.getExpectedValue();

                    return map.keySet().containsAll(keys);
                });

        registry.put(
                ConditionOperator.FIELD_LESS_THAN,
                (value, condition, config) ->
                        compareField(
                                value,
                                condition,
                                config) < 0);

        registry.put(
                ConditionOperator.FIELD_LESS_OR_EQUAL,
                (value, condition, config) ->
                        compareField(
                                value,
                                condition,
                                config) <= 0);

        registry.put(
                ConditionOperator.FIELD_GREATER_THAN,
                (value, condition, config) ->
                        compareField(
                                value,
                                condition,
                                config) > 0);

        registry.put(
                ConditionOperator.FIELD_GREATER_OR_EQUAL,
                (value, condition, config) ->
                        compareField(
                                value,
                                condition,
                                config) >= 0);

        registry.put(
                ConditionOperator.EXTENSION,
                (value, condition, config) -> {
                    ConditionExtension<Object> extension =
                            (ConditionExtension<Object>)
                                    condition.getExtension();

                    return extension.evaluate(config, value);
                });

        for (ConditionOperator operator :
                ConditionOperator.values()) {
            if (!registry.containsKey(operator)) {
                throw new IllegalStateException(
                        "Missing evaluator for "
                                + operator.name());
            }
        }

        return Collections.unmodifiableMap(registry);
    }

    private static int compareField(
            Object value,
            Condition<?> condition,
            ReadonlyConfig config) {

        if (value == null) {
            return Integer.MIN_VALUE;
        }

        Object other = config.get(
                condition.getCompareOption());

        if (other == null) {
            return Integer.MIN_VALUE;
        }

        return compareValues(value, other);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static int compareValues(Object left, Object right) {
        if (left == null || right == null) {
            throw new OptionValidationException(
                    "Cannot compare null values");
        }

        if (left instanceof Number
                && right instanceof Number) {
            return compareNumbers(
                    (Number) left,
                    (Number) right);
        }

        if (left instanceof Comparable
                && left.getClass().isInstance(right)) {
            try {
                return ((Comparable) left).compareTo(right);
            } catch (RuntimeException e) {
                throw new OptionValidationException(
                        "Cannot compare values of type %s and %s",
                        left.getClass().getSimpleName(),
                        right.getClass().getSimpleName());
            }
        }

        throw new OptionValidationException(
                "Cannot compare values of type %s and %s",
                left.getClass().getSimpleName(),
                right.getClass().getSimpleName());
    }

    private static int compareNumbers(
            Number left,
            Number right) {

        if (left instanceof BigDecimal
                || right instanceof BigDecimal) {
            BigDecimal leftValue =
                    left instanceof BigDecimal
                            ? (BigDecimal) left
                            : new BigDecimal(left.toString());

            BigDecimal rightValue =
                    right instanceof BigDecimal
                            ? (BigDecimal) right
                            : new BigDecimal(right.toString());

            return leftValue.compareTo(rightValue);
        }

        if (left instanceof Double
                || left instanceof Float
                || right instanceof Double
                || right instanceof Float) {
            return Double.compare(
                    left.doubleValue(),
                    right.doubleValue());
        }

        return Long.compare(
                left.longValue(),
                right.longValue());
    }

    @FunctionalInterface
    private interface Evaluator {
        boolean evaluate(
                Object value,
                Condition<?> condition,
                ReadonlyConfig config);
    }
}