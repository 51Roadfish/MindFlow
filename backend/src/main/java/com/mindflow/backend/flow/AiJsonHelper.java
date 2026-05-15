package com.mindflow.backend.flow;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AI 模型返回的 JSON 经常带格式问题（多余文本、未转义换行等），提供容错解析。
 */
final class AiJsonHelper {

    private static final ObjectMapper lenientMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    /**
     * 从 AI 响应中提取 JSON（去掉 ```json 等标记），用宽松设置解析。
     * 若解析失败，会自动清理常见格式问题后重试。
     */
    static <T> T parse(String raw, Class<T> targetType) throws Exception {
        String cleaned = raw
                .replaceAll("```[jJ][sS][oO][nN]?", "")
                .replaceAll("```", "")
                .trim();

        // 替换中文标点为英文
        cleaned = cleaned
                .replace('，', ',')
                .replace('（', '(')
                .replace('）', ')')
                .replace('“', '"')  // "
                .replace('”', '"'); // "

        // 提取第一个 {} 或 [] 包裹的内容
        int start = cleaned.indexOf('{');
        if (start < 0) start = cleaned.indexOf('[');
        int end = -1;
        if (start >= 0) {
            char open = cleaned.charAt(start);
            char close = open == '{' ? '}' : ']';
            int depth = 0;
            for (int i = start; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                if (c == open) depth++;
                else if (c == close) {
                    depth--;
                    if (depth == 0) { end = i + 1; break; }
                }
            }
        }
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end);
        }

        try {
            return lenientMapper.readValue(cleaned, targetType);
        } catch (Exception e) {
            // 尝试修复常见 JSON 格式问题后重试：
            // 1) 数字值后缺少逗号，如 {"score": 85 "feedback": ...}
            // 2) 字符串值后缺少逗号
            // 3) 重复逗号 / 对象前多余逗号
            // 4) 末尾多余逗号
            String fixed = cleaned
                    .replaceAll("(\\d|\"|true|false|null)\\s+(\"[^\"]+\"\\s*:)", "$1, $2")
                    .replaceAll(",\\s*,", ",")
                    .replaceAll(",\\s*}", "}")
                    .replaceAll(",\\s*]", "]")
                    .replaceAll("\\{\\s*,", "{");
            return lenientMapper.readValue(fixed, targetType);
        }
    }

    private AiJsonHelper() {}
}
