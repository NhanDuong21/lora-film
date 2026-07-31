package com.project.paymentservice.provider.vnpay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.config.VnPayProperties;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentRefund;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.RefundType;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.provider.ProviderCrypto;
import com.project.paymentservice.provider.ProviderHttpClientFactory;
import com.project.paymentservice.provider.ProviderRefundResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(VnPayPaymentProvider.class);
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
        require(properties.getRefundUrl(), "payment.providers.vnpay.refund-url");
        this.httpClient = ProviderHttpClientFactory.create(
                Duration.ofMillis(properties.getConnectTimeoutMillis()));
    }

    @Override
    public ProviderCode providerCode() {
        return ProviderCode.VNPAY;
    }

    @Override
    public int recoveryRetryDelaySeconds() {
        return properties.getQueryRetryDelaySeconds();
    }

    @Override
    public PaymentSession createSession(PaymentSessionRequest request) {
        Instant now = Instant.now();
        Instant expiry = request.getExpiresAt().isBefore(now.plusSeconds(900))
                ? request.getExpiresAt() : now.plusSeconds(900);
        String providerTransactionCode = providerTransactionCode(request);
        TreeMap<String, String> fields = new TreeMap<>();
        fields.put("vnp_Version", properties.getVersion());
        fields.put("vnp_Command", properties.getCommand());
        fields.put("vnp_TmnCode", properties.getTmnCode());
        fields.put("vnp_Amount", request.getAmount().movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).toPlainString());
        fields.put("vnp_CurrCode", request.getCurrency());
        fields.put("vnp_TxnRef", providerTransactionCode);
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
                providerTransactionCode,
                providerTransactionCode,
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
                log.warn("VNPay QueryDR HTTP failure: txnRef={}, status={}",
                        transactionCode, response.statusCode());
                return Optional.empty();
            }
            Map<String, Object> raw = objectMapper.readValue(
                    response.body(), new TypeReference<Map<String, Object>>() {});
            Map<String, String> resultValues = toStrings(raw);
            if (!"00".equals(resultValues.get("vnp_ResponseCode"))) {
                log.info("VNPay QueryDR deferred: txnRef={}, responseCode={}, message={}",
                        transactionCode,
                        resultValues.get("vnp_ResponseCode"),
                        resultValues.getOrDefault("vnp_Message", ""));
                return Optional.empty();
            }
            ProviderCallbackResult result = verifyQueryResponse(resultValues);
            if (!queryResponseIsTrusted(payment, transactionCode, resultValues, result)) {
                log.warn("VNPay QueryDR response rejected by integrity checks: txnRef={}",
                        transactionCode);
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
                log.info("VNPay QueryDR is not terminal yet: txnRef={}, transactionStatus={}",
                        transactionCode, transactionStatus);
                return Optional.empty();
            }
            return Optional.of(result);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("VNPay QueryDR request failed: txnRef={}, error={}",
                    transactionCode, exception.getClass().getSimpleName());
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

    @Override
    public ProviderRefundResult refund(Payment payment, PaymentRefund refund) {
        String requestId = refund.getPublicId().replace("-", "");
        String createDate = PROVIDER_TIME.format(Instant.now());
        String transactionDate = originalTransactionDate(payment);
        if (transactionDate == null
                || payment.getExternalTransactionId() == null
                || payment.getExternalTransactionId().isBlank()) {
            throw new IllegalStateException("VNPay original transaction data is incomplete");
        }
        String transactionType = refund.getRefundType() == RefundType.FULL ? "02" : "03";
        String transactionCode = payment.getProviderOrderId() == null
                ? payment.getPaymentTransactionCode() : payment.getProviderOrderId();
        String amount = refund.getRequestedAmount().movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        String orderInfo = "Hoan tien " + payment.getPaymentTransactionCode();
        String createBy = "LoraFilm";
        String ipAddress = normalizeIp(properties.getQueryIpAddress());

        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_RequestId", requestId);
        values.put("vnp_Version", properties.getVersion());
        values.put("vnp_Command", "refund");
        values.put("vnp_TmnCode", properties.getTmnCode());
        values.put("vnp_TransactionType", transactionType);
        values.put("vnp_TxnRef", transactionCode);
        values.put("vnp_Amount", amount);
        values.put("vnp_TransactionNo", payment.getExternalTransactionId());
        values.put("vnp_TransactionDate", transactionDate);
        values.put("vnp_CreateBy", createBy);
        values.put("vnp_CreateDate", createDate);
        values.put("vnp_IpAddr", ipAddress);
        values.put("vnp_OrderInfo", orderInfo);
        String signatureSource = String.join("|",
                requestId,
                properties.getVersion(),
                "refund",
                properties.getTmnCode(),
                transactionType,
                transactionCode,
                amount,
                payment.getExternalTransactionId(),
                transactionDate,
                createBy,
                createDate,
                ipAddress,
                orderInfo);
        values.put("vnp_SecureHash", ProviderCrypto.hmacHex(
                "HmacSHA512", properties.getHashSecret(), signatureSource));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getRefundUrl()))
                    .timeout(Duration.ofMillis(properties.getReadTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(values)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("VNPay refund HTTP " + response.statusCode());
            }
            Map<String, String> body = toStrings(objectMapper.readValue(
                    response.body(), new TypeReference<Map<String, Object>>() {}));
            return refundResponse(refund, body);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("VNPay refund interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("VNPay refund result is uncertain", exception);
        }
    }

    @Override
    public Optional<ProviderRefundResult> queryRefund(
            Payment payment,
            PaymentRefund refund) {
        String transactionDate = originalTransactionDate(payment);
        if (transactionDate == null) {
            return Optional.empty();
        }
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = PROVIDER_TIME.format(Instant.now());
        String transactionCode = payment.getProviderOrderId() == null
                ? payment.getPaymentTransactionCode() : payment.getProviderOrderId();
        String orderInfo = "Truy van hoan tien " + refund.getRefundCode();
        String ipAddress = normalizeIp(properties.getQueryIpAddress());

        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_RequestId", requestId);
        values.put("vnp_Version", properties.getVersion());
        values.put("vnp_Command", "querydr");
        values.put("vnp_TmnCode", properties.getTmnCode());
        values.put("vnp_TxnRef", transactionCode);
        values.put("vnp_OrderInfo", orderInfo);
        if (refund.getProviderRefundId() != null
                && !refund.getProviderRefundId().isBlank()) {
            values.put("vnp_TransactionNo", refund.getProviderRefundId());
        }
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
                log.warn("VNPay refund QueryDR HTTP failure: refundCode={}, status={}",
                        refund.getRefundCode(), response.statusCode());
                return Optional.empty();
            }
            Map<String, String> resultValues = toStrings(objectMapper.readValue(
                    response.body(), new TypeReference<Map<String, Object>>() {}));
            String responseCode = value(resultValues, "vnp_ResponseCode");
            if ("94".equals(responseCode)) {
                log.info(
                        "VNPay refund QueryDR throttled: refundCode={}, responseCode={}, message={}",
                        refund.getRefundCode(),
                        responseCode,
                        value(resultValues, "vnp_Message"));
                return Optional.of(throttledRefundQueryResult(
                        refund, resultValues));
            }
            if (!"00".equals(responseCode)) {
                log.info(
                        "VNPay refund QueryDR unavailable: refundCode={}, responseCode={}, message={}",
                        refund.getRefundCode(),
                        responseCode,
                        value(resultValues, "vnp_Message"));
                return Optional.of(unavailableRefundQueryResult(
                        refund, resultValues));
            }
            return Optional.of(refundQueryResponse(payment, refund, resultValues));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("VNPay refund QueryDR failed: refundCode={}, error={}",
                    refund.getRefundCode(), exception.getMessage());
            return Optional.empty();
        }
    }

    private ProviderRefundResult throttledRefundQueryResult(
            PaymentRefund refund,
            Map<String, String> values) {
        ProviderRefundResult result = new ProviderRefundResult();
        result.setState(ProviderRefundResult.State.PROCESSING);
        result.setRetryAfterSeconds(refundQueryThrottleDelay(refund));
        result.setProviderOrderId(refund.getProviderOrderId());
        result.setProviderRequestId(refund.getProviderRequestId());
        result.setProviderRefundId(refund.getProviderRefundId());
        result.setResponseCode("94");
        result.setFailureCode("VNPAY_QUERYDR_RATE_LIMITED");
        result.setMessageSanitized(
                "VNPay đang giới hạn tần suất tra cứu; hệ thống sẽ tự kiểm tra lại theo khoảng chờ tăng dần");
        Map<String, String> sanitized = new LinkedHashMap<>(values);
        sanitized.remove("vnp_SecureHash");
        result.setSummarySanitized(json(sanitized));
        return result;
    }

    private ProviderRefundResult unavailableRefundQueryResult(
            PaymentRefund refund,
            Map<String, String> values) {
        String responseCode = value(values, "vnp_ResponseCode");
        String providerMessage = value(values, "vnp_Message");
        ProviderRefundResult result = new ProviderRefundResult();
        result.setState(ProviderRefundResult.State.PROCESSING);
        result.setRetryAfterSeconds(refundQueryThrottleDelay(refund));
        result.setProviderOrderId(refund.getProviderOrderId());
        result.setProviderRequestId(refund.getProviderRequestId());
        result.setProviderRefundId(refund.getProviderRefundId());
        result.setResponseCode(responseCode);
        result.setFailureCode("VNPAY_QUERYDR_RESPONSE_"
                + (responseCode.isBlank() ? "UNKNOWN" : responseCode));
        result.setMessageSanitized(providerMessage.isBlank()
                ? "VNPay chưa trả về kết quả tra cứu hoàn tiền xác định"
                : "VNPay trả về khi tra cứu: " + providerMessage);
        Map<String, String> sanitized = new LinkedHashMap<>(values);
        sanitized.remove("vnp_SecureHash");
        result.setSummarySanitized(json(sanitized));
        return result;
    }

    private int refundQueryThrottleDelay(PaymentRefund refund) {
        int priorAttempts = Math.max(0, refund.getRetryCount());
        int exponent = Math.min(priorAttempts, 4);
        long delay = (long) properties.getQueryRetryDelaySeconds() << exponent;
        return (int) Math.min(delay, properties.getQueryRetryMaxDelaySeconds());
    }

    private ProviderRefundResult refundResponse(
            PaymentRefund refund,
            Map<String, String> values) {
        String responseCode = value(values, "vnp_ResponseCode");
        String secureHash = values.get("vnp_SecureHash");
        if ("94".equals(responseCode)
                && (secureHash == null || secureHash.isBlank())) {
            /*
             * VNPay sandbox returns only responseCode/message for a duplicate
             * refund request, despite documenting vnp_SecureHash as mandatory.
             * This response must never complete a refund. It only tells the
             * worker to wait for the provider QueryDR cooldown and verify the
             * eventual signed terminal result.
             */
            log.info(
                    "VNPay duplicate refund acknowledged without a signed terminal result: "
                            + "refundCode={}, retryAfterSeconds={}",
                    refund.getRefundCode(),
                    properties.getQueryRetryDelaySeconds());
            ProviderRefundResult duplicate = new ProviderRefundResult();
            duplicate.setProviderOrderId(refund.getRefundCode());
            duplicate.setProviderRequestId(refund.getPublicId());
            duplicate.setProviderRefundId(refund.getProviderRefundId());
            duplicate.setResponseCode(responseCode);
            duplicate.setRetryAfterSeconds(properties.getQueryRetryDelaySeconds());
            Map<String, String> sanitized = new LinkedHashMap<>(values);
            sanitized.remove("vnp_SecureHash");
            duplicate.setSummarySanitized(json(sanitized));
            applyRefundState(duplicate, "");
            return duplicate;
        }
        String documentedSignatureSource = String.join("|",
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
                value(values, "vnp_OrderInfo"));
        String compatibilitySignatureSource = documentedSignatureSource
                + "|" + value(values, "vnp_PromotionCode")
                + "|" + value(values, "vnp_PromotionAmount");
        String documentedExpected = ProviderCrypto.hmacHex(
                "HmacSHA512", properties.getHashSecret(), documentedSignatureSource);
        String compatibilityExpected = ProviderCrypto.hmacHex(
                "HmacSHA512", properties.getHashSecret(), compatibilitySignatureSource);
        boolean signatureMatches = ProviderCrypto.constantTimeEquals(
                    documentedExpected, secureHash)
                || ProviderCrypto.constantTimeEquals(
                    compatibilityExpected, secureHash);
        String responseTmnCode = value(values, "vnp_TmnCode");
        String responseCommand = value(values, "vnp_Command");
        boolean tmnCodeMatches = responseTmnCode.isBlank()
                || properties.getTmnCode().equals(responseTmnCode);
        boolean commandMatches = responseCommand.isBlank()
                || "refund".equalsIgnoreCase(responseCommand);
        if (!signatureMatches || !tmnCodeMatches || !commandMatches) {
            logRefundIntegrityFailure(
                    refund, values, signatureMatches, tmnCodeMatches, commandMatches);
            throw new IllegalStateException(
                    "VNPay refund response failed integrity checks"
                            + " (signature=" + signatureMatches
                            + ", tmnCode=" + tmnCodeMatches
                            + ", command=" + commandMatches + ")");
        }
        Payment payment = refund.getPayment();
        String expectedTransactionCode = payment.getProviderOrderId() == null
                ? payment.getPaymentTransactionCode() : payment.getProviderOrderId();
        String expectedAmount = refund.getRequestedAmount().movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        String expectedTransactionType =
                refund.getRefundType() == RefundType.FULL ? "02" : "03";
        boolean duplicateProcessing = "94".equals(responseCode);
        boolean transactionMatches = duplicateProcessing
                ? blankOrEquals(value(values, "vnp_TxnRef"), expectedTransactionCode)
                    && blankOrEquals(value(values, "vnp_Amount"), expectedAmount)
                    && blankOrEquals(
                        value(values, "vnp_TransactionType"),
                        expectedTransactionType)
                : expectedTransactionCode.equals(value(values, "vnp_TxnRef"))
                    && expectedAmount.equals(value(values, "vnp_Amount"))
                    && expectedTransactionType.equals(
                        value(values, "vnp_TransactionType"));
        if (!transactionMatches) {
            throw new IllegalStateException(
                    "VNPay refund response does not match the requested transaction");
        }
        ProviderRefundResult result = new ProviderRefundResult();
        result.setProviderOrderId(refund.getRefundCode());
        result.setProviderRequestId(refund.getPublicId());
        result.setProviderRefundId(value(values, "vnp_TransactionNo"));
        result.setResponseCode(responseCode);
        result.setMessageSanitized(value(values, "vnp_Message"));
        Map<String, String> sanitized = new LinkedHashMap<>(values);
        sanitized.remove("vnp_SecureHash");
        result.setSummarySanitized(json(sanitized));
        applyRefundState(result, value(values, "vnp_TransactionStatus"));
        return result;
    }

    private boolean blankOrEquals(String actual, String expected) {
        return actual == null || actual.isBlank() || expected.equals(actual);
    }

    private void logRefundIntegrityFailure(
            PaymentRefund refund,
            Map<String, String> values,
            boolean signatureMatches,
            boolean tmnCodeMatches,
            boolean commandMatches) {
        Map<String, String> sanitized = new LinkedHashMap<>(values);
        String secureHash = sanitized.remove("vnp_SecureHash");
        log.warn(
                "VNPay refund response integrity failure: refundCode={}, "
                        + "signatureMatch={}, tmnCodeMatch={}, commandMatch={}, "
                        + "secureHashPresent={}, secureHashLength={}, response={}",
                refund.getRefundCode(),
                signatureMatches,
                tmnCodeMatches,
                commandMatches,
                secureHash != null && !secureHash.isBlank(),
                secureHash == null ? 0 : secureHash.length(),
                json(sanitized));
    }

    private ProviderRefundResult refundQueryResponse(
            Payment payment,
            PaymentRefund refund,
            Map<String, String> values) {
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
        String expectedSignature = ProviderCrypto.hmacHex(
                "HmacSHA512", properties.getHashSecret(), signatureSource);
        String transactionCode = payment.getProviderOrderId() == null
                ? payment.getPaymentTransactionCode() : payment.getProviderOrderId();
        String expectedAmount = refund.getRequestedAmount().movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        String expectedTransactionType =
                refund.getRefundType() == RefundType.FULL ? "02" : "03";
        String responseTransactionNo = value(values, "vnp_TransactionNo");
        boolean refundIdMatches = refund.getProviderRefundId() == null
                || refund.getProviderRefundId().isBlank()
                || refund.getProviderRefundId().equals(responseTransactionNo);

        if (!properties.getTmnCode().equals(value(values, "vnp_TmnCode"))
                || !"querydr".equalsIgnoreCase(value(values, "vnp_Command"))
                || value(values, "vnp_ResponseId").isBlank()
                || responseTransactionNo.isBlank()
                || !ProviderCrypto.constantTimeEquals(
                    expectedSignature, values.get("vnp_SecureHash"))
                || !transactionCode.equals(value(values, "vnp_TxnRef"))
                || !expectedAmount.equals(value(values, "vnp_Amount"))
                || !expectedTransactionType.equals(
                    value(values, "vnp_TransactionType"))
                || !refundIdMatches) {
            throw new IllegalStateException(
                    "VNPay refund QueryDR response failed integrity checks");
        }

        ProviderRefundResult result = new ProviderRefundResult();
        result.setProviderOrderId(refund.getRefundCode());
        result.setProviderRequestId(refund.getPublicId());
        result.setProviderRefundId(responseTransactionNo);
        result.setResponseCode(value(values, "vnp_ResponseCode"));
        result.setOccurredAt(parseProviderTime(values.get("vnp_PayDate")));
        Map<String, String> sanitized = new LinkedHashMap<>(values);
        sanitized.remove("vnp_SecureHash");
        result.setSummarySanitized(json(sanitized));
        applyRefundState(result, value(values, "vnp_TransactionStatus"));
        return result;
    }

    private void applyRefundState(
            ProviderRefundResult result,
            String transactionStatus) {
        String responseCode = result.getResponseCode();
        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            result.setState(ProviderRefundResult.State.SUCCESS);
            result.setFailureCode(null);
            result.setMessageSanitized("VNPay xác nhận hoàn tiền thành công");
            return;
        }
        if ("94".equals(responseCode)) {
            result.setState(ProviderRefundResult.State.PROCESSING);
            result.setRetryAfterSeconds(properties.getQueryRetryDelaySeconds());
            result.setFailureCode("VNPAY_REFUND_DUPLICATE_ACCEPTED");
            result.setMessageSanitized(
                    "VNPay xác nhận giao dịch đã có yêu cầu hoàn tiền trước đó; hệ thống sẽ tra cứu kết quả");
            return;
        }
        if ("00".equals(responseCode)
                && ("01".equals(transactionStatus)
                || "05".equals(transactionStatus)
                || "06".equals(transactionStatus)
                || transactionStatus.isBlank())) {
            result.setState(ProviderRefundResult.State.PROCESSING);
            result.setRetryAfterSeconds(properties.getQueryRetryDelaySeconds());
            result.setFailureCode("VNPAY_REFUND_PROCESSING_"
                    + (transactionStatus.isBlank() ? "UNKNOWN" : transactionStatus));
            result.setMessageSanitized(switch (transactionStatus) {
                case "05" -> "VNPay đang xử lý yêu cầu hoàn tiền";
                case "06" -> "VNPay đã chuyển yêu cầu hoàn tiền sang ngân hàng";
                case "01" -> "Yêu cầu hoàn tiền chưa hoàn tất tại VNPay";
                default -> "VNPay đã tiếp nhận yêu cầu nhưng chưa trả về trạng thái hoàn tiền";
            });
            return;
        }
        if ("99".equals(responseCode)
                || ("00".equals(responseCode)
                    && ("04".equals(transactionStatus)
                        || "07".equals(transactionStatus)))) {
            result.setState(ProviderRefundResult.State.PROCESSING);
            result.setRetryAfterSeconds(properties.getQueryRetryDelaySeconds());
            result.setFailureCode("VNPAY_REFUND_REVIEW_"
                    + (transactionStatus.isBlank() ? responseCode : transactionStatus));
            result.setMessageSanitized(
                    "Kết quả hoàn tiền tại VNPay chưa xác định và cần tiếp tục kiểm tra");
            return;
        }

        result.setState(ProviderRefundResult.State.FAILED);
        String terminalCode = transactionStatus.isBlank()
                ? responseCode : transactionStatus;
        result.setFailureCode("VNPAY_REFUND_" + terminalCode);
        result.setMessageSanitized("VNPay từ chối yêu cầu hoàn tiền (mã "
                + terminalCode + ")");
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
        boolean isSuccess = "00".equals(responseCode) && ("00".equals(transactionStatus) || transactionStatus == null || transactionStatus.isEmpty());
        boolean isCancelled = "24".equals(responseCode) || "24".equals(transactionStatus);
        result.setResult(isSuccess ? "SUCCESS" : (isCancelled ? "CANCELLED" : "FAILED"));
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
        String secureHash = values.get("vnp_SecureHash");
        result.setSignatureValid(secureHash != null
                && !secureHash.isBlank()
                && ProviderCrypto.constantTimeEquals(expected, secureHash));
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

    private boolean queryResponseIsTrusted(
            Payment payment,
            String transactionCode,
            Map<String, String> values,
            ProviderCallbackResult result) {
        if (!result.isSignatureValid()
                || !properties.getTmnCode().equals(values.get("vnp_TmnCode"))
                || !transactionCode.equals(result.getProviderOrderId())) {
            return false;
        }
        String command = value(values, "vnp_Command");
        if (!command.isBlank() && !"querydr".equalsIgnoreCase(command)) {
            return false;
        }
        if (value(values, "vnp_ResponseId").isBlank()
                || value(values, "vnp_TransactionNo").isBlank()) {
            return false;
        }
        if (payment.getAmount() == null
                || payment.getCurrency() == null
                || !"VND".equalsIgnoreCase(payment.getCurrency())) {
            return false;
        }
        try {
            java.math.BigDecimal providerAmount = new java.math.BigDecimal(
                    value(values, "vnp_Amount")).movePointLeft(2);
            return providerAmount.compareTo(payment.getAmount()) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
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

    private String providerTransactionCode(PaymentSessionRequest request) {
        String value = request.getPaymentTransactionCode() == null
                ? "" : request.getPaymentTransactionCode().replaceAll("[^A-Za-z0-9]", "");
        if (value.isBlank() && request.getPaymentPublicId() != null) {
            value = request.getPaymentPublicId().replaceAll("[^A-Za-z0-9]", "");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("VNPay transaction reference must not be blank");
        }
        return value.length() > 100 ? value.substring(0, 100) : value;
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
