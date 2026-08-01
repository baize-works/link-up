package com.link.up.api.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.link.up.api.connector.schema.ConnectorOptionScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 单选配置项。
 *
 * @param <T> 配置值类型
 */
public class SingleChoiceOption<T>
        extends Option<T> {

    /**
     * 可选值列表。
     */
    private final List<T> optionValues;

    public SingleChoiceOption(
            String key,
            TypeReference<T> typeReference,
            List<T> optionValues,
            T defaultValue) {

        super(
                key,
                typeReference,
                defaultValue);

        this.optionValues =
                Collections.unmodifiableList(
                        new ArrayList<T>(
                                Objects.requireNonNull(
                                        optionValues,
                                        "optionValues")));
    }

    public List<T> getOptionValues() {
        return optionValues;
    }

    @Override
    public SingleChoiceOption<T>
    withDescription(String description) {

        super.withDescription(description);
        return this;
    }

    @Override
    public SingleChoiceOption<T>
    withFallbackKeys(String... fallbackKeys) {

        super.withFallbackKeys(
                fallbackKeys);
        return this;
    }

    @Override
    public SingleChoiceOption<T> sensitive() {
        super.sensitive();
        return this;
    }

    @Override
    public SingleChoiceOption<T>
    withSensitive(boolean sensitive) {

        super.withSensitive(sensitive);
        return this;
    }

    @Override
    public SingleChoiceOption<T>
    withSemanticType(String semanticType) {

        super.withSemanticType(
                semanticType);
        return this;
    }

    @Override
    public SingleChoiceOption<T>
    withScope(ConnectorOptionScope scope) {

        super.withScope(scope);
        return this;
    }
}
