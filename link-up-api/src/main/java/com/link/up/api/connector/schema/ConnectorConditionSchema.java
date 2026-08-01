package com.link.up.api.connector.schema;

/**
 * 可序列化的条件表达式节点。
 *
 * <p>{@code logicalOperator} 表示当前节点与 {@code next} 的连接关系，
 * 仅允许 AND 或 OR；最后一个节点为空。
 */
public final class ConnectorConditionSchema {

    private final String optionKey;
    private final String operator;
    private final Object expectedValue;
    private final String compareOptionKey;
    private final String extensionDescription;
    private final String logicalOperator;
    private final ConnectorConditionSchema next;

    public ConnectorConditionSchema(
            String optionKey,
            String operator,
            Object expectedValue,
            String compareOptionKey,
            String extensionDescription,
            String logicalOperator,
            ConnectorConditionSchema next) {

        this.optionKey = optionKey;
        this.operator = operator;
        this.expectedValue = expectedValue;
        this.compareOptionKey = compareOptionKey;
        this.extensionDescription = extensionDescription;
        this.logicalOperator = logicalOperator;
        this.next = next;
    }

    public String getOptionKey() {
        return optionKey;
    }

    public String getOperator() {
        return operator;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public String getCompareOptionKey() {
        return compareOptionKey;
    }

    public String getExtensionDescription() {
        return extensionDescription;
    }

    public String getLogicalOperator() {
        return logicalOperator;
    }

    public ConnectorConditionSchema getNext() {
        return next;
    }
}
