package com.link.up.api.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.link.up.api.connector.schema.ConnectorOptionScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 配置项定义。
 *
 * @param <T> 配置值类型
 */
public class Option<T> {

    /**
     * 配置键。
     */
    private final String key;

    /**
     * 配置值类型。
     */
    private final TypeReference<T> typeReference;

    /**
     * 默认值。
     */
    private final T defaultValue;

    /**
     * 兼容的旧配置键。
     */
    private final List<String> fallbackKeys =
            new ArrayList<String>();

    /**
     * 配置说明。
     */
    private String description = "";

    /**
     * 是否包含密码、Token 等敏感信息。
     */
    private boolean sensitive;

    /**
     * 稳定业务语义，例如 JDBC_URL、PASSWORD、SQL。
     */
    private String semanticType = "GENERIC";

    /**
     * 配置值通常由哪一层提供。
     */
    private ConnectorOptionScope scope =
            ConnectorOptionScope.TASK;

    public Option(
            String key,
            TypeReference<T> typeReference,
            T defaultValue) {

        this.key =
                Objects.requireNonNull(
                        key,
                        "key");
        this.typeReference =
                Objects.requireNonNull(
                        typeReference,
                        "typeReference");
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public TypeReference<T> typeReference() {
        return typeReference;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getFallbackKeys() {
        return Collections.unmodifiableList(
                fallbackKeys);
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

    public Option<T> withDescription(
            String description) {

        this.description =
                description == null
                        ? ""
                        : description;
        return this;
    }

    public Option<T> withFallbackKeys(
            String... keys) {

        if (keys == null) {
            return this;
        }

        for (String fallbackKey : keys) {
            if (fallbackKey != null
                    && !fallbackKey
                    .trim()
                    .isEmpty()
                    && !fallbackKeys
                    .contains(
                            fallbackKey)) {

                fallbackKeys.add(
                        fallbackKey);
            }
        }

        return this;
    }

    public Option<T> sensitive() {
        return withSensitive(true);
    }

    public Option<T> withSensitive(
            boolean sensitive) {

        this.sensitive = sensitive;
        return this;
    }

    public Option<T> withSemanticType(
            String semanticType) {

        if (semanticType == null
                || semanticType
                .trim()
                .isEmpty()) {

            this.semanticType = "GENERIC";
        } else {
            this.semanticType =
                    semanticType
                            .trim()
                            .toUpperCase(
                                    java.util.Locale.ROOT);
        }

        return this;
    }

    public Option<T> withScope(
            ConnectorOptionScope scope) {

        this.scope =
                Objects.requireNonNull(
                        scope,
                        "scope");
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Option)) {
            return false;
        }

        Option<?> that = (Option<?>) obj;

        return Objects.equals(
                key,
                that.key)
                && Objects.equals(
                typeReference.getType(),
                that.typeReference
                        .getType())
                && Objects.equals(
                defaultValue,
                that.defaultValue)
                && Objects.equals(
                fallbackKeys,
                that.fallbackKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                key,
                typeReference.getType(),
                defaultValue,
                fallbackKeys);
    }

    @Override
    public String toString() {
        return String.format(
                "Option{key='%s', defaultValue=%s, fallbackKeys=%s}",
                key,
                defaultValue,
                fallbackKeys);
    }
}
