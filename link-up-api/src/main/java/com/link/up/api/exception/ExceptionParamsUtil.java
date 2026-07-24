package com.link.up.api.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 异常参数处理工具类。
 *
 * <p>用于提取异常描述模板中的参数，并根据参数值生成完整的异常描述。
 */
public final class ExceptionParamsUtil {

    /**
     * 匹配由尖括号包裹的参数，例如：{@code <param1>}。
     */
    private static final Pattern PARAMS_PATTERN =
            Pattern.compile("<([a-zA-Z0-9]+)>");

    private ExceptionParamsUtil() {
        // 工具类禁止实例化
    }

    /**
     * 获取异常描述模板中的所有参数名称。
     *
     * <p>参数需要使用尖括号包裹，例如：
     * {@code "<param1> <param2>"} 将返回 {@code ["param1", "param2"]}。
     *
     * @param description 异常描述模板
     * @return 参数名称列表
     */
    public static List<String> getParams(String description) {
        Matcher matcher = PARAMS_PATTERN.matcher(description);
        List<String> params = new ArrayList<>();

        while (matcher.find()) {
            params.add(matcher.group(1));
        }

        return params;
    }

    /**
     * 根据参数值替换异常描述模板中的占位参数。
     *
     * @param descriptionTemplate 异常描述模板
     * @param params              参数名称与参数值的映射
     * @return 替换参数后的异常描述
     */
    public static String getDescription(
            String descriptionTemplate,
            Map<String, String> params) {

        assertParamsMatchWithDescription(descriptionTemplate, params);

        String description = descriptionTemplate;
        for (String param : getParams(descriptionTemplate)) {
            description = description.replace(
                    String.format("<%s>", param),
                    params.get(param)
            );
        }

        return description;
    }

    /**
     * 校验异常描述模板中声明的参数是否都已设置。
     *
     * @param descriptionTemplate 异常描述模板
     * @param params              参数名称与参数值的映射
     * @throws IllegalArgumentException 模板中的参数未设置时抛出
     */
    public static void assertParamsMatchWithDescription(
            String descriptionTemplate,
            Map<String, String> params) {

        getParams(descriptionTemplate).forEach(param -> {
            if (!params.containsKey(param)) {
                throw new IllegalArgumentException(
                        String.format(
                                "异常描述模板 [%s] 中的参数 [%s] 未设置",
                                descriptionTemplate,
                                param
                        )
                );
            }
        });
    }
}