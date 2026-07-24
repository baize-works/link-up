package com.link.up.api.configuration;

import com.link.up.api.configuration.util.ConfigUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;

/**
 * 只读配置对象。
 */
@Slf4j
public final class ReadonlyConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, Object> confData;

    private ReadonlyConfig(Map<String, Object> confData) {
        this.confData = Collections.unmodifiableMap(
                new LinkedHashMap<>(confData));
    }

    public static ReadonlyConfig fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        return new ReadonlyConfig(map);
    }

    public static ReadonlyConfig fromConfig(Config config) {
        Objects.requireNonNull(config, "config");

        try {
            String json = config.root().render(
                    ConfigRenderOptions.concise());

            Map<String, Object> data =
                    OBJECT_MAPPER.readValue(
                            json,
                            new TypeReference<Map<String, Object>>() {
                            });

            return fromMap(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Failed to parse HOCON configuration",
                    e);
        }
    }

    /**
     * 获取配置值，不存在时返回默认值。
     */
    public <T> T get(Option<T> option) {
        return getOptional(option).orElseGet(option::defaultValue);
    }

    /**
     * 获取用户实际配置的值，不包含默认值。
     */
    public <T> Optional<T> getOptional(Option<T> option) {
        Objects.requireNonNull(option, "option");

        Object value = getValue(option.key());

        if (value == null) {
            for (String fallbackKey : option.getFallbackKeys()) {
                value = getValue(fallbackKey);

                if (value != null) {
                    log.warn(
                            "Please use the new key '{}' instead of deprecated key '{}'",
                            option.key(),
                            fallbackKey);
                    break;
                }
            }
        }

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(
                ConfigUtil.convertValue(value, option));
    }

    public Map<String, String> toMap() {
        if (confData.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        confData.forEach(
                (key, value) ->
                        result.put(
                                key,
                                ConfigUtil.convertToJsonString(value)));

        return result;
    }

    public Map<String, Object> getSourceMap() {
        return confData;
    }

    @SuppressWarnings("unchecked")
    private Object getValue(String key) {
        if (confData.containsKey(key)) {
            return confData.get(key);
        }

        String[] paths = key.split("\\.");
        Map<String, Object> current = confData;

        for (int i = 0; i < paths.length; i++) {
            Object value = current.get(paths[i]);

            if (i == paths.length - 1) {
                return value;
            }

            if (!(value instanceof Map)) {
                return null;
            }

            current = (Map<String, Object>) value;
        }

        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ReadonlyConfig)) {
            return false;
        }

        ReadonlyConfig that = (ReadonlyConfig) obj;
        return Objects.equals(confData, that.confData);
    }

    @Override
    public int hashCode() {
        return confData.hashCode();
    }

    @Override
    public String toString() {
        return ConfigUtil.convertToJsonString(confData);
    }
}