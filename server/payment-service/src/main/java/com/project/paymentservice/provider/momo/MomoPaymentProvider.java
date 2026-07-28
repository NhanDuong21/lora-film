package com.project.paymentservice.provider.momo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.config.MomoProperties;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.provider.ProviderCrypto;
import com.project.paymentservice.provider.ProviderHttpClientFactory;
import com.project.paymentservice.provider.ProviderSessionUncertainException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "payment.providers.momo.enabled", havingValue = "true")
public class MomoPaymentProvider implements PaymentProvider {
    private final MomoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MomoPaymentProvider(MomoProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        require(properties.getPartnerCode(), "payment.providers.momo.partner-code");
        require(properties.getAccessKey(), "payment.providers.momo.access-key");
        require(properties.getSecretKey(), "payment.providers.momo.secret-key");
        require(properties.getRedirectUrl(), "payment.providers.momo.redirect-url");
        require(properties.getIpnUrl(), "payment.providers.momo.ipn-url");
        this.httpClient = ProviderHttpClientFactory.create(
                Duration.ofMillis(properties.getConnectTimeoutMillis()));
    }

    @Override
    public ProviderCode providerCode() {
        return ProviderCode.MOMO;
    }

    @Override
    public PaymentSession createSession(PaymentSessionRequest request) {
        long amount = request.getAmount().longValueExact();
        if (amount < 1_000 || amount > 50_000_000) {
            throw new BusinessException("MOMO_AMOUNT_OUT_OF_RANGE",
                    "Số tiền không nằm trong giới hạn MoMo Sandbox", HttpStatus.BAD_REQUEST);
        }
        String orderId = request.getPaymentTransactionCode();
        String requestId = request.getPaymentPublicId();
        String orderInfo = safeDescription(request);
        String extraData = "";
        String signatureSource = "accessKey=" + properties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + properties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + properties.getPartnerCode()
                + "&redirectUrl=" + properties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=captureWallet";
        String signature = ProviderCrypto.hmacHex(
                "HmacSHA256", properties.getSecretKey(), signatureSource);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", properties.getPartnerCode());
        payload.put("partnerName", "LoraFilm");
        payload.put("storeId", "LoraFilm");
        payload.put("requestId", requestId);
        payload.put("amount", amount);
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", properties.getRedirectUrl());
        payload.put("ipnUrl", properties.getIpnUrl());
        payload.put("lang", "vi");
        payload.put("requestType", "captureWallet");
        payload.put("autoCapture", true);
        payload.put("extraData", extraData);
        payload.put("signature", signature);

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getCreateUrl()))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw uncertain(orderId, requestId, "MoMo HTTP " + response.statusCode(), null);
            }
            JsonNode json = objectMapper.readTree(response.body());
            int resultCode = json.path("resultCode").asInt(-1);
            if (resultCode != 0 || json.path("payUrl").asText().isBlank()) {
                throw new BusinessException("MOMO_SESSION_REJECTED",
                        "MoMo từ chối khởi tạo phiên: " + resultCode,
                        HttpStatus.BAD_GATEWAY);
            }
            Instant expiry = request.getExpiresAt().isBefore(Instant.now().plusSeconds(900))
                    ? request.getExpiresAt() : Instant.now().plusSeconds(900);
            PaymentSession session = new PaymentSession(
                    orderId,
                    json.path("requestId").asText(requestId),
                    json.path("payUrl").asText(),
                    expiry);
            session.setSanitizedProviderSummary(objectMapper.writeValueAsString(Map.of(
                    "provider", "MOMO",
                    "resultCode", resultCode,
                    "requestId", requestId,
                    "orderId", orderId)));
            return session;
        } catch (BusinessException | ProviderSessionUncertainException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw uncertain(orderId, requestId, "MoMo request interrupted", exception);
        } catch (Exception exception) {
            throw uncertain(orderId, requestId, exception.getMessage(), exception);
        }
    }

    @Override
    public Optional<ProviderCallbackResult> queryStatus(Payment payment) {
        String requestId = payment.getPublicId();
        String orderId = payment.getProviderOrderId() == null
                ? payment.getPaymentTransactionCode() : payment.getProviderOrderId();
        String source = "accessKey=" + properties.getAccessKey()
                + "&orderId=" + orderId
                + "&partnerCode=" + properties.getPartnerCode()
                + "&requestId=" + requestId;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", properties.getPartnerCode());
        payload.put("requestId", requestId);
        payload.put("orderId", orderId);
        payload.put("lang", "vi");
        payload.put("signature", ProviderCrypto.hmacHex(
                "HmacSHA256", properties.getSecretKey(), source));
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getQueryUrl()))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            Map<String, Object> parsed = objectMapper.readValue(
                    response.body(), new TypeReference<Map<String, Object>>() {});
            String resultCode = String.valueOf(parsed.getOrDefault("resultCode", ""));
            if ("1000".equals(resultCode)
                    || "7000".equals(resultCode)
                    || "7002".equals(resultCode)) {
                return Optional.empty();
            }
            Map<String, String> values = toStrings(parsed);
            if (!properties.getPartnerCode().equals(value(values, "partnerCode"))
                    || !orderId.equals(value(values, "orderId"))
                    || !requestId.equals(value(values, "requestId"))) {
                return Optional.empty();
            }
            /*
             * MoMo's Query API signs the merchant request but its documented
             * response does not contain a signature. Trust the HTTPS
             * server-to-server response only after its echoed merchant,
             * payment and request identifiers have all matched. Some
             * environments may include a signature; reject it when present
             * but invalid instead of silently downgrading verification.
             */
            if (!value(values, "signature").isBlank()) {
                ProviderCallbackResult signed = verify(values, "QUERY");
                if (!signed.isSignatureValid()) {
                    return Optional.empty();
                }
            }
            return Optional.of(toTrustedQueryResult(values));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    @Override
    public ProviderCallbackResult verifyCallback(Map<String, String> parameters, String rawBody) {
        try {
            Map<String, Object> body = objectMapper.readValue(
                    rawBody, new TypeReference<Map<String, Object>>() {});
            return verify(toStrings(body), "IPN");
        } catch (Exception exception) {
            ProviderCallbackResult invalid = new ProviderCallbackResult();
            invalid.setSignatureValid(false);
            invalid.setDeduplicationKey("INVALID:" + Integer.toHexString(rawBody.hashCode()));
            invalid.setResult("FAILED");
            invalid.setEventType("IPN");
            invalid.setOccurredAt(Instant.now());
            invalid.setSanitizedPayload("{}");
            return invalid;
        }
    }

    @Override
    public ProviderCallbackResult verifyReturn(Map<String, String> parameters) {
        return verify(parameters, "RETURN");
    }

    private ProviderCallbackResult verify(Map<String, String> values, String eventType) {
        String source = "accessKey=" + properties.getAccessKey()
                + "&amount=" + value(values, "amount")
                + "&extraData=" + value(values, "extraData")
                + "&message=" + value(values, "message")
                + "&orderId=" + value(values, "orderId")
                + "&orderInfo=" + value(values, "orderInfo")
                + "&orderType=" + value(values, "orderType")
                + "&partnerCode=" + value(values, "partnerCode")
                + "&payType=" + value(values, "payType")
                + "&requestId=" + value(values, "requestId")
                + "&responseTime=" + value(values, "responseTime")
                + "&resultCode=" + value(values, "resultCode")
                + "&transId=" + value(values, "transId");
        String expected = ProviderCrypto.hmacHex("HmacSHA256", properties.getSecretKey(), source);
        boolean valid = ProviderCrypto.constantTimeEquals(expected, values.get("signature"));
        String resultCode = value(values, "resultCode");

        ProviderCallbackResult result = new ProviderCallbackResult();
        result.setSignatureValid(valid);
        result.setProviderOrderId(values.get("orderId"));
        result.setExternalTransactionId(values.get("transId"));
        result.setResponseCode(resultCode);
        try {
            result.setAmount(new java.math.BigDecimal(value(values, "amount")));
        } catch (NumberFormatException ignored) {
            result.setAmount(java.math.BigDecimal.ZERO);
        }
        result.setCurrency("VND");
        result.setEventType(eventType);
        result.setOccurredAt(Instant.now());
        result.setResult("0".equals(resultCode) ? "SUCCESS"
                : ("1006".equals(resultCode) ? "CANCELLED" : "FAILED"));
        result.setDeduplicationKey(eventType + ":" + value(values, "orderId")
                + ":" + value(values, "transId") + ":" + resultCode);
        Map<String, String> sanitized = new LinkedHashMap<>(values);
        sanitized.remove("signature");
        try {
            result.setSanitizedPayload(objectMapper.writeValueAsString(sanitized));
        } catch (Exception ignored) {
            result.setSanitizedPayload("{}");
        }
        return result;
    }

    private ProviderCallbackResult toTrustedQueryResult(Map<String, String> values) {
        ProviderCallbackResult result = new ProviderCallbackResult();
        result.setSignatureValid(true);
        result.setProviderOrderId(value(values, "orderId"));
        result.setExternalTransactionId(value(values, "transId"));
        String resultCode = value(values, "resultCode");
        result.setResponseCode(resultCode);
        try {
            result.setAmount(new java.math.BigDecimal(value(values, "amount")));
        } catch (NumberFormatException ignored) {
            result.setAmount(java.math.BigDecimal.ZERO);
        }
        result.setCurrency("VND");
        result.setEventType("QUERY");
        result.setOccurredAt(Instant.now());
        result.setResult("0".equals(resultCode) ? "SUCCESS"
                : ("1006".equals(resultCode) ? "CANCELLED" : "FAILED"));
        result.setDeduplicationKey("QUERY:" + value(values, "orderId")
                + ":" + value(values, "transId") + ":" + resultCode);
        Map<String, String> sanitized = new LinkedHashMap<>(values);
        sanitized.remove("signature");
        try {
            result.setSanitizedPayload(objectMapper.writeValueAsString(sanitized));
        } catch (Exception ignored) {
            result.setSanitizedPayload("{}");
        }
        return result;
    }

    private Map<String, String> toStrings(Map<String, Object> values) {
        Map<String, String> converted = new LinkedHashMap<>();
        values.forEach((key, value) -> converted.put(key, value == null ? "" : String.valueOf(value)));
        return converted;
    }

    private String value(Map<String, String> values, String key) {
        return values.getOrDefault(key, "");
    }

    private String safeDescription(PaymentSessionRequest request) {
        String value = request.getOrderDescription();
        if (value == null || value.isBlank()) {
            value = "Thanh toan don " + request.getPaymentTransactionCode();
        }
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    private ProviderSessionUncertainException uncertain(
            String orderId, String requestId, String detail, Throwable cause) {
        return new ProviderSessionUncertainException(
                "Chưa xác định được kết quả khởi tạo phiên MoMo",
                orderId,
                requestId,
                "{\"provider\":\"MOMO\",\"state\":\"UNCERTAIN\"}",
                cause);
    }

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured when MoMo is enabled");
        }
    }
}
