package com.project.paymentservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.service.ProviderCallbackService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class ProviderPaymentController {
    private final ProviderCallbackService callbackService;
    private final PaymentRuntimeProperties runtimeProperties;
    private final ObjectMapper objectMapper;

    public ProviderPaymentController(
            ProviderCallbackService callbackService,
            PaymentRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper) {
        this.callbackService = callbackService;
        this.runtimeProperties = runtimeProperties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/callback/vnpay")
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> parameters) {
        try {
            ProviderCallbackService.CallbackOutcome outcome =
                    callbackService.process(ProviderCode.VNPAY, parameters, "");
            if (!outcome.signatureValid()) {
                return ResponseEntity.ok(vnpayAck("97", "Invalid signature"));
            }
            if (outcome.paymentId() == null) {
                return ResponseEntity.ok(vnpayAck("01", "Order not found"));
            }
            return ResponseEntity.ok(vnpayAck("00", "Confirm Success"));
        } catch (BusinessException exception) {
            return ResponseEntity.ok(vnpayAck(
                    "PAYMENT_AMOUNT_MISMATCH".equals(exception.getErrorCode()) ? "04" : "99",
                    exception.getErrorCode()));
        }
    }

    @PostMapping("/callback/momo")
    public ResponseEntity<Map<String, Object>> momoIpn(@RequestBody String rawBody) {
        Map<String, String> parameters = parseBody(rawBody);
        try {
            ProviderCallbackService.CallbackOutcome outcome =
                    callbackService.process(ProviderCode.MOMO, parameters, rawBody);
            return ResponseEntity.ok(momoAck(parameters,
                    outcome.signatureValid() && outcome.processed() ? 0 : 1,
                    outcome.signatureValid() ? "Success" : "Invalid signature"));
        } catch (BusinessException exception) {
            return ResponseEntity.ok(momoAck(parameters, 1, exception.getErrorCode()));
        }
    }

    @GetMapping("/return/vnpay")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> parameters) {
        return redirect(ProviderCode.VNPAY, callbackService.verifyReturn(
                ProviderCode.VNPAY, parameters));
    }

    @GetMapping("/return/momo")
    public ResponseEntity<Void> momoReturn(@RequestParam Map<String, String> parameters) {
        return redirect(ProviderCode.MOMO, callbackService.verifyReturn(
                ProviderCode.MOMO, parameters));
    }

    private ResponseEntity<Void> redirect(
            ProviderCode provider, ProviderCallbackService.ReturnOutcome outcome) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(runtimeProperties.getFrontendReturnUrl())
                .queryParam("provider", provider.name())
                .queryParam("verified", outcome.signatureValid());
        if (outcome.paymentPublicId() != null) {
            builder.queryParam("paymentPublicId", outcome.paymentPublicId());
        }
        if (outcome.bookingPublicId() != null) {
            builder.queryParam("bookingPublicId", outcome.bookingPublicId());
        }
        URI location = builder.build(true).toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString()).build();
    }

    private Map<String, String> vnpayAck(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }

    private Map<String, Object> momoAck(
            Map<String, String> parameters, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("partnerCode", parameters.getOrDefault("partnerCode", ""));
        response.put("requestId", parameters.getOrDefault("requestId", ""));
        response.put("orderId", parameters.getOrDefault("orderId", ""));
        response.put("resultCode", code);
        response.put("message", message);
        return response;
    }

    private Map<String, String> parseBody(String body) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    body, new TypeReference<Map<String, Object>>() {});
            Map<String, String> result = new LinkedHashMap<>();
            parsed.forEach((key, value) ->
                    result.put(key, value == null ? "" : String.valueOf(value)));
            return result;
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
