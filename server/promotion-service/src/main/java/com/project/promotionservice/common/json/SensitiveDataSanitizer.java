package com.project.promotionservice.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SensitiveDataSanitizer {

    private static final Set<String> SENSITIVE_FIELD_PARTS = Set.of(
            "phone",
            "email",
            "password",
            "secret",
            "token",
            "authorization",
            "address");

    public JsonNode sanitize(JsonNode value) {
        if (value == null || value.isNull()) {
            return value;
        }
        if (value.isObject()) {
            ObjectNode result = ((ObjectNode) value).objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                result.set(field.getKey(), isSensitive(field.getKey())
                        ? result.textNode("[REDACTED]")
                        : sanitize(field.getValue()));
            }
            return result;
        }
        if (value.isArray()) {
            ArrayNode result = ((ArrayNode) value).arrayNode();
            value.forEach(item -> result.add(sanitize(item)));
            return result;
        }
        return value.deepCopy();
    }

    private boolean isSensitive(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_PARTS.stream().anyMatch(normalized::contains);
    }
}
