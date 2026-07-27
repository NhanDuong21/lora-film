package com.project.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.config.MomoProperties;
import com.project.paymentservice.config.VnPayProperties;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.provider.momo.MomoPaymentProvider;
import com.project.paymentservice.provider.vnpay.VnPayPaymentProvider;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class ProviderSignatureTest {

    @Test
    void vnpayCreatesAmountInMinorUnitsAndVerifiesSignedCallback() throws Exception {
        String secret = "test-vnpay-secret";
        VnPayProperties properties = new VnPayProperties();
        properties.setTmnCode("TESTCODE");
        properties.setHashSecret(secret);
        properties.setReturnUrl("http://localhost/return");
        VnPayPaymentProvider provider =
                new VnPayPaymentProvider(properties, new ObjectMapper());

        PaymentSessionRequest request = request();
        PaymentSession session = provider.createSession(request);
        assertTrue(session.getPaymentUrl().contains("vnp_Amount=15000000"));
        assertTrue(session.getExpiresAt().compareTo(request.getExpiresAt()) <= 0);

        Map<String, String> callback = new TreeMap<>();
        callback.put("vnp_Amount", "15000000");
        callback.put("vnp_ResponseCode", "00");
        callback.put("vnp_TransactionStatus", "00");
        callback.put("vnp_TransactionNo", "998877");
        callback.put("vnp_TxnRef", "PAY-0001");
        callback.put("vnp_SecureHash", hmac(
                "HmacSHA512", secret, canonicalUrlEncoded(callback)));

        ProviderCallbackResult result = provider.verifyCallback(callback, "");
        assertTrue(result.isSignatureValid());
        assertEquals("SUCCESS", result.getResult());
        assertEquals(0, new BigDecimal("150000").compareTo(result.getAmount()));

        callback.put("vnp_Amount", "15000100");
        assertFalse(provider.verifyCallback(callback, "").isSignatureValid());
    }

    @Test
    void momoVerifiesHmacSha256AndRejectsChangedAmount() throws Exception {
        String secret = "test-momo-secret";
        MomoProperties properties = new MomoProperties();
        properties.setPartnerCode("MOMO");
        properties.setAccessKey("ACCESS");
        properties.setSecretKey(secret);
        properties.setRedirectUrl("http://localhost/return");
        properties.setIpnUrl("http://localhost/ipn");
        MomoPaymentProvider provider =
                new MomoPaymentProvider(properties, new ObjectMapper());

        Map<String, String> callback = new LinkedHashMap<>();
        callback.put("partnerCode", "MOMO");
        callback.put("orderId", "PAY-0001");
        callback.put("requestId", "d14bd538-83b8-4778-8200-5a49de7af0df");
        callback.put("amount", "150000");
        callback.put("orderInfo", "Thanh toan don PAY-0001");
        callback.put("orderType", "momo_wallet");
        callback.put("transId", "123456789");
        callback.put("resultCode", "0");
        callback.put("message", "Successful.");
        callback.put("payType", "qr");
        callback.put("responseTime", "1785140000000");
        callback.put("extraData", "");
        callback.put("signature", hmac("HmacSHA256", secret, momoSource(callback)));

        String raw = new ObjectMapper().writeValueAsString(callback);
        ProviderCallbackResult result = provider.verifyCallback(Map.of(), raw);
        assertTrue(result.isSignatureValid());
        assertEquals("SUCCESS", result.getResult());
        assertEquals("PAY-0001", result.getProviderOrderId());

        callback.put("amount", "160000");
        assertFalse(provider.verifyCallback(
                Map.of(), new ObjectMapper().writeValueAsString(callback)).isSignatureValid());
    }

    private PaymentSessionRequest request() {
        PaymentSessionRequest request = new PaymentSessionRequest();
        request.setPaymentPublicId("d14bd538-83b8-4778-8200-5a49de7af0df");
        request.setPaymentTransactionCode("PAY-0001");
        request.setBookingPublicId("74bbbca7-b513-482b-851e-e7cc7a8cf66a");
        request.setAmount(new BigDecimal("150000"));
        request.setCurrency("VND");
        request.setExpiresAt(Instant.now().plusSeconds(600));
        request.setClientIp("127.0.0.1");
        request.setOrderDescription("Thanh toan don PAY-0001");
        return request;
    }

    private String canonicalUrlEncoded(Map<String, String> values) {
        StringBuilder result = new StringBuilder();
        values.entrySet().stream()
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!result.isEmpty()) result.append('&');
                    result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                            .append('=')
                            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                });
        return result.toString();
    }

    private String momoSource(Map<String, String> values) {
        return "accessKey=ACCESS"
                + "&amount=" + values.get("amount")
                + "&extraData=" + values.get("extraData")
                + "&message=" + values.get("message")
                + "&orderId=" + values.get("orderId")
                + "&orderInfo=" + values.get("orderInfo")
                + "&orderType=" + values.get("orderType")
                + "&partnerCode=" + values.get("partnerCode")
                + "&payType=" + values.get("payType")
                + "&requestId=" + values.get("requestId")
                + "&responseTime=" + values.get("responseTime")
                + "&resultCode=" + values.get("resultCode")
                + "&transId=" + values.get("transId");
    }

    private String hmac(String algorithm, String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
