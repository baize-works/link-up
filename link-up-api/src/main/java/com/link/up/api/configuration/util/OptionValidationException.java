package com.link.up.api.configuration.util;

import com.link.up.api.configuration.Option;
import com.link.up.api.exception.FluxRuntimeException;
import com.link.up.api.exception.error.FluxApiErrorCode;

/**
 * 配置项校验异常。
 */
public class OptionValidationException
        extends FluxRuntimeException {

    private final String rawMessage;

    public OptionValidationException(
            String message,
            Throwable cause) {

        super(
                FluxApiErrorCode.OPTION_VALIDATION_FAILED,
                message,
                cause);

        this.rawMessage = message;
    }

    public OptionValidationException(String message) {
        super(
                FluxApiErrorCode.OPTION_VALIDATION_FAILED,
                message);

        this.rawMessage = message;
    }

    public OptionValidationException(
            String format,
            Object... args) {

        this(String.format(format, args));
    }

    public OptionValidationException(Option<?> option) {
        this(
                "The option(\"%s\") is incorrectly configured, please refer to the doc: %s",
                option.key(),
                option.getDescription());
    }

    /**
     * 获取不包含错误码前缀的原始消息。
     */
    public String getRawMessage() {
        return rawMessage;
    }
}