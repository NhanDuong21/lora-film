package com.project.notificationservice.service;

import com.project.notificationservice.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateRenderer {

    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Pattern VALID_VARIABLE_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }

        Map<String, Object> safeVariables = variables != null ? variables : Collections.emptyMap();
        Set<String> placeholders = new LinkedHashSet<>();
        Matcher matcher = BRACE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!VALID_VARIABLE_PATTERN.matcher(variableName).matches()) {
                throw new BusinessException(
                        "Expression or invalid variable name execution is blocked: " + variableName,
                        "NOTIFICATION_TEMPLATE_RENDER_FAILED",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
            placeholders.add(variableName);
        }

        List<String> missingVariables = new ArrayList<>();
        for (String placeholder : placeholders) {
            if (!safeVariables.containsKey(placeholder) || safeVariables.get(placeholder) == null) {
                missingVariables.add(placeholder);
            }
        }

        if (!missingVariables.isEmpty()) {
            Map<String, Object> errorData = Map.of("missingVariables", missingVariables);
            throw new BusinessException(
                    "Required template variables are missing",
                    "NOTIFICATION_TEMPLATE_VARIABLE_MISSING",
                    HttpStatus.BAD_REQUEST,
                    errorData
            );
        }

        String renderedText = template;
        for (String placeholder : placeholders) {
            Object value = safeVariables.get(placeholder);
            renderedText = renderedText.replace("{" + placeholder + "}", String.valueOf(value));
        }

        return renderedText;
    }
}
