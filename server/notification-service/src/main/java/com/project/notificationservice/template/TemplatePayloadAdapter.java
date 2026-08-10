package com.project.notificationservice.template;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TemplatePayloadAdapter {

    public Map<String, Object> adapt(
            Map<String, Object> payload,
            TemplateRegistry.TemplateDocument template) {
        Map<String, Object> expanded = new LinkedHashMap<>();
        if (payload != null) {
            payload.forEach((key, value) -> {
                expanded.put(key, value);
                expanded.putIfAbsent(toSnakeCase(key), value);
            });
        }

        alias(expanded, "user_name", "customerName", "userName", "fullName", "name");
        alias(expanded, "poster_url", "moviePosterUrl", "posterUrl");
        alias(expanded, "room_name", "auditoriumName", "roomName");
        Object qrCodeUrlObj = first(expanded, "ticketAccessUrl", "qrCodeUrl", "bookingCode", "ticketCode");
        if (qrCodeUrlObj != null) {
            String qrCodeUrlStr = String.valueOf(qrCodeUrlObj);
            if (qrCodeUrlStr.startsWith("https://api.qrserver.com")) {
                expanded.put("qr_code_url", qrCodeUrlStr);
            } else if (!qrCodeUrlStr.isBlank()) {
                try {
                    String encoded = java.net.URLEncoder.encode(qrCodeUrlStr, java.nio.charset.StandardCharsets.UTF_8.name());
                    expanded.put("qr_code_url", "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encoded);
                } catch (Exception e) {
                    expanded.put("qr_code_url", qrCodeUrlStr);
                }
            }
        }

        // Enrich each ticket entry in `tickets` list with a `qr_code_url` field
        Object ticketsObj = expanded.get("tickets");
        if (ticketsObj instanceof java.util.List<?> ticketsList) {
            java.util.List<Map<String, Object>> enrichedTickets = new java.util.ArrayList<>();
            for (Object entry : ticketsList) {
                if (entry instanceof Map<?, ?> rawEntry) {
                    Map<String, Object> ticket = new LinkedHashMap<>();
                    rawEntry.forEach((k, v) -> ticket.put(String.valueOf(k), v));
                    // Generate qr_code_url from ticketCode or ticketAccessUrl
                    String qrData = ticket.containsKey("ticketCode")
                            ? String.valueOf(ticket.get("ticketCode"))
                            : ticket.containsKey("ticketAccessUrl")
                                    ? String.valueOf(ticket.get("ticketAccessUrl"))
                                    : null;
                    if (qrData != null && !qrData.isBlank()) {
                        try {
                            String encoded = java.net.URLEncoder.encode(qrData,
                                    java.nio.charset.StandardCharsets.UTF_8.name());
                            ticket.put("qr_code_url",
                                    "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encoded);
                        } catch (Exception ignored) {
                            ticket.put("qr_code_url", qrData);
                        }
                    }
                    enrichedTickets.add(ticket);
                }
            }
            if (!enrichedTickets.isEmpty()) {
                expanded.put("tickets", enrichedTickets);
            }
        }
        alias(expanded, "ticket_link", "ticketAccessUrl", "deepLink");
        alias(expanded, "amount", "totalAmount", "totalPaid");
        alias(expanded, "transaction_id", "paymentCode", "transactionId");
        alias(expanded, "promo_code", "promotionCode", "promoCode");
        alias(expanded, "expired_time", "expiredAt", "expiresAt");
        alias(expanded, "expiry_time", "expiresAt", "expiredAt");
        alias(expanded, "expiry_date", "expiresAt", "expiryAt");
        alias(expanded, "failure_reason", "failureMessage", "reason");
        alias(expanded, "cancel_reason", "cancellationReason", "reason");
        alias(expanded, "cancellation_reason", "cancellationReason", "reason");
        alias(expanded, "discount_value", "discountValue", "discount");
        alias(expanded, "discount_applied", "discountApplied", "discount");
        for (String link : new String[]{
                "payment_link", "rebook_link", "retry_link", "support_link",
                "login_link", "verification_link", "use_now_link", "explore_link",
                "try_now_link", "secure_account_link"}) {
            alias(expanded, link, "deepLink");
        }

        Object seats = first(expanded, "seatNames", "seats");
        if (seats instanceof Collection<?> collection) {
            expanded.put("seats", collection.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ")));
        }
        Object foodItems = first(expanded, "foodItems", "combos");
        if (foodItems instanceof Collection<?> collection) {
            expanded.put("combos", formatFoodItems(collection));
        }

        Map<String, Object> renderingPayload = new LinkedHashMap<>();
        for (String variable : template.variablesSchema().keySet()) {
            if (expanded.containsKey(variable)) {
                renderingPayload.put(variable, expanded.get(variable));
            }
        }
        // Always preserve collection & ticket structural properties for Handlebars loops & sections
        for (String key : new String[]{"tickets", "foodItems", "combos", "food_items", "ticketCodes", "ticketTypes", "seatNames"}) {
            if (expanded.containsKey(key)) {
                renderingPayload.put(key, expanded.get(key));
            }
        }
        return Map.copyOf(renderingPayload);
    }

    private void alias(Map<String, Object> values, String target, String... sources) {
        if (values.containsKey(target)) return;
        Object value = first(values, sources);
        if (value != null) values.put(target, value);
    }

    private Object first(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private String formatFoodItems(Collection<?> items) {
        return items.stream().map(item -> {
            if (!(item instanceof Map<?, ?> map)) return String.valueOf(item);
            Object name = firstMapValue(map, "name", "productName");
            Object quantity = firstMapValue(map, "quantity", "qty");
            if (name == null) return String.valueOf(item);
            return quantity == null ? String.valueOf(name) : name + " x" + quantity;
        }).collect(Collectors.joining(", "));
    }

    private Object firstMapValue(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private String toSnakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }
}
