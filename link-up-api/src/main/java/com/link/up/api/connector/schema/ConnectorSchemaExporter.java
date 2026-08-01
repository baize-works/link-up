package com.link.up.api.connector.schema;

import com.link.up.api.configuration.Option;
import com.link.up.api.configuration.SingleChoiceOption;
import com.link.up.api.configuration.util.Condition;
import com.link.up.api.configuration.util.ConditionRule;
import com.link.up.api.configuration.util.OptionRule;
import com.link.up.api.configuration.util.RequiredOption;
import com.link.up.api.factory.Factory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将运行时 {@link OptionRule} 转换为稳定、可序列化的 Connector Schema。
 */
public final class ConnectorSchemaExporter {

    public ConnectorSchema export(
            Factory factory,
            ConnectorRole role,
            OptionRule optionRule) {

        if (factory == null) {
            throw new IllegalArgumentException(
                    "factory must not be null");
        }
        if (role == null) {
            throw new IllegalArgumentException(
                    "role must not be null");
        }
        if (optionRule == null) {
            throw new IllegalArgumentException(
                    "optionRule must not be null");
        }

        Map<String, Option<?>> optionIndex =
                new LinkedHashMap<String, Option<?>>();

        collectOptions(
                optionRule,
                optionIndex,
                Collections.newSetFromMap(
                        new IdentityHashMap<OptionRule, Boolean>()));

        Set<String> absolutelyRequired =
                new java.util.LinkedHashSet<String>();

        for (RequiredOption required :
                optionRule.getRequiredOptions()) {
            if (required
                    instanceof RequiredOption
                    .AbsolutelyRequiredOptions) {
                absolutelyRequired.addAll(
                        optionKeys(
                                required.getOptions()));
            }
        }

        List<ConnectorOptionSchema> options =
                new ArrayList<ConnectorOptionSchema>();

        for (Option<?> option :
                optionIndex.values()) {
            options.add(
                    optionSchema(
                            option,
                            absolutelyRequired.contains(
                                    option.key())));
        }

        Collections.sort(
                options,
                new Comparator<ConnectorOptionSchema>() {
                    public int compare(
                            ConnectorOptionSchema first,
                            ConnectorOptionSchema second) {
                        return first.getKey()
                                .compareTo(
                                        second.getKey());
                    }
                });

        List<ConnectorRuleSchema> rules =
                exportRules(optionRule);

        List<ConnectorCapability> capabilities =
                new ArrayList<ConnectorCapability>(
                        factory.capabilities());

        Collections.sort(
                capabilities,
                new Comparator<ConnectorCapability>() {
                    public int compare(
                            ConnectorCapability first,
                            ConnectorCapability second) {
                        return first.name()
                                .compareTo(
                                        second.name());
                    }
                });

        String implementationVersion =
                implementationVersion(
                        factory.getClass());

        String fingerprint =
                fingerprint(
                        factory.factoryIdentifier(),
                        role,
                        factory.schemaVersion(),
                        options,
                        rules,
                        capabilities);

        return new ConnectorSchema(
                factory.factoryIdentifier(),
                role,
                factory.schemaVersion(),
                fingerprint,
                factory.getClass().getName(),
                implementationVersion,
                options,
                rules,
                capabilities);
    }

    private static void collectOptions(
            OptionRule rule,
            Map<String, Option<?>> optionIndex,
            Set<OptionRule> visited) {

        if (!visited.add(rule)) {
            throw new IllegalArgumentException(
                    "Circular OptionRule graph detected");
        }

        for (Option<?> option :
                rule.getOptionalOptions()) {
            putOption(optionIndex, option);
        }

        for (RequiredOption required :
                rule.getRequiredOptions()) {

            for (Option<?> option :
                    required.getOptions()) {
                putOption(optionIndex, option);
            }

            if (required
                    instanceof RequiredOption
                    .ConditionalRequiredOptions) {

                collectConditionOptions(
                        ((RequiredOption
                                .ConditionalRequiredOptions)
                                required)
                                .getCondition(),
                        optionIndex);
            }
        }

        for (Condition<?> constraint :
                rule.getValueConstraints()) {
            collectConditionOptions(
                    constraint,
                    optionIndex);
        }

        for (ConditionRule conditionRule :
                rule.getConditionRules()) {

            collectConditionOptions(
                    conditionRule.getCondition(),
                    optionIndex);

            collectOptions(
                    conditionRule.getOptionRule(),
                    optionIndex,
                    visited);
        }

        visited.remove(rule);
    }

    private static void collectConditionOptions(
            Condition<?> condition,
            Map<String, Option<?>> optionIndex) {

        Set<Condition<?>> visited =
                Collections.newSetFromMap(
                        new IdentityHashMap<Condition<?>, Boolean>());

        Condition<?> current = condition;

        while (current != null) {
            if (!visited.add(current)) {
                throw new IllegalArgumentException(
                        "Circular condition chain detected");
            }

            putOption(
                    optionIndex,
                    current.getOption());

            if (current.getCompareOption() != null) {
                putOption(
                        optionIndex,
                        current.getCompareOption());
            }

            current = current.getNext();
        }
    }

    private static void putOption(
            Map<String, Option<?>> optionIndex,
            Option<?> option) {

        Option<?> existing =
                optionIndex.get(option.key());

        if (existing == null) {
            optionIndex.put(
                    option.key(),
                    option);
            return;
        }

        if (!existing.typeReference()
                .getType()
                .equals(
                        option.typeReference()
                                .getType())) {

            throw new IllegalArgumentException(
                    "Option key '"
                            + option.key()
                            + "' is declared with different types");
        }
    }

    private static List<ConnectorRuleSchema>
    exportRules(OptionRule rule) {

        List<ConnectorRuleSchema> result =
                new ArrayList<ConnectorRuleSchema>();

        for (RequiredOption required :
                rule.getRequiredOptions()) {

            if (required
                    instanceof RequiredOption
                    .AbsolutelyRequiredOptions) {

                result.add(
                        new ConnectorRuleSchema(
                                ConnectorRuleSchema.REQUIRED,
                                optionKeys(
                                        required.getOptions()),
                                null,
                                null));

            } else if (required
                    instanceof RequiredOption
                    .ExclusiveRequiredOptions) {

                result.add(
                        new ConnectorRuleSchema(
                                ConnectorRuleSchema.EXCLUSIVE,
                                optionKeys(
                                        required.getOptions()),
                                null,
                                null));

            } else if (required
                    instanceof RequiredOption
                    .BundledRequiredOptions) {

                result.add(
                        new ConnectorRuleSchema(
                                ConnectorRuleSchema.BUNDLED,
                                optionKeys(
                                        required.getOptions()),
                                null,
                                null));

            } else if (required
                    instanceof RequiredOption
                    .ConditionalRequiredOptions) {

                RequiredOption
                        .ConditionalRequiredOptions
                        conditional =
                        (RequiredOption
                                .ConditionalRequiredOptions)
                                required;

                result.add(
                        new ConnectorRuleSchema(
                                ConnectorRuleSchema.REQUIRED_WHEN,
                                optionKeys(
                                        conditional.getOptions()),
                                conditionSchema(
                                        conditional.getCondition()),
                                null));
            }
        }

        for (Condition<?> constraint :
                rule.getValueConstraints()) {

            result.add(
                    new ConnectorRuleSchema(
                            ConnectorRuleSchema.CONSTRAINT,
                            Collections.singletonList(
                                    constraint
                                            .getOption()
                                            .key()),
                            conditionSchema(constraint),
                            null));
        }

        for (ConditionRule conditionRule :
                rule.getConditionRules()) {

            OptionRule nested =
                    conditionRule.getOptionRule();

            result.add(
                    new ConnectorRuleSchema(
                            ConnectorRuleSchema.RULE_WHEN,
                            optionKeys(
                                    nested.getOptionalOptions()),
                            conditionSchema(
                                    conditionRule.getCondition()),
                            exportRules(nested)));
        }

        return Collections.unmodifiableList(result);
    }

    private static ConnectorConditionSchema
    conditionSchema(Condition<?> condition) {

        if (condition == null) {
            return null;
        }

        String logicalOperator = null;

        if (condition.getNext() != null) {
            logicalOperator =
                    Boolean.TRUE.equals(
                            condition.and())
                            ? "AND"
                            : "OR";
        }

        String extensionDescription =
                condition.getExtension() == null
                        ? null
                        : condition.getExtension()
                        .description();

        return new ConnectorConditionSchema(
                condition.getOption().key(),
                condition.getOperator().name(),
                jsonValue(
                        condition.getExpectedValue()),
                condition.getCompareOption() == null
                        ? null
                        : condition.getCompareOption()
                        .key(),
                extensionDescription,
                logicalOperator,
                conditionSchema(
                        condition.getNext()));
    }

    private static ConnectorOptionSchema optionSchema(
            Option<?> option,
            boolean required) {

        Type type =
                option.typeReference()
                        .getType();

        ConnectorOptionValueType valueType =
                valueType(type);

        List<Object> allowedValues =
                allowedValues(option, type);

        return new ConnectorOptionSchema(
                option.key(),
                valueType,
                type.getTypeName(),
                elementJavaType(type),
                jsonValue(
                        option.defaultValue()),
                allowedValues,
                option.getDescription(),
                option.getFallbackKeys(),
                required,
                option.isSensitive(),
                option.getSemanticType(),
                option.getScope());
    }

    private static List<Object> allowedValues(
            Option<?> option,
            Type type) {

        List<Object> result =
                new ArrayList<Object>();

        if (option instanceof SingleChoiceOption) {
            for (Object value :
                    ((SingleChoiceOption<?>) option)
                            .getOptionValues()) {
                result.add(jsonValue(value));
            }
        } else if (type instanceof Class
                && ((Class<?>) type).isEnum()) {

            Object[] constants =
                    ((Class<?>) type)
                            .getEnumConstants();

            if (constants != null) {
                for (Object constant : constants) {
                    result.add(
                            jsonValue(constant));
                }
            }
        }

        return result;
    }

    private static ConnectorOptionValueType
    valueType(Type type) {

        if (type == Boolean.class
                || type == boolean.class) {
            return ConnectorOptionValueType.BOOLEAN;
        }
        if (type == Integer.class
                || type == int.class
                || type == Short.class
                || type == short.class
                || type == Byte.class
                || type == byte.class) {
            return ConnectorOptionValueType.INTEGER;
        }
        if (type == Long.class
                || type == long.class) {
            return ConnectorOptionValueType.LONG;
        }
        if (type == BigDecimal.class) {
            return ConnectorOptionValueType.DECIMAL;
        }
        if (type == Float.class
                || type == float.class) {
            return ConnectorOptionValueType.FLOAT;
        }
        if (type == Double.class
                || type == double.class) {
            return ConnectorOptionValueType.DOUBLE;
        }
        if (type == String.class
                || type == Character.class
                || type == char.class) {
            return ConnectorOptionValueType.STRING;
        }
        if (type == Duration.class) {
            return ConnectorOptionValueType.DURATION;
        }
        if (type instanceof Class
                && ((Class<?>) type).isEnum()) {
            return ConnectorOptionValueType.ENUM;
        }

        Class<?> rawType = rawType(type);

        if (rawType != null
                && Map.class.isAssignableFrom(
                        rawType)) {
            return ConnectorOptionValueType.MAP;
        }
        if (rawType != null
                && Collection.class
                .isAssignableFrom(rawType)) {
            return ConnectorOptionValueType.LIST;
        }
        if (type instanceof Class) {
            return ConnectorOptionValueType.OBJECT;
        }

        return ConnectorOptionValueType.UNKNOWN;
    }

    private static String elementJavaType(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedType =
                (ParameterizedType) type;

        Type raw =
                parameterizedType.getRawType();

        if (!(raw instanceof Class)
                || !Collection.class
                .isAssignableFrom(
                        (Class<?>) raw)) {
            return null;
        }

        Type[] arguments =
                parameterizedType
                        .getActualTypeArguments();

        return arguments.length == 0
                ? null
                : arguments[0].getTypeName();
    }

    private static Class<?> rawType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type raw =
                    ((ParameterizedType) type)
                            .getRawType();
            return raw instanceof Class
                    ? (Class<?>) raw
                    : null;
        }
        return null;
    }

    private static List<String> optionKeys(
            List<? extends Option<?>> options) {

        List<String> result =
                new ArrayList<String>();

        for (Option<?> option : options) {
            result.add(option.key());
        }

        return result;
    }

    private static Object jsonValue(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }

        if (value instanceof Duration) {
            return value.toString();
        }

        if (value instanceof Map) {
            Map<?, ?> source =
                    (Map<?, ?>) value;

            Map<String, Object> result =
                    new java.util.TreeMap<String, Object>();

            for (Map.Entry<?, ?> entry :
                    source.entrySet()) {

                result.put(
                        String.valueOf(
                                entry.getKey()),
                        jsonValue(
                                entry.getValue()));
            }

            return result;
        }

        if (value instanceof Collection) {
            List<Object> result =
                    new ArrayList<Object>();

            for (Object item :
                    (Collection<?>) value) {
                result.add(
                        jsonValue(item));
            }

            return result;
        }

        if (value.getClass().isArray()) {
            int length =
                    java.lang.reflect.Array
                            .getLength(value);

            List<Object> result =
                    new ArrayList<Object>(
                            length);

            for (int index = 0;
                 index < length;
                 index++) {

                result.add(
                        jsonValue(
                                java.lang.reflect.Array
                                        .get(
                                                value,
                                                index)));
            }

            return result;
        }

        /*
         * Connector 自定义对象不能直接泄漏到协议中，
         * 否则会把插件 ClassLoader 和 Java 实现细节带给控制面。
         */
        return String.valueOf(value);
    }

    private static String implementationVersion(
            Class<?> implementationClass) {

        Package implementationPackage =
                implementationClass.getPackage();

        String version =
                implementationPackage == null
                        ? null
                        : implementationPackage
                        .getImplementationVersion();

        return version == null
                || version.trim().isEmpty()
                ? "unknown"
                : version;
    }

    private static String fingerprint(
            String connectorId,
            ConnectorRole role,
            String schemaVersion,
            List<ConnectorOptionSchema> options,
            List<ConnectorRuleSchema> rules,
            List<ConnectorCapability> capabilities) {

        StringBuilder canonical =
                new StringBuilder();

        canonical.append(connectorId)
                .append('|')
                .append(role.name())
                .append('|')
                .append(schemaVersion);

        for (ConnectorOptionSchema option :
                options) {

            canonical.append("\nO|")
                    .append(option.getKey())
                    .append('|')
                    .append(option.getValueType())
                    .append('|')
                    .append(option.getJavaType())
                    .append('|')
                    .append(option.getElementJavaType())
                    .append('|')
                    .append(canonicalValue(
                            option.getDefaultValue()))
                    .append('|')
                    .append(canonicalValue(
                            option.getAllowedValues()))
                    .append('|')
                    .append(option.isRequired())
                    .append('|')
                    .append(option.isSensitive())
                    .append('|')
                    .append(option.getSemanticType())
                    .append('|')
                    .append(option.getScope())
                    .append('|')
                    .append(option.getDescription())
                    .append('|')
                    .append(option.getFallbackKeys());
        }

        for (ConnectorRuleSchema rule : rules) {
            appendRule(canonical, rule);
        }

        for (ConnectorCapability capability :
                capabilities) {
            canonical.append("\nC|")
                    .append(capability.name());
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256");

            byte[] value =
                    digest.digest(
                            canonical.toString()
                                    .getBytes(
                                            StandardCharsets.UTF_8));

            StringBuilder hexadecimal =
                    new StringBuilder(
                            value.length * 2);

            for (byte current : value) {
                hexadecimal.append(
                        String.format(
                                "%02x",
                                current & 0xff));
            }

            return "sha256:"
                    + hexadecimal.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception);
        }
    }

    private static void appendRule(
            StringBuilder canonical,
            ConnectorRuleSchema rule) {

        canonical.append("\nR|")
                .append(rule.getType())
                .append('|')
                .append(rule.getOptionKeys());

        appendCondition(
                canonical,
                rule.getCondition());

        for (ConnectorRuleSchema nested :
                rule.getNestedRules()) {
            appendRule(canonical, nested);
        }
    }

    private static void appendCondition(
            StringBuilder canonical,
            ConnectorConditionSchema condition) {

        ConnectorConditionSchema current =
                condition;

        while (current != null) {
            canonical.append("|Q:")
                    .append(current.getOptionKey())
                    .append(':')
                    .append(current.getOperator())
                    .append(':')
                    .append(canonicalValue(
                            current.getExpectedValue()))
                    .append(':')
                    .append(current.getCompareOptionKey())
                    .append(':')
                    .append(current.getExtensionDescription())
                    .append(':')
                    .append(current.getLogicalOperator());

            current = current.getNext();
        }
    }

    private static String canonicalValue(
            Object value) {

        if (value == null) {
            return "null";
        }

        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            List<String> entries =
                    new ArrayList<String>();

            for (Map.Entry<?, ?> entry :
                    map.entrySet()) {
                entries.add(
                        canonicalValue(entry.getKey())
                                + "="
                                + canonicalValue(
                                entry.getValue()));
            }

            Collections.sort(entries);
            return entries.toString();
        }

        if (value instanceof Collection) {
            List<String> values =
                    new ArrayList<String>();

            for (Object item :
                    (Collection<?>) value) {
                values.add(
                        canonicalValue(item));
            }

            return values.toString();
        }

        if (value.getClass().isArray()) {
            int length =
                    java.lang.reflect.Array
                            .getLength(value);

            List<String> values =
                    new ArrayList<String>(length);

            for (int index = 0;
                 index < length;
                 index++) {
                values.add(
                        canonicalValue(
                                java.lang.reflect.Array
                                        .get(
                                                value,
                                                index)));
            }

            return values.toString();
        }

        return String.valueOf(
                jsonValue(value));
    }
}
