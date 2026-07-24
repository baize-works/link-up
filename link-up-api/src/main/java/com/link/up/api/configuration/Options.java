package com.link.up.api.configuration;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 配置项构建工具。
 */
public final class Options {

    private Options() {
    }

    public static OptionBuilder key(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Option key must not be blank");
        }
        return new OptionBuilder(key.trim());
    }

    public static final class OptionBuilder {

        private final String key;

        private OptionBuilder(String key) {
            this.key = key;
        }

        public TypedOptionBuilder<Boolean> booleanType() {
            return type(new TypeReference<Boolean>() {
            });
        }

        public TypedOptionBuilder<Integer> intType() {
            return type(new TypeReference<Integer>() {
            });
        }

        public TypedOptionBuilder<Long> longType() {
            return type(new TypeReference<Long>() {
            });
        }

        public TypedOptionBuilder<BigDecimal> bigDecimalType() {
            return type(new TypeReference<BigDecimal>() {
            });
        }

        public TypedOptionBuilder<Float> floatType() {
            return type(new TypeReference<Float>() {
            });
        }

        public TypedOptionBuilder<Double> doubleType() {
            return type(new TypeReference<Double>() {
            });
        }

        public TypedOptionBuilder<String> stringType() {
            return type(new TypeReference<String>() {
            });
        }

        public TypedOptionBuilder<Duration> durationType() {
            return type(new TypeReference<Duration>() {
            });
        }

        public <T extends Enum<T>> TypedOptionBuilder<T> enumType(Class<T> enumClass) {
            Objects.requireNonNull(enumClass, "enumClass");

            return type(
                    new TypeReference<T>() {
                        @Override
                        public Type getType() {
                            return enumClass;
                        }
                    });
        }

        public TypedOptionBuilder<Map<String, String>> mapType() {
            return type(new TypeReference<Map<String, String>>() {
            });
        }

        public TypedOptionBuilder<Map<String, Object>> mapObjectType() {
            return type(new TypeReference<Map<String, Object>>() {
            });
        }

        public TypedOptionBuilder<List<String>> listType() {
            return type(new TypeReference<List<String>>() {
            });
        }

        public <T> TypedOptionBuilder<List<T>> listType(Class<T> elementType) {
            Objects.requireNonNull(elementType, "elementType");

            return type(
                    new TypeReference<List<T>>() {
                        @Override
                        public Type getType() {
                            return new ParameterizedType() {
                                @Override
                                public Type[] getActualTypeArguments() {
                                    return new Type[]{elementType};
                                }

                                @Override
                                public Type getRawType() {
                                    return List.class;
                                }

                                @Override
                                public Type getOwnerType() {
                                    return null;
                                }
                            };
                        }
                    });
        }

        public <T> TypedOptionBuilder<T> objectType(Class<T> objectType) {
            Objects.requireNonNull(objectType, "objectType");

            return type(
                    new TypeReference<T>() {
                        @Override
                        public Type getType() {
                            return objectType;
                        }
                    });
        }

        public <T> TypedOptionBuilder<T> type(TypeReference<T> typeReference) {
            return new TypedOptionBuilder<>(
                    key,
                    Objects.requireNonNull(typeReference, "typeReference"));
        }

        public <T> SingleChoiceOptionBuilder<T> singleChoice(
                Class<T> optionType,
                List<T> optionValues) {

            Objects.requireNonNull(optionType, "optionType");
            Objects.requireNonNull(optionValues, "optionValues");

            if (optionValues.isEmpty()) {
                throw new IllegalArgumentException(
                        "Single choice option values must not be empty");
            }

            TypeReference<T> typeReference =
                    new TypeReference<T>() {
                        @Override
                        public Type getType() {
                            return optionType;
                        }
                    };

            return new SingleChoiceOptionBuilder<>(
                    key,
                    typeReference,
                    optionValues);
        }
    }

    public static final class TypedOptionBuilder<T> {

        private final String key;
        private final TypeReference<T> typeReference;

        private TypedOptionBuilder(
                String key,
                TypeReference<T> typeReference) {
            this.key = key;
            this.typeReference = typeReference;
        }

        public Option<T> defaultValue(T value) {
            return new Option<>(key, typeReference, value);
        }

        public Option<T> noDefaultValue() {
            return new Option<>(key, typeReference, null);
        }
    }

    public static final class SingleChoiceOptionBuilder<T> {

        private final String key;
        private final TypeReference<T> typeReference;
        private final List<T> optionValues;

        private SingleChoiceOptionBuilder(
                String key,
                TypeReference<T> typeReference,
                List<T> optionValues) {

            this.key = key;
            this.typeReference = typeReference;
            this.optionValues = new ArrayList<>(optionValues);
        }

        public SingleChoiceOption<T> defaultValue(T value) {
            if (value != null && !optionValues.contains(value)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Default value '%s' must be one of %s",
                                value,
                                optionValues));
            }

            return new SingleChoiceOption<>(
                    key,
                    typeReference,
                    optionValues,
                    value);
        }

        public SingleChoiceOption<T> noDefaultValue() {
            return new SingleChoiceOption<>(
                    key,
                    typeReference,
                    optionValues,
                    null);
        }
    }
}