package com.link.up.api.configuration.util;

import java.util.Objects;

/**
 * 条件触发的子规则。
 */
public final class ConditionRule {

    private final Condition<?> condition;
    private final OptionRule optionRule;

    public ConditionRule(
            Condition<?> condition,
            OptionRule optionRule) {

        this.condition = Objects.requireNonNull(
                condition,
                "condition");

        this.optionRule = Objects.requireNonNull(
                optionRule,
                "optionRule");
    }

    public Condition<?> getCondition() {
        return condition;
    }

    public OptionRule getOptionRule() {
        return optionRule;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConditionRule)) {
            return false;
        }

        ConditionRule that = (ConditionRule) obj;
        return Objects.equals(condition, that.condition)
                && Objects.equals(optionRule, that.optionRule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition, optionRule);
    }
}