package com.link.up.api.configuration.util;

import com.link.up.api.configuration.Option;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.LongFunction;
import java.util.stream.Collectors;

/**
 * 配置值转换工具。
 */
@Slf4j
public final class ConfigUtil {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private ConfigUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertValue(
            Object rawValue,
            Option<T> option) {

        if (rawValue == null) {
            return null;
        }

        TypeReference<T> typeReference =
                option.typeReference();

        if (typeReference.getType() instanceof Class) {
            Class<T> targetType =
                    (Class<T>) typeReference.getType();

            if (targetType.isInstance(rawValue)) {
                return (T) rawValue;
            }

            try {
                return convertSimpleValue(
                        rawValue,
                        targetType);
            } catch (IllegalArgumentException ignored) {
                // 继续尝试 Jackson 转换。
            }
        }

        try {
            return OBJECT_MAPPER.convertValue(
                    rawValue,
                    typeReference);
        } catch (IllegalArgumentException e) {
            if (isListType(typeReference)) {
                try {
                    Class<?> elementType =
                            (Class<?>)
                                    ((ParameterizedType)
                                            typeReference.getType())
                                            .getActualTypeArguments()[0];

                    log.warn(
                            "Option '{}' should use list syntax; comma-separated strings are temporarily supported",
                            option.key());

                    return (T) convertToList(
                            rawValue,
                            elementType);
                } catch (RuntimeException ignored) {
                    // 使用统一异常。
                }
            }

            throw new IllegalArgumentException(
                    String.format(
                            "Cannot convert value '%s' to type '%s'",
                            rawValue,
                            typeReference.getType().getTypeName()),
                    e);
        }
    }

    private static boolean isListType(
            TypeReference<?> typeReference) {

        if (!(typeReference.getType()
                instanceof ParameterizedType)) {
            return false;
        }

        ParameterizedType type =
                (ParameterizedType) typeReference.getType();

        return List.class.equals(type.getRawType());
    }

    private static List<?> convertToList(
            Object rawValue,
            Class<?> elementType) {

        if (rawValue instanceof List) {
            return ((List<?>) rawValue)
                    .stream()
                    .map(value ->
                            convertSimpleValue(
                                    value,
                                    elementType))
                    .collect(Collectors.toList());
        }

        return Arrays.stream(
                rawValue.toString().split(","))
                .map(String::trim)
                .map(value ->
                        convertSimpleValue(
                                value,
                                elementType))
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T convertSimpleValue(
            Object rawValue,
            Class<T> targetType) {

        if (targetType.isInstance(rawValue)) {
            return (T) rawValue;
        }

        if (Boolean.class.equals(targetType)) {
            return (T) convertToBoolean(rawValue);
        }

        if (String.class.equals(targetType)) {
            return (T) rawValue.toString();
        }

        if (Integer.class.equals(targetType)) {
            return (T) convertToInt(rawValue);
        }

        if (Long.class.equals(targetType)) {
            return (T) convertToLong(rawValue);
        }

        if (Float.class.equals(targetType)) {
            return (T) convertToFloat(rawValue);
        }

        if (Double.class.equals(targetType)) {
            return (T) convertToDouble(rawValue);
        }

        if (BigDecimal.class.equals(targetType)) {
            return (T) new BigDecimal(
                    rawValue.toString());
        }

        if (Duration.class.equals(targetType)) {
            return (T) convertToDuration(rawValue);
        }

        if (targetType.isEnum()) {
            return (T) convertToEnum(
                    rawValue,
                    (Class<? extends Enum>) targetType);
        }

        if (Object.class.equals(targetType)) {
            return (T) rawValue;
        }

        throw new IllegalArgumentException(
                "Unsupported simple type: "
                        + targetType.getName());
    }

    private static Integer convertToInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Long) {
            long longValue = (Long) value;

            if (longValue > Integer.MAX_VALUE
                    || longValue < Integer.MIN_VALUE) {
                throw new IllegalArgumentException(
                        "Integer value out of range: "
                                + longValue);
            }

            return (int) longValue;
        }

        return Integer.parseInt(
                value.toString().trim());
    }

    private static Long convertToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        return Long.parseLong(
                value.toString().trim());
    }

    private static Float convertToFloat(Object value) {
        if (value instanceof Float) {
            return (Float) value;
        }

        double doubleValue =
                Double.parseDouble(
                        value.toString().trim());

        if (doubleValue != 0D
                && (doubleValue > Float.MAX_VALUE
                || doubleValue < -Float.MAX_VALUE)) {
            throw new IllegalArgumentException(
                    "Float value out of range: "
                            + doubleValue);
        }

        return (float) doubleValue;
    }

    private static Double convertToDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        }

        return Double.parseDouble(
                value.toString().trim());
    }

    private static Boolean convertToBoolean(Object value) {
        String text =
                value.toString()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if ("true".equals(text)) {
            return true;
        }

        if ("false".equals(text)) {
            return false;
        }

        throw new IllegalArgumentException(
                "Boolean value must be true or false: "
                        + value);
    }

    private static <E extends Enum<E>> E convertToEnum(
            Object value,
            Class<E> enumType) {

        String text =
                value.toString().trim();

        return Arrays.stream(
                enumType.getEnumConstants())
                .filter(item ->
                        item.name().equalsIgnoreCase(text)
                                || item.toString()
                                .equalsIgnoreCase(text))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format(
                                                "Enum %s only supports %s",
                                                enumType.getSimpleName(),
                                                Arrays.toString(
                                                        enumType.getEnumConstants()))));
    }

    private static Duration convertToDuration(Object value) {
        if (value instanceof Duration) {
            return (Duration) value;
        }

        String text =
                value.toString().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException(
                    "Duration value must not be blank");
        }

        String normalized =
                text.replaceAll("\\s+", "")
                        .toUpperCase(Locale.ROOT);

        Duration shorthand =
                tryParseShorthandDuration(normalized);

        if (shorthand != null) {
            return shorthand;
        }

        try {
            return Duration.parse(text);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Unsupported duration value: "
                            + value,
                    e);
        }
    }

    private static Duration tryParseShorthandDuration(
            String value) {

        Duration duration =
                parseDuration(
                        value,
                        "MS",
                        Duration::ofMillis);

        if (duration != null) {
            return duration;
        }

        duration = parseDuration(
                value,
                "S",
                Duration::ofSeconds);

        if (duration != null) {
            return duration;
        }

        duration = parseDuration(
                value,
                "M",
                Duration::ofMinutes);

        if (duration != null) {
            return duration;
        }

        duration = parseDuration(
                value,
                "H",
                Duration::ofHours);

        if (duration != null) {
            return duration;
        }

        return parseDuration(
                value,
                "D",
                Duration::ofDays);
    }

    private static Duration parseDuration(
            String value,
            String suffix,
            LongFunction<Duration> converter) {

        if (!value.endsWith(suffix)) {
            return null;
        }

        String number =
                value.substring(
                        0,
                        value.length() - suffix.length());

        if (!isInteger(number)) {
            return null;
        }

        try {
            return converter.apply(
                    Long.parseLong(number));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        int start = 0;

        if (value.charAt(0) == '+'
                || value.charAt(0) == '-') {
            if (value.length() == 1) {
                return false;
            }
            start = 1;
        }

        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static String convertToJsonString(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return (String) value;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Cannot convert value to JSON: "
                            + value,
                    e);
        }
    }
}