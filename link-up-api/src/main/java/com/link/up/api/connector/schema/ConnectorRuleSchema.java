package com.link.up.api.connector.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可供控制面消费的 Connector 配置规则。
 */
public final class ConnectorRuleSchema {

    public static final String REQUIRED = "REQUIRED";
    public static final String EXCLUSIVE = "EXCLUSIVE";
    public static final String BUNDLED = "BUNDLED";
    public static final String REQUIRED_WHEN = "REQUIRED_WHEN";
    public static final String CONSTRAINT = "CONSTRAINT";
    public static final String RULE_WHEN = "RULE_WHEN";

    private final String type;
    private final List<String> optionKeys;
    private final ConnectorConditionSchema condition;
    private final List<ConnectorRuleSchema> nestedRules;

    public ConnectorRuleSchema(
            String type,
            List<String> optionKeys,
            ConnectorConditionSchema condition,
            List<ConnectorRuleSchema> nestedRules) {

        this.type = type;
        this.optionKeys =
                Collections.unmodifiableList(
                        new ArrayList<String>(
                                optionKeys == null
                                        ? Collections.<String>emptyList()
                                        : optionKeys));
        this.condition = condition;
        this.nestedRules =
                Collections.unmodifiableList(
                        new ArrayList<ConnectorRuleSchema>(
                                nestedRules == null
                                        ? Collections.<ConnectorRuleSchema>emptyList()
                                        : nestedRules));
    }

    public String getType() {
        return type;
    }

    public List<String> getOptionKeys() {
        return optionKeys;
    }

    public ConnectorConditionSchema getCondition() {
        return condition;
    }

    public List<ConnectorRuleSchema> getNestedRules() {
        return nestedRules;
    }
}
