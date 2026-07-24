package com.link.up.api.configuration.util;

import com.link.up.api.configuration.Option;
import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.configuration.SingleChoiceOption;

import java.util.*;
import java.util.stream.Collectors;

import static com.link.up.api.configuration.util.OptionUtil.*;

/**
 * 配置规则校验器。
 */
public final class ConfigValidator {

    private static final String TYPE_REQUIRED =
            "required";

    private static final String TYPE_VALUE =
            "value";

    private static final String TYPE_BUNDLED =
            "bundled";

    private static final String TYPE_EXCLUSIVE =
            "exclusive";

    private static final String TYPE_CONDITIONAL =
            "conditional";

    private static final String TYPE_SINGLE_CHOICE =
            "singleChoice";

    private final ReadonlyConfig config;

    private ConfigValidator(ReadonlyConfig config) {
        if (config == null) {
            throw new IllegalArgumentException(
                    "config must not be null");
        }

        this.config = config;
    }

    public static ConfigValidator of(
            ReadonlyConfig config) {

        return new ConfigValidator(config);
    }

    /**
     * 校验未声明的配置键。
     *
     * @param commonOptions 通用配置项，可为空
     */
    public static void validateUnknownKeys(
            ReadonlyConfig config,
            OptionRule rule,
            String connectorName,
            Option<?>... commonOptions) {

        Set<String> declaredKeys =
                collectDeclaredKeys(rule);

        if (commonOptions != null) {
            collectKeys(
                    declaredKeys,
                    Arrays.asList(commonOptions));
        }

        List<String> unknownKeys =
                new ArrayList<>();

        validatePaths(
                config.getSourceMap(),
                "",
                declaredKeys,
                unknownKeys);

        if (!unknownKeys.isEmpty()) {
            throw new OptionValidationException(
                    "Connector '%s' has unknown option keys: %s. Declared options are: %s",
                    connectorName,
                    unknownKeys,
                    declaredKeys.stream()
                            .sorted()
                            .collect(Collectors.toList()));
        }
    }

    private static Set<String> collectDeclaredKeys(
            OptionRule rule) {

        Set<String> keys =
                new HashSet<>();

        if (rule == null) {
            return keys;
        }

        collectKeys(
                keys,
                rule.getOptionalOptions());

        for (RequiredOption required :
                rule.getRequiredOptions()) {
            collectKeys(
                    keys,
                    required.getOptions());

            if (required
                    instanceof
                    RequiredOption
                            .ConditionalRequiredOptions) {

                collectConditionKeys(
                        keys,
                        ((RequiredOption
                                .ConditionalRequiredOptions)
                                required)
                                .getCondition());
            }
        }

        for (ConditionRule conditionRule :
                rule.getConditionRules()) {

            collectConditionKeys(
                    keys,
                    conditionRule.getCondition());

            keys.addAll(
                    collectDeclaredKeys(
                            conditionRule
                                    .getOptionRule()));
        }

        for (Condition<?> constraint :
                rule.getValueConstraints()) {

            collectConditionKeys(
                    keys,
                    constraint);
        }

        return keys;
    }

    private static void collectConditionKeys(
            Set<String> keys,
            Condition<?> condition) {

        Condition<?> current = condition;

        while (current != null) {
            addOptionKeys(
                    keys,
                    current.getOption());

            if (current.getCompareOption()
                    != null) {
                addOptionKeys(
                        keys,
                        current.getCompareOption());
            }

            current = current.getNext();
        }
    }

    private static void collectKeys(
            Set<String> keys,
            List<? extends Option<?>> options) {

        for (Option<?> option : options) {
            addOptionKeys(keys, option);
        }
    }

    private static void addOptionKeys(
            Set<String> keys,
            Option<?> option) {

        keys.add(option.key());
        keys.addAll(
                option.getFallbackKeys());
    }

    @SuppressWarnings("unchecked")
    private static void validatePaths(
            Map<String, Object> map,
            String prefix,
            Set<String> declaredKeys,
            List<String> unknownKeys) {

        for (Map.Entry<String, Object> entry :
                map.entrySet()) {

            String fullKey =
                    prefix.isEmpty()
                            ? entry.getKey()
                            : prefix
                            + "."
                            + entry.getKey();

            boolean valid =
                    declaredKeys.contains(fullKey)
                            || declaredKeys.stream()
                            .anyMatch(key ->
                                    key.startsWith(
                                            fullKey + "."));

            if (!valid) {
                unknownKeys.add(fullKey);
                continue;
            }

            if (entry.getValue() instanceof Map
                    && !declaredKeys.contains(fullKey)) {

                validatePaths(
                        (Map<String, Object>)
                                entry.getValue(),
                        fullKey,
                        declaredKeys,
                        unknownKeys);
            }
        }
    }

    public void validate(OptionRule rule) {
        if (rule == null) {
            return;
        }

        List<String> errors =
                new ArrayList<>();

        collectErrors(
                rule,
                null,
                errors);

        if (errors.isEmpty()) {
            return;
        }

        StringBuilder message =
                new StringBuilder();

        message.append(
                String.format(
                        "Option validation failed (%d error%s):",
                        errors.size(),
                        errors.size() > 1 ? "s" : ""));

        for (int i = 0;
             i < errors.size();
             i++) {

            message.append(
                    String.format(
                            "\n  [%d] %s",
                            i + 1,
                            errors.get(i)));
        }

        throw new OptionValidationException(
                message.toString());
    }

    private void collectErrors(
            OptionRule rule,
            Condition<?> activeCondition,
            List<String> errors) {

        Set<String> structurallyAbsentKeys =
                new HashSet<>();

        for (RequiredOption requiredOption :
                rule.getRequiredOptions()) {

            String error =
                    checkRequiredOption(
                            requiredOption,
                            activeCondition);

            if (error != null) {
                errors.add(error);

                collectAbsentKeys(
                        requiredOption,
                        structurallyAbsentKeys);
            }

            if (requiredOption
                    instanceof
                    RequiredOption
                            .ConditionalRequiredOptions
                    && !matchCondition(
                    (RequiredOption
                            .ConditionalRequiredOptions)
                            requiredOption)) {
                continue;
            }

            for (Option<?> option :
                    requiredOption.getOptions()) {

                if (option
                        instanceof SingleChoiceOption) {
                    validateSingleChoice(
                            (SingleChoiceOption<?>) option,
                            errors);
                }
            }
        }

        for (Option<?> option :
                rule.getOptionalOptions()) {

            if (option instanceof SingleChoiceOption) {
                validateSingleChoice(
                        (SingleChoiceOption<?>) option,
                        errors);
            }
        }

        for (ConditionRule conditionRule :
                rule.getConditionRules()) {

            Condition<?> condition =
                    conditionRule.getCondition();

            try {
                if (validate(condition)) {
                    collectErrors(
                            conditionRule.getOptionRule(),
                            condition,
                            errors);
                }
            } catch (OptionValidationException e) {
                errors.add(
                        formatError(
                                condition.toString(),
                                TYPE_CONDITIONAL,
                                e.getRawMessage()));
            }
        }

        for (Condition<?> constraint :
                rule.getValueConstraints()) {

            if (structurallyAbsentKeys.contains(
                    constraint.getOption().key())) {
                continue;
            }

            if (!isConstraintApplicable(
                    constraint,
                    rule)) {
                continue;
            }

            try {
                if (!validate(constraint)) {
                    errors.add(
                            formatError(
                                    constraint
                                            .getOption()
                                            .key(),
                                    TYPE_VALUE,
                                    constraint.toString()));
                }
            } catch (OptionValidationException e) {
                errors.add(
                        formatError(
                                constraint
                                        .getOption()
                                        .key(),
                                TYPE_VALUE,
                                e.getRawMessage()));
            }
        }
    }

    private String checkRequiredOption(
            RequiredOption requiredOption,
            Condition<?> activeCondition) {

        if (requiredOption
                instanceof
                RequiredOption
                        .AbsolutelyRequiredOptions) {

            return checkAbsolutelyRequired(
                    (RequiredOption
                            .AbsolutelyRequiredOptions)
                            requiredOption,
                    activeCondition);
        }

        if (requiredOption
                instanceof
                RequiredOption
                        .BundledRequiredOptions) {

            return checkBundled(
                    (RequiredOption
                            .BundledRequiredOptions)
                            requiredOption);
        }

        if (requiredOption
                instanceof
                RequiredOption
                        .ExclusiveRequiredOptions) {

            return checkExclusive(
                    (RequiredOption
                            .ExclusiveRequiredOptions)
                            requiredOption);
        }

        if (requiredOption
                instanceof
                RequiredOption
                        .ConditionalRequiredOptions) {

            return checkConditional(
                    (RequiredOption
                            .ConditionalRequiredOptions)
                            requiredOption);
        }

        throw new UnsupportedOperationException(
                "Unsupported required option type: "
                        + requiredOption.getClass()
                        .getName());
    }

    private String checkAbsolutelyRequired(
            RequiredOption.AbsolutelyRequiredOptions required,
            Condition<?> activeCondition) {

        List<Option<?>> absent =
                getAbsentOptions(
                        required.getRequiredOption());

        if (absent.isEmpty()) {
            return null;
        }

        String hint =
                activeCondition == null
                        ? ""
                        : " when ["
                        + activeCondition
                        + "]";

        return formatError(
                getOptionKeys(absent),
                TYPE_REQUIRED,
                "required option is not configured"
                        + hint);
    }

    private String checkBundled(
            RequiredOption.BundledRequiredOptions bundled) {

        List<Option<?>> present =
                new ArrayList<>();

        List<Option<?>> absent =
                new ArrayList<>();

        for (Option<?> option :
                bundled.getRequiredOption()) {

            if (hasOption(option)) {
                present.add(option);
            } else {
                absent.add(option);
            }
        }

        if (present.isEmpty()
                || absent.isEmpty()) {
            return null;
        }

        return formatOptionsError(
                getOptionKeys(
                        bundled.getRequiredOption()),
                TYPE_BUNDLED,
                String.format(
                        "bundled options must be present or absent together "
                                + "(present: [%s], absent: [%s])",
                        getOptionKeys(present),
                        getOptionKeys(absent)));
    }

    private String checkExclusive(
            RequiredOption.ExclusiveRequiredOptions exclusive) {

        List<Option<?>> present =
                exclusive.getExclusiveOptions()
                        .stream()
                        .filter(this::hasOption)
                        .collect(Collectors.toList());

        if (present.size() == 1) {
            return null;
        }

        if (present.isEmpty()) {
            return formatOptionsError(
                    getOptionKeys(
                            exclusive
                                    .getExclusiveOptions()),
                    TYPE_EXCLUSIVE,
                    "exactly one option must be configured");
        }

        return formatOptionsError(
                getOptionKeys(
                        exclusive.getExclusiveOptions()),
                TYPE_EXCLUSIVE,
                "multiple exclusive options are configured: "
                        + getOptionKeys(present));
    }

    private String checkConditional(
            RequiredOption.ConditionalRequiredOptions conditional) {

        if (!matchCondition(conditional)) {
            return null;
        }

        List<Option<?>> absent =
                getAbsentOptions(
                        conditional.getRequiredOption());

        if (absent.isEmpty()) {
            return null;
        }

        return formatError(
                getOptionKeys(absent),
                TYPE_CONDITIONAL,
                String.format(
                        "required because [%s] is true",
                        conditional.getCondition()));
    }

    private List<Option<?>> getAbsentOptions(
            List<Option<?>> options) {

        return options.stream()
                .filter(option ->
                        !hasOption(option)
                                && option.defaultValue()
                                == null)
                .collect(Collectors.toList());
    }

    private boolean hasOption(Option<?> option) {
        return config.getOptional(option)
                .isPresent();
    }

    private void validateSingleChoice(
            SingleChoiceOption<?> option,
            List<String> errors) {

        List<?> values =
                option.getOptionValues();

        if (values == null || values.isEmpty()) {
            errors.add(
                    formatError(
                            option.key(),
                            TYPE_SINGLE_CHOICE,
                            "optionValues must not be empty"));
            return;
        }

        Object defaultValue =
                option.defaultValue();

        if (defaultValue != null
                && !values.contains(defaultValue)) {

            errors.add(
                    formatError(
                            option.key(),
                            TYPE_SINGLE_CHOICE,
                            String.format(
                                    "defaultValue(%s) must be one of %s",
                                    defaultValue,
                                    values)));
        }

        Optional<?> configuredValue =
                config.getOptional(option);

        if (configuredValue.isPresent()
                && !values.contains(
                configuredValue.get())) {

            errors.add(
                    formatError(
                            option.key(),
                            TYPE_SINGLE_CHOICE,
                            String.format(
                                    "value(%s) must be one of %s",
                                    configuredValue.get(),
                                    values)));
        }
    }

    /**
     * 判断可选配置的值约束是否需要执行。
     */
    private boolean isConstraintApplicable(
            Condition<?> condition,
            OptionRule rule) {

        Option<?> headOption =
                condition.getOption();

        for (RequiredOption required :
                rule.getRequiredOptions()) {

            if (required
                    instanceof
                    RequiredOption
                            .AbsolutelyRequiredOptions
                    && required.getOptions()
                    .contains(headOption)) {
                return true;
            }
        }

        return anyOrSegmentFullyPresent(condition);
    }

    /**
     * 任意一个 OR 分组中的配置全部存在时执行校验。
     */
    private boolean anyOrSegmentFullyPresent(
            Condition<?> condition) {

        Condition<?> current = condition;

        while (current != null) {
            Set<Option<?>> segmentOptions =
                    new HashSet<>();

            while (current != null) {
                segmentOptions.add(
                        current.getOption());

                if (current.getCompareOption()
                        != null) {
                    segmentOptions.add(
                            current.getCompareOption());
                }

                if (!current.hasNext()) {
                    current = null;
                    break;
                }

                boolean isAnd =
                        Boolean.TRUE.equals(
                                current.and());

                current = current.getNext();

                if (!isAnd) {
                    break;
                }
            }

            boolean allPresent =
                    segmentOptions.stream()
                            .allMatch(this::hasOption);

            if (allPresent) {
                return true;
            }
        }

        return false;
    }

    /**
     * 按 AND 优先于 OR 的规则执行条件。
     */
    private boolean validate(
            Condition<?> condition) {

        Condition<?> current = condition;

        while (current != null) {
            boolean groupResult = true;

            while (current != null) {
                if (groupResult) {
                    groupResult =
                            ConditionEvaluators.evaluate(
                                    current,
                                    config);
                }

                if (!current.hasNext()) {
                    current = null;
                    break;
                }

                boolean isAnd =
                        Boolean.TRUE.equals(
                                current.and());

                current = current.getNext();

                if (!isAnd) {
                    break;
                }
            }

            if (groupResult) {
                return true;
            }
        }

        return false;
    }

    private boolean matchCondition(
            RequiredOption.ConditionalRequiredOptions conditional) {

        return validate(
                conditional.getCondition());
    }

    private void collectAbsentKeys(
            RequiredOption required,
            Set<String> absentKeys) {

        if (required
                instanceof
                RequiredOption
                        .ConditionalRequiredOptions
                && !matchCondition(
                (RequiredOption
                        .ConditionalRequiredOptions)
                        required)) {
            return;
        }

        for (Option<?> option :
                getAbsentOptions(
                        required.getOptions())) {

            absentKeys.add(option.key());
        }
    }
}