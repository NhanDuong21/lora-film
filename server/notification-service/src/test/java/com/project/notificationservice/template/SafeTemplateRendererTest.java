package com.project.notificationservice.template;

import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.TemplateStatus;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.template.TemplateRegistry.TemplateDocument;
import com.project.notificationservice.template.TemplateRegistry.VariableDefinition;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeTemplateRendererTest {

    private final SafeTemplateRenderer renderer = new SafeTemplateRenderer(20_000, 200, 480);

    @Test
    void rejectsMissingRequiredVariables() {
        assertThat(renderer.validate(document(), Map.of()).valid()).isFalse();
        assertThat(renderer.validate(document(), Map.of()).errors())
                .contains("missing required variable: customerName");
    }

    @Test
    void rejectsUnknownVariablesInStrictMode() {
        assertThat(renderer.validate(document(), Map.of(
                "customerName", "A",
                "unexpected", "value")).errors())
                .contains("unknown variable: unexpected");
    }

    @Test
    void sanitizesUnsafeHtmlAndEscapesValues() {
        TemplateRegistry.RenderedTemplate rendered =
                renderer.render(document(), Map.of("customerName", "<Admin>"));
        assertThat(rendered.htmlContent())
                .contains("&lt;Admin&gt;")
                .doesNotContain("script", "alert");
    }

    @Test
    void rejectsUnsupportedHelpers() {
        TemplateDocument unsafe = new TemplateDocument(
                "TICKET_PURCHASED", "Test", "Test", Category.TRANSACTIONAL,
                Channel.EMAIL, "vi-VN", TemplateStatus.PUBLISHED,
                Map.of("customerName", new VariableDefinition("string", true)),
                Map.of("customerName", "A"),
                "{{lookup customerName}}", "<p>{{customerName}}</p>", "{{customerName}}",
                "0123456789012345678901234567890123456789", "v000001", Instant.now());
        assertThatThrownBy(() -> renderer.render(unsafe, unsafe.sampleData()))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("unsupported helper");
    }

    private TemplateDocument document() {
        return new TemplateDocument(
                "TICKET_PURCHASED",
                "Test",
                "Test",
                Category.TRANSACTIONAL,
                Channel.EMAIL,
                "vi-VN",
                TemplateStatus.PUBLISHED,
                Map.of("customerName", new VariableDefinition("string", true)),
                Map.of("customerName", "A"),
                "{{customerName}}",
                "<p>{{customerName}}</p><script>alert(1)</script>",
                "{{customerName}}",
                "0123456789012345678901234567890123456789",
                "v000001",
                Instant.now());
    }
}
