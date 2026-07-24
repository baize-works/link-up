package com.link.up.api.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Flux global exception, used to tell user more clearly error messages
 */
public class FluxRuntimeException extends RuntimeException {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FluxErrorCode FluxErrorCode;
    private final Map<String, String> params;

    public FluxRuntimeException(FluxErrorCode FluxErrorCode, String errorMessage) {
        super(FluxErrorCode.getErrorMessage() + " - " + errorMessage);
        this.FluxErrorCode = FluxErrorCode;
        this.params = new HashMap<>();
        ExceptionParamsUtil.assertParamsMatchWithDescription(
                FluxErrorCode.getDescription(), params);
    }

    public FluxRuntimeException(
            FluxErrorCode FluxErrorCode, String errorMessage, Throwable cause) {
        super(FluxErrorCode.getErrorMessage() + " - " + errorMessage, cause);
        this.FluxErrorCode = FluxErrorCode;
        this.params = new HashMap<>();
        ExceptionParamsUtil.assertParamsMatchWithDescription(
                FluxErrorCode.getDescription(), params);
    }

    public FluxRuntimeException(FluxErrorCode FluxErrorCode, Throwable cause) {
        super(FluxErrorCode.getErrorMessage(), cause);
        this.FluxErrorCode = FluxErrorCode;
        this.params = new HashMap<>();
        ExceptionParamsUtil.assertParamsMatchWithDescription(
                FluxErrorCode.getDescription(), params);
    }

    public FluxRuntimeException(
            FluxErrorCode FluxErrorCode, Map<String, String> params) {
        super(ExceptionParamsUtil.getDescription(FluxErrorCode.getErrorMessage(), params));
        this.FluxErrorCode = FluxErrorCode;
        this.params = params;
    }

    public FluxRuntimeException(
            FluxErrorCode FluxErrorCode, Map<String, String> params, Throwable cause) {
        super(
                ExceptionParamsUtil.getDescription(FluxErrorCode.getErrorMessage(), params),
                cause);
        this.FluxErrorCode = FluxErrorCode;
        this.params = params;
    }

    public FluxErrorCode getFluxErrorCode() {
        return FluxErrorCode;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Map<String, String> getParamsValueAsMap(String key) {
        try {
            return OBJECT_MAPPER.readValue(
                    params.get(key), new TypeReference<Map<String, String>>() {
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T getParamsValueAs(String key) {
        try {
            return OBJECT_MAPPER.readValue(params.get(key), new TypeReference<T>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
