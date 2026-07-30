package com.project.notificationservice.template;

import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.TemplateStatus;
import com.project.notificationservice.template.TemplateRegistry.TemplateDocument;
import com.project.notificationservice.template.TemplateRegistry.VariableDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplatePayloadAdapterTest {

    private final TemplatePayloadAdapter adapter = new TemplatePayloadAdapter();

    @Test
    void adaptsBookingPayloadToLegacyMailVariablesAndDropsUnknownValues() {
        TemplateDocument template = document(Map.of(
                "user_name", optional(),
                "booking_code", optional(),
                "poster_url", optional(),
                "room_name", optional(),
                "seats", optional(),
                "combos", optional(),
                "qr_code_url", optional(),
                "retry_link", optional(),
                "failure_reason", optional()));

        Map<String, Object> adapted = adapter.adapt(Map.of(
                "customerName", "An",
                "bookingCode", "BK-01",
                "moviePosterUrl", "https://cdn/poster.jpg",
                "auditoriumName", "Hall 2",
                "seatNames", List.of("A1", "A2"),
                "foodItems", List.of(Map.of("name", "Popcorn", "quantity", 2)),
                "ticketAccessUrl", "https://tickets/one",
                "deepLink", "/payments/retry",
                "failureMessage", "Provider rejected payment",
                "internalOnly", "must-not-render"), template);

        assertThat(adapted).containsEntry("user_name", "An")
                .containsEntry("booking_code", "BK-01")
                .containsEntry("poster_url", "https://cdn/poster.jpg")
                .containsEntry("room_name", "Hall 2")
                .containsEntry("seats", "A1, A2")
                .containsEntry("combos", "Popcorn x2")
                .containsEntry("qr_code_url", "https://tickets/one")
                .containsEntry("retry_link", "/payments/retry")
                .containsEntry("failure_reason", "Provider rejected payment")
                .doesNotContainKey("internalOnly");
    }

    private TemplateDocument document(Map<String, TemplateRegistry.VariableDefinition> schema) {
        return new TemplateDocument(
                "TICKET_PURCHASED", "Ticket", "", Category.TRANSACTIONAL,
                Channel.EMAIL, "vi-VN", TemplateStatus.PUBLISHED, schema, Map.of(),
                "Ticket", "<p>Ticket</p>", "Ticket", "a".repeat(40), null, null);
    }

    private VariableDefinition optional() {
        return new VariableDefinition("string", false);
    }
}
