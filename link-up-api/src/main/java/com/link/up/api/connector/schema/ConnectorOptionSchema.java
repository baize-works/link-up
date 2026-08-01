package com.link.up.api.connector.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Connector Option 的稳定协议视图。
 */
public final class ConnectorOptionSchema {

    private final String key;
    private final ConnectorOptionValueType valueType;
    private final String javaType;
    private final String elementJavaType;
    private final Object defaultValue;
    private final List<Object> allowedValues;
    private final String description;
    private final List<String> fallbackKeys;
    private final boolean required;
    private final boolean sensitive;
    private final String semanticType;
    private final ConnectorOptionScope scope;

    public ConnectorOptionSchema(
            String key,
            ConnectorOptionValueType valueType,
            String javaType,
            String elementJavaType,
            Object defaultValue,
            List<Object> allowedValues,
            String description,
            List<String> fallbackKeys,
            boolean required,
            boolean sensitive,
            String semanticType,
            ConnectorOptionScope scope) {

        this.key = key;
        this.valueType = valueType;
        this.javaType = javaType;
        this.elementJavaType = elementJavaType;
        this.defaultValue = defaultValue;
        this.allowedValues =
                Collections.unmodifiableList(
                        new ArrayList<Object>(
                                allowedValues == null
                                        ? Collections.emptyList()
                                        : allowedValues));
        this.description = description;
        this.fallbackKeys =
                Collections.unmodifiableList(
                        new ArrayList<String>(
                                fallbackKeys == null
                                        ? Collections.<String>emptyList()
                                        : fallbackKeys));
        this.required = required;
        this.sensitive = sensitive;
        this.semanticType = semanticType;
        this.scope = scope;
    }

    public String getKey() {
        return key;
    }

    public ConnectorOptionValueType getValueType() {
        return valueType;
    }

    public String getJavaType() {
        return javaType;
    }

    public String getElementJavaType() {
        return elementJavaType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public List<Object> getAllowedValues() {
        return allowedValues;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getFallbackKeys() {
        return fallbackKeys;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public String getSemanticType() {
        return semanticType;
    }

    public ConnectorOptionScope getScope() {
        return scope;
    }
}
