package com.link.up.api.configuration.util;

import com.link.up.api.configuration.Option;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 配置校验辅助工具。
 */
public final class OptionUtil {

    private static final String ERROR_TEMPLATE =
            "%s: %s\n      type: %s\n      constraint: %s";

    private OptionUtil() {
    }

    public static String getOptionKeys(
            List<? extends Option<?>> options) {

        return options.stream()
                .map(option -> "'" + option.key() + "'")
                .collect(Collectors.joining(", "));
    }

    public static String formatError(
            String optionKey,
            String type,
            String constraint) {

        return String.format(
                ERROR_TEMPLATE,
                "option",
                optionKey,
                type,
                constraint);
    }

    public static String formatOptionsError(
            String optionKeys,
            String type,
            String constraint) {

        return String.format(
                ERROR_TEMPLATE,
                "options",
                optionKeys,
                type,
                constraint);
    }
}