package com.project.notificationservice.service;

import com.project.notificationservice.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    public void testRender_Success() {
        String template = "Hello {name}, your booking code is {bookingCode}.";
        Map<String, Object> variables = Map.of(
                "name", "John Doe",
                "bookingCode", "LORA123"
        );

        String result = renderer.render(template, variables);
        assertEquals("Hello John Doe, your booking code is LORA123.", result);
    }

    @Test
    public void testRender_IgnoreExtraVariables() {
        String template = "Hello {name}.";
        Map<String, Object> variables = Map.of(
                "name", "John Doe",
                "extraVar", "ignored"
        );

        String result = renderer.render(template, variables);
        assertEquals("Hello John Doe.", result);
    }

    @Test
    public void testRender_MissingVariables_ThrowsException() {
        String template = "Hello {name}, code is {bookingCode}.";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");
        variables.put("bookingCode", null); // null counts as missing

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            renderer.render(template, variables);
        });

        assertEquals("NOTIFICATION_TEMPLATE_VARIABLE_MISSING", exception.getErrorCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> errorData = (Map<String, Object>) exception.getData();
        assertNotNull(errorData);
        
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) errorData.get("missingVariables");
        assertTrue(missing.contains("bookingCode"));
    }

    @Test
    public void testRender_InvalidPlaceholder_ThrowsException() {
        // Nested placeholders or methods or dots are rejected
        String template1 = "Hello {user.name}.";
        assertThrows(BusinessException.class, () -> renderer.render(template1, Map.of("user.name", "John")));

        String template2 = "Hello {1+1}.";
        assertThrows(BusinessException.class, () -> renderer.render(template2, Map.of("1+1", "2")));

        String template3 = "Hello {name()}.";
        assertThrows(BusinessException.class, () -> renderer.render(template3, Map.of("name()", "John")));
    }
}
