package com.link.up.api.configuration.util;

import com.link.up.api.configuration.Option;

import java.util.*;

/**
 * 配置项规则。
 */
public final class OptionRule {

    private final List<Option<?>> optionalOptions;
    private final List<RequiredOption> requiredOptions;
    private final List<ConditionRule> conditionRules;
    private final List<Condition<?>> valueConstraints;

    private OptionRule(
            List<Option<?>> optionalOptions,
            List<RequiredOption> requiredOptions,
            List<ConditionRule> conditionRules,
            List<Condition<?>> valueConstraints) {

        this.optionalOptions =
                Collections.unmodifiableList(
                        new ArrayList<>(optionalOptions)
                );

        this.requiredOptions =
                Collections.unmodifiableList(
                        new ArrayList<>(requiredOptions)
                );

        this.conditionRules =
                Collections.unmodifiableList(
                        new ArrayList<>(conditionRules)
                );

        this.valueConstraints =
                Collections.unmodifiableList(
                        new ArrayList<>(valueConstraints)
                );
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Option<?>> getOptionalOptions() {
        return optionalOptions;
    }

    public List<RequiredOption> getRequiredOptions() {
        return requiredOptions;
    }

    public List<ConditionRule> getConditionRules() {
        return conditionRules;
    }

    public List<Condition<?>> getValueConstraints() {
        return valueConstraints;
    }

    public boolean hasOptions() {
        return !optionalOptions.isEmpty()
                || !requiredOptions.isEmpty()
                || !conditionRules.isEmpty()
                || !valueConstraints.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OptionRule)) {
            return false;
        }

        OptionRule that = (OptionRule) obj;
        return Objects.equals(optionalOptions, that.optionalOptions)
                && Objects.equals(requiredOptions, that.requiredOptions)
                && Objects.equals(conditionRules, that.conditionRules)
                && Objects.equals(valueConstraints, that.valueConstraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                optionalOptions,
                requiredOptions,
                conditionRules,
                valueConstraints);
    }

    public static final class Builder {

        private final List<Option<?>> optionalOptions =
                new ArrayList<>();

        private final List<RequiredOption> requiredOptions =
                new ArrayList<>();

        private final List<ConditionRule> conditionRules =
                new ArrayList<>();

        private final List<Condition<?>> valueConstraints =
                new ArrayList<>();

        private Builder() {
        }

        private static <T> List<T> merge(
                List<T> first,
                List<T> second) {

            List<T> result =
                    new ArrayList<>(first);

            result.addAll(second);
            return result;
        }

        private static void requireOptions(
                Option<?>[] options) {

            if (options == null
                    || options.length == 0) {
                throw new OptionValidationException(
                        "Options must not be empty");
            }

            for (Option<?> option : options) {
                Objects.requireNonNull(
                        option,
                        "option");
            }
        }

        private static OptionValidationException duplicate(
                Option<?> option) {

            return new OptionValidationException(
                    "Option '%s' is declared repeatedly",
                    option.key());
        }

        public Builder optional(Option<?>... options) {
            requireOptions(options);

            for (Option<?> option : options) {
                verifyOptionalDuplicate(option);
                optionalOptions.add(option);
            }

            return this;
        }

        public Builder required(Option<?>... options) {
            requireOptions(options);

            RequiredOption required =
                    RequiredOption
                            .AbsolutelyRequiredOptions
                            .of(options);

            verifyStructuralDuplicate(required);
            requiredOptions.add(required);
            return this;
        }

        public Builder exclusive(Option<?>... options) {
            requireOptions(options);

            if (options.length < 2) {
                throw new OptionValidationException(
                        "Exclusive options must contain at least two options");
            }

            RequiredOption required =
                    RequiredOption
                            .ExclusiveRequiredOptions
                            .of(options);

            verifyStructuralDuplicate(required);
            requiredOptions.add(required);
            return this;
        }

        public Builder bundled(Option<?>... options) {
            requireOptions(options);

            if (options.length < 2) {
                throw new OptionValidationException(
                        "Bundled options must contain at least two options");
            }

            RequiredOption required =
                    RequiredOption
                            .BundledRequiredOptions
                            .of(options);

            verifyStructuralDuplicate(required);
            requiredOptions.add(required);
            return this;
        }

        public <T> Builder conditional(
                Option<T> conditionOption,
                T expectedValue,
                Option<?>... required) {

            verifyConditionalExists(conditionOption);

            return requiredWhen(
                    Condition.of(
                            conditionOption,
                            expectedValue),
                    required);
        }

        public <T> Builder conditional(
                Option<T> conditionOption,
                List<T> expectedValues,
                Option<?>... required) {

            verifyConditionalExists(conditionOption);

            if (expectedValues == null
                    || expectedValues.isEmpty()) {
                throw new OptionValidationException(
                        "Conditional values must not be empty");
            }

            Condition<T> condition = null;

            for (T value : expectedValues) {
                if (condition == null) {
                    condition =
                            Condition.of(
                                    conditionOption,
                                    value);
                } else {
                    condition.or(
                            Condition.of(
                                    conditionOption,
                                    value));
                }
            }

            return requiredWhen(condition, required);
        }

        public Builder requiredWhen(
                Condition<?> condition,
                Option<?>... required) {

            Objects.requireNonNull(condition, "condition");
            requireOptions(required);

            verifyConditionalTarget(required);

            RequiredOption conditional =
                    RequiredOption
                            .ConditionalRequiredOptions
                            .of(
                                    condition,
                                    Arrays.asList(required));

            requiredOptions.add(conditional);
            return this;
        }

        public <T> Builder conditionalRule(
                Option<T> conditionOption,
                T expectedValue,
                OptionRule rule) {

            verifyConditionalExists(conditionOption);

            return ruleWhen(
                    Condition.of(
                            conditionOption,
                            expectedValue),
                    rule);
        }

        public <T> Builder conditionalRule(
                Option<T> conditionOption,
                List<T> expectedValues,
                OptionRule rule) {

            verifyConditionalExists(conditionOption);

            if (expectedValues == null
                    || expectedValues.isEmpty()) {
                throw new OptionValidationException(
                        "Conditional values must not be empty");
            }

            Condition<T> condition = null;

            for (T value : expectedValues) {
                if (condition == null) {
                    condition =
                            Condition.of(
                                    conditionOption,
                                    value);
                } else {
                    condition.or(
                            Condition.of(
                                    conditionOption,
                                    value));
                }
            }

            return ruleWhen(condition, rule);
        }

        public Builder ruleWhen(
                Condition<?> condition,
                OptionRule rule) {

            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(rule, "rule");

            if (!rule.hasOptions()) {
                throw new OptionValidationException(
                        "Conditional rule must not be empty");
            }

            mergeConditionRule(condition, rule);
            return this;
        }

        public Builder required(
                Option<?> option,
                Condition<?> firstCondition,
                Condition<?>... otherConditions) {

            required(option);
            addConstraints(
                    firstCondition,
                    otherConditions);

            return this;
        }

        public Builder required(
                Option<?> firstOption,
                Option<?> secondOption,
                Condition<?> firstCondition,
                Condition<?>... otherConditions) {

            required(
                    firstOption,
                    secondOption);

            addConstraints(
                    firstCondition,
                    otherConditions);

            return this;
        }

        public Builder optional(
                Option<?> option,
                Condition<?> firstCondition,
                Condition<?>... otherConditions) {

            optional(option);
            addConstraints(
                    firstCondition,
                    otherConditions);

            return this;
        }

        public Builder optional(
                Option<?> firstOption,
                Option<?> secondOption,
                Condition<?> firstCondition,
                Condition<?>... otherConditions) {

            optional(
                    firstOption,
                    secondOption);

            addConstraints(
                    firstCondition,
                    otherConditions);

            return this;
        }

        public <T> Builder conditional(
                Option<T> conditionOption,
                T expectedValue,
                Condition<?> firstCondition,
                Condition<?>... otherConditions) {

            verifyConditionalExists(conditionOption);

            return constraintsWhen(
                    Condition.of(
                            conditionOption,
                            expectedValue),
                    firstCondition,
                    otherConditions);
        }

        public Builder constraintsWhen(
                Condition<?> trigger,
                Condition<?> firstCondition,
                Condition<?>... otherConditions) {

            Objects.requireNonNull(trigger, "trigger");

            List<Condition<?>> constraints =
                    new ArrayList<>();

            constraints.add(
                    Objects.requireNonNull(
                            firstCondition,
                            "firstCondition"));

            if (otherConditions != null) {
                Collections.addAll(
                        constraints,
                        otherConditions);
            }

            OptionRule rule =
                    new OptionRule(
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList(),
                            constraints);

            mergeConditionRule(trigger, rule);
            return this;
        }

        public OptionRule build() {
            return new OptionRule(
                    optionalOptions,
                    requiredOptions,
                    conditionRules,
                    valueConstraints);
        }

        private void addConstraints(
                Condition<?> firstCondition,
                Condition<?>[] otherConditions) {

            valueConstraints.add(
                    Objects.requireNonNull(
                            firstCondition,
                            "firstCondition"));

            if (otherConditions != null) {
                Collections.addAll(
                        valueConstraints,
                        otherConditions);
            }
        }

        private void mergeConditionRule(
                Condition<?> condition,
                OptionRule newRule) {

            for (int i = 0;
                 i < conditionRules.size();
                 i++) {

                ConditionRule current =
                        conditionRules.get(i);

                if (!current.getCondition()
                        .equals(condition)) {
                    continue;
                }

                OptionRule oldRule =
                        current.getOptionRule();

                OptionRule merged =
                        new OptionRule(
                                merge(
                                        oldRule.optionalOptions,
                                        newRule.optionalOptions),
                                merge(
                                        oldRule.requiredOptions,
                                        newRule.requiredOptions),
                                merge(
                                        oldRule.conditionRules,
                                        newRule.conditionRules),
                                merge(
                                        oldRule.valueConstraints,
                                        newRule.valueConstraints));

                conditionRules.set(
                        i,
                        new ConditionRule(
                                condition,
                                merged));
                return;
            }

            conditionRules.add(
                    new ConditionRule(
                            condition,
                            newRule));
        }

        private void verifyOptionalDuplicate(
                Option<?> option) {

            if (optionalOptions.contains(option)) {
                throw duplicate(option);
            }

            for (RequiredOption required :
                    requiredOptions) {
                if (required
                        instanceof
                        RequiredOption
                                .ConditionalRequiredOptions) {
                    continue;
                }

                if (required.getOptions()
                        .contains(option)) {
                    throw duplicate(option);
                }
            }
        }

        private void verifyStructuralDuplicate(
                RequiredOption current) {

            for (Option<?> option :
                    current.getOptions()) {

                if (optionalOptions.contains(option)) {
                    throw duplicate(option);
                }

                for (RequiredOption existing :
                        requiredOptions) {
                    if (existing.getOptions()
                            .contains(option)) {
                        throw duplicate(option);
                    }
                }
            }
        }

        private void verifyConditionalTarget(
                Option<?>[] options) {

            for (Option<?> option : options) {
                for (RequiredOption existing :
                        requiredOptions) {

                    if (existing
                            instanceof
                            RequiredOption
                                    .ConditionalRequiredOptions) {
                        continue;
                    }

                    if (existing.getOptions()
                            .contains(option)) {
                        throw duplicate(option);
                    }
                }
            }
        }

        private void verifyConditionalExists(
                Option<?> conditionOption) {

            if (optionalOptions.contains(
                    conditionOption)) {
                return;
            }

            for (RequiredOption required :
                    requiredOptions) {
                if (required.getOptions()
                        .contains(conditionOption)) {
                    return;
                }
            }

            throw new OptionValidationException(
                    "Conditional option '%s' is not declared",
                    conditionOption.key());
        }
    }
}