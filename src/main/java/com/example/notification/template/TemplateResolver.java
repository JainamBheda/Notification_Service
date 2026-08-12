package com.example.notification.template;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TemplateResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\}");

    public String resolve(String template, Map<String, Object> data) {
        if (template == null) {
            return null;
        }
        Map<String, Object> values = data == null ? Map.of() : data;
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = values.get(key);
            String replacement = value == null ? "" : Matcher.quoteReplacement(String.valueOf(value));
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
