package com.link.up.api.configuration.util;

import com.link.up.api.configuration.Option;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * 条件创建工具。
 */
public final class Conditions {

    private Conditions() {
    }

    public static <T> Condition<T> equalTo(
            Option<T> option,
            Object value) {

        return new Condition<>(
                option,
                ConditionOperator.EQUAL,
                value,
                null);
    }

    public static <T> Condition<T> notEqual(
            Option<T> option,
            Object value) {

        return new Condition<>(
                option,
                ConditionOperator.NOT_EQUAL,
                value,
                null);
    }

    public static <T> Condition<T> greaterThan(
            Option<T> option,
            Object value) {

        return literal(
                option,
                ConditionOperator.GREATER_THAN,
                value);
    }

    public static <T> Condition<T> greaterOrEqual(
            Option<T> option,
            Object value) {

        return literal(
                option,
                ConditionOperator.GREATER_OR_EQUAL,
                value);
    }

    public static <T> Condition<T> lessThan(
            Option<T> option,
            Object value) {

        return literal(
                option,
                ConditionOperator.LESS_THAN,
                value);
    }

    public static <T> Condition<T> lessOrEqual(
            Option<T> option,
            Object value) {

        return literal(
                option,
                ConditionOperator.LESS_OR_EQUAL,
                value);
    }

    public static Condition<String> notBlank(
            Option<String> option) {

        return unary(
                option,
                ConditionOperator.NOT_BLANK);
    }

    public static Condition<String> startsWith(
            Option<String> option,
            String prefix) {

        return literal(
                option,
                ConditionOperator.STARTS_WITH,
                prefix);
    }

    public static Condition<String> contains(
            Option<String> option,
            String text) {

        return literal(
                option,
                ConditionOperator.CONTAINS,
                text);
    }

    public static Condition<String> matches(
            Option<String> option,
            String regex) {

        return literal(
                option,
                ConditionOperator.MATCHES,
                regex);
    }

    public static Condition<String> upperCase(
            Option<String> option) {

        return unary(
                option,
                ConditionOperator.UPPER_CASE);
    }

    public static Condition<String> lowerCase(
            Option<String> option) {

        return unary(
                option,
                ConditionOperator.LOWER_CASE);
    }

    public static <T extends Collection<?>> Condition<T> notEmpty(
            Option<T> option) {

        return unary(
                option,
                ConditionOperator.NOT_EMPTY);
    }

    public static <T extends Collection<?>> Condition<T> unique(
            Option<T> option) {

        return unary(
                option,
                ConditionOperator.COLLECTION_UNIQUE);
    }

    public static <T extends Map<?, ?>> Condition<T> mapNotEmpty(
            Option<T> option) {

        return unary(
                option,
                ConditionOperator.MAP_NOT_EMPTY);
    }

    public static <T extends Map<?, ?>> Condition<T> mapContainsKey(
            Option<T> option,
            Object key) {

        return literal(
                option,
                ConditionOperator.MAP_CONTAINS_KEY,
                key);
    }

    public static <T extends Map<?, ?>> Condition<T> mapContainsKeys(
            Option<T> option,
            Object... keys) {

        return literal(
                option,
                ConditionOperator.MAP_CONTAINS_KEYS,
                Arrays.asList(keys));
    }

    public static <T> Condition<T> lessThanField(
            Option<T> option,
            Option<T> other) {

        return field(
                option,
                ConditionOperator.FIELD_LESS_THAN,
                other);
    }

    public static <T> Condition<T> lessOrEqualField(
            Option<T> option,
            Option<T> other) {

        return field(
                option,
                ConditionOperator.FIELD_LESS_OR_EQUAL,
                other);
    }

    public static <T> Condition<T> greaterThanField(
            Option<T> option,
            Option<T> other) {

        return field(
                option,
                ConditionOperator.FIELD_GREATER_THAN,
                other);
    }

    public static <T> Condition<T> greaterOrEqualField(
            Option<T> option,
            Option<T> other) {

        return field(
                option,
                ConditionOperator.FIELD_GREATER_OR_EQUAL,
                other);
    }

    public static <T> Condition<T> extension(
            Option<T> option,
            ConditionExtension<T> extension) {

        return new Condition<>(
                option,
                ConditionOperator.EXTENSION,
                null,
                null,
                extension);
    }

    private static <T> Condition<T> unary(
            Option<T> option,
            ConditionOperator operator) {

        return new Condition<>(
                option,
                operator,
                null,
                null);
    }

    private static <T> Condition<T> literal(
            Option<T> option,
            ConditionOperator operator,
            Object value) {

        return new Condition<>(
                option,
                operator,
                value,
                null);
    }

    private static <T> Condition<T> field(
            Option<T> option,
            ConditionOperator operator,
            Option<?> compareOption) {

        return new Condition<>(
                option,
                operator,
                null,
                compareOption);
    }
}