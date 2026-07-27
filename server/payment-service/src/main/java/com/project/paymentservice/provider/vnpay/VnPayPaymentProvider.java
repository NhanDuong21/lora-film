package com.project.paymentservice.provider.vnpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.config.VnPayProperties;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.provider.ProviderCrypto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.providers.vnpay.enabled", havingValue = "true")
public class VnPayPaymentProvider implements PaymentProvider {
    private static final DateTimeFormatter PROVIDER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final VnPayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VnPayPaymentProvider(VnPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        require(properties.getTmnCode(), "payment.providers.vnpay.tmn-code");
        require(properties.getHashSecret(), "payment.providers.vnpay.hash-secret");
        require(properties.getReturnUrl(), "payment.providers.vnpay.return-url");
        require(properties.getQueryUrl(), "payment.providers.vnpay.query-url");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .build();
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
                "createDate", fields.get("vnp_CreateDate"),
                "expiresAt", expiry.toString())));
        return session;
    }

    @Override
    public Optional<ProviderCallbackResult> queryStatus(Payment payment) {
        String transactionDate = originalTransactionDate(payment);
        if (transactionDate == null) {
            return Optional.empty();
        }
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = PROVIDER_TIME.format(Instant.now());
        String transactionCode = payment.getProviderOrderId() == null
                ? payment.getPaymentTransactionCode() : payment.getProviderOrderId();
        String orderInfo = "Truy van giao dich " + transactionCode;
        String ipAddress = normalizeIp(properties.getQueryIpAddress());

        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_RequestId", requestId);
        values.put("vnp_Version", properties.getVersion());
        values.put("vnp_Command", "querydr");
        values.put("vnp_TmnCode", properties.getTmnCode());
        values.put("vnp_TxnRef", transactionCode);
        values.put("vnp_OrderInfo", orderInfo);
        values.put("vnp_TransactionDate", transactionDate);
        values.put("vnp_CreateDate", createDate);
        values.put("vnp_IpAddr", ipAddress);
        String signatureSource = String.join("|",
                requestId,
                properties.getVersion(),
                "querydr",
                properties.getTmnCode(),
                transactionCode,
                transactionDate,
                createDate,
                ipAddress,
                orderInfo);
        values.put("vnp_SecureHash", ProviderCrypto.hmacHex(
                "HmacSHA512", properties.getHashSecret(), signatureSource));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getQueryUrl()))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(values)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            Map<String, Object> raw = objectMapper.readValue(
                    response.body(), new TypeReference<Map<String, Object>>() {});
            Map<String, String> resultValues = toStrings(raw);
            if (!"00".equals(resultValues.get("vnp_ResponseCode"))) {
                return Optional.empty();
            }
            ProviderCallbackResult result = verifyQueryResponse(resultValues);
            if (!result.isSignatureValid()
                    || !transactionCode.equals(result.getProviderOrderId())) {
                return Optional.empty();
            }
            String transactionStatus = resultValues.getOrDefault(
                    "vnp_TransactionStatus", "");
            if ("01".equals(transactionStatus)
                    || "04".equals(transactionStatus)
                    || "05".equals(transactionStatus)
                    || "06".equals(transactionStatus)
                    || "07".equals(transactionStatus)
                    || "09".equals(transactionStatus)) {
                return Optional.empty();
            }
            return Optional.of(result);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        }
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

    private ProviderCallbackResult verifyQueryResponse(Map<String, String> values) {
        String signatureSource = String.join("|",
                value(values, "vnp_ResponseId"),
                value(values, "vnp_Command"),
                value(values, "vnp_ResponseCode"),
                value(values, "vnp_Message"),
                value(values, "vnp_TmnCode"),
                value(values, "vnp_TxnRef"),
                value(values, "vnp_Amount"),
                value(values, "vnp_BankCode"),
                value(values, "vnp_PayDate"),
                value(values, "vnp_TransactionNo"),
                value(values, "vnp_TransactionType"),
                value(values, "vnp_TransactionStatus"),
                value(values, "vnp_OrderInfo"),
                value(values, "vnp_PromotionCode"),
                value(values, "vnp_PromotionAmount"));
        String expected = ProviderCrypto.hmacHex(
                "HmacSHA512", properties.getHashSecret(), signatureSource);
        String transactionStatus = value(values, "vnp_TransactionStatus");

        ProviderCallbackResult result = new ProviderCallbackResult();
        result.setSignatureValid(ProviderCrypto.constantTimeEquals(
                expected, values.get("vnp_SecureHash")));
        result.setProviderOrderId(values.get("vnp_TxnRef"));
        result.setExternalTransactionId(values.get("vnp_TransactionNo"));
        result.setResponseCode(transactionStatus);
        try {
            result.setAmount(new java.math.BigDecimal(
                    value(values, "vnp_Amount")).movePointLeft(2));
        } catch (NumberFormatException ignored) {
            result.setAmount(java.math.BigDecimal.ZERO);
        }
        result.setCurrency("VND");
        result.setEventType("QUERY");
        result.setOccurredAt(parseProviderTime(values.get("vnp_PayDate")));
        result.setResult("00".equals(transactionStatus) ? "SUCCESS" : "FAILED");
        result.setDeduplicationKey("QUERY:" + value(values, "vnp_TxnRef")
                + ":" + value(values, "vnp_TransactionNo")
                + ":" + transactionStatus);
        Map<String, String> sanitized = new LinkedHashMap<>(values);
        sanitized.remove("vnp_SecureHash");
        result.setSanitizedPayload(json(sanitized));
        return result;
    }

    private String originalTransactionDate(Payment payment) {
        try {
            JsonNode summary = objectMapper.readTree(
                    payment.getLatestProviderSummarySanitized());
            String value = summary.path("createDate").asText();
            if (!value.isBlank()) {
                return value;
            }
        } catch (Exception ignored) {
            // Older rows may not have a createDate in the provider summary.
        }
        return payment.getCreatedAt() == null
                ? null : PROVIDER_TIME.format(payment.getCreatedAt());
    }

    private Instant parseProviderTime(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return java.time.LocalDateTime
                    .parse(value, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .toInstant();
        } catch (RuntimeException exception) {
            return Instant.now();
        }
    }

    private Map<String, String> toStrings(Map<String, Object> values) {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((key, value) ->
                result.put(key, value == null ? "" : String.valueOf(value)));
        return result;
    }

    private String value(Map<String, String> values, String key) {
        return values.getOrDefault(key, "");
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
