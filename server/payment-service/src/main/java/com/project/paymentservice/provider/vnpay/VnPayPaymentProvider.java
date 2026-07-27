package com.project.paymentservice.provider.vnpay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.config.VnPayProperties;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.provider.ProviderCrypto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Component
@ConditionalOnProperty(name = "payment.providers.vnpay.enabled", havingValue = "true")
public class VnPayPaymentProvider implements PaymentProvider {
    private static final DateTimeFormatter PROVIDER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final VnPayProperties properties;
    private final ObjectMapper objectMapper;

    public VnPayPaymentProvider(VnPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        require(properties.getTmnCode(), "payment.providers.vnpay.tmn-code");
        require(properties.getHashSecret(), "payment.providers.vnpay.hash-secret");
        require(properties.getReturnUrl(), "payment.providers.vnpay.return-url");
    }

    @Override
    public ProviderCode providerCode() {
        return ProviderCode.VNPAY;
    }

    @Override
    public PaymentSession createSession(PaymentSessionRequest request) {
        Instant now = Instant.now();
        Instant expiry = request.getExpiresAt().isBefore(now.plusSeconds(900))
                ? request.getExpiresAt() : now.plusSeconds(900);
        TreeMap<String, String> fields = new TreeMap<>();
        fields.put("vnp_Version", properties.getVersion());
        fields.put("vnp_Command", properties.getCommand());
        fields.put("vnp_TmnCode", properties.getTmnCode());
        fields.put("vnp_Amount", request.getAmount().movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).toPlainString());
        fields.put("vnp_CurrCode", request.getCurrency());
        fields.put("vnp_TxnRef", request.getPaymentTransactionCode());
        fields.put("vnp_OrderInfo", safeDescription(request));
        fields.put("vnp_OrderType", properties.getOrderType());
        fields.put("vnp_Locale", "vn");
        fields.put("vnp_ReturnUrl", properties.getReturnUrl());
        fields.put("vnp_IpAddr", normalizeIp(request.getClientIp()));
        fields.put("vnp_CreateDate", PROVIDER_TIME.format(now));
        fields.put("vnp_ExpireDate", PROVIDER_TIME.format(expiry));

        String query = canonical(fields);
        String signature = ProviderCrypto.hmacHex("HmacSHA512", properties.getHashSecret(), query);
        String paymentUrl = properties.getPaymentUrl() + "?" + query
                + "&vnp_SecureHash=" + signature;
        PaymentSession session = new PaymentSession(
                request.getPaymentTransactionCode(),
                request.getPaymentTransactionCode(),
                paymentUrl,
                expiry);
        session.setSanitizedProviderSummary(json(Map.of(
                "provider", "VNPAY",
                "tmnCode", properties.getTmnCode(),
                "expiresAt", expiry.toString())));
        return session;
    }

    @Override
    public ProviderCallbackResult verifyCallback(Map<String, String> parameters, String rawBody) {
        return verify(parameters, "IPN");
    }

    @Override
    public ProviderCallbackResult verifyReturn(Map<String, String> parameters) {
        return verify(parameters, "RETURN");
    }

    private ProviderCallbackResult verify(Map<String, String> parameters, String eventType) {
        TreeMap<String, String> signedFields = new TreeMap<>();
        parameters.forEach((key, value) -> {
            if (key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)
                    && value != null && !value.isBlank()) {
                signedFields.put(key, value);
            }
        });
        String expected = ProviderCrypto.hmacHex(
                "HmacSHA512", properties.getHashSecret(), canonical(signedFields));
        boolean valid = ProviderCrypto.constantTimeEquals(expected, parameters.get("vnp_SecureHash"));
        String responseCode = parameters.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = parameters.getOrDefault("vnp_TransactionStatus", "");

        ProviderCallbackResult result = new ProviderCallbackResult();
        result.setSignatureValid(valid);
        result.setProviderOrderId(parameters.get("vnp_TxnRef"));
        result.setExternalTransactionId(parameters.get("vnp_TransactionNo"));
        result.setResponseCode(responseCode);
        try {
            result.setAmount(new java.math.BigDecimal(parameters.getOrDefault("vnp_Amount", "0"))
                    .movePointLeft(2));
        } catch (NumberFormatException ignored) {
            result.setAmount(java.math.BigDecimal.ZERO);
        }
        result.setCurrency("VND");
        result.setEventType(eventType);
        result.setOccurredAt(Instant.now());
        result.setResult("00".equals(responseCode) && "00".equals(transactionStatus)
                ? "SUCCESS" : ("24".equals(responseCode) ? "CANCELLED" : "FAILED"));
        String transaction = parameters.getOrDefault("vnp_TransactionNo", "NO_TX");
        result.setDeduplicationKey(eventType + ":" + parameters.get("vnp_TxnRef")
                + ":" + transaction + ":" + responseCode);
        TreeMap<String, String> sanitized = new TreeMap<>(parameters);
        sanitized.remove("vnp_SecureHash");
        sanitized.remove("vnp_SecureHashType");
        result.setSanitizedPayload(json(sanitized));
        return result;
    }

    private String canonical(Map<String, String> fields) {
        StringBuilder builder = new StringBuilder();
        fields.forEach((key, value) -> {
            if (value == null || value.isBlank()) {
                return;
            }
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(encode(key)).append('=').append(encode(value));
        });
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String safeDescription(PaymentSessionRequest request) {
        String description = request.getOrderDescription();
        if (description == null || description.isBlank()) {
            description = "Thanh toan don " + request.getPaymentTransactionCode();
        }
        return description.length() > 255 ? description.substring(0, 255) : description;
    }

    private String normalizeIp(String value) {
        if (value == null || value.isBlank() || value.contains(":")) {
            return "127.0.0.1";
        }
        return value;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured when VNPay is enabled");
        }
    }
}
