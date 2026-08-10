package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.ReconciliationAssignRequest;
import com.project.paymentservice.dto.request.ReconciliationResolveRequest;
import com.project.paymentservice.dto.request.CreateRefundRequest;
import com.project.paymentservice.dto.request.CompleteCashRefundRequest;
import com.project.paymentservice.dto.response.AdminPaymentDetailResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.dto.response.RefundResponse;
import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.entity.PaymentReconciliationCase;
import com.project.paymentservice.entity.PaymentWebhookEvent;
import com.project.paymentservice.enumtype.OutboxStatus;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.ReconciliationCaseStatus;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import com.project.paymentservice.enumtype.RefundStatus;
import com.project.paymentservice.enumtype.WebhookProcessingStatus;
import com.project.paymentservice.security.CurrentUserProvider;
import com.project.paymentservice.service.AdminPaymentService;
import com.project.paymentservice.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {
    private final AdminPaymentService service;
    private final CurrentUserProvider currentUserProvider;
    private final RefundService refundService;

    public AdminPaymentController(
            AdminPaymentService service,
            CurrentUserProvider currentUserProvider,
            RefundService refundService) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
        this.refundService = refundService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentDetailResponse>>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) ProviderCode provider,
            @RequestParam(required = false) ReconciliationStatus reconciliationStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.search(
                query, status, provider, reconciliationStatus, from, to,
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/{paymentPublicId}")
    public ResponseEntity<ApiResponse<AdminPaymentDetailResponse>> detail(
            @PathVariable String paymentPublicId) {
        return ResponseEntity.ok(ApiResponse.success(service.detail(paymentPublicId)));
    }

    @PostMapping("/{paymentPublicId}/refunds")
    public ResponseEntity<ApiResponse<RefundResponse>> createRefund(
            @PathVariable String paymentPublicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã tiếp nhận yêu cầu hoàn tiền",
                refundService.createAdminRefund(
                        paymentPublicId,
                        idempotencyKey,
                        currentUserProvider.getCurrentUserId(),
                        request)));
    }

    @GetMapping("/refunds")
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> refunds(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(refundService.list(
                status,
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "requestedAt")))));
    }

    @GetMapping("/refunds/{refundPublicId}")
    public ResponseEntity<ApiResponse<RefundResponse>> refundDetail(
            @PathVariable String refundPublicId) {
        return ResponseEntity.ok(ApiResponse.success(
                refundService.detail(refundPublicId)));
    }

    @PostMapping("/refunds/{refundPublicId}/retry")
    public ResponseEntity<ApiResponse<RefundResponse>> retryRefund(
            @PathVariable String refundPublicId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã đưa yêu cầu hoàn tiền vào hàng đợi xử lý lại",
                refundService.retry(refundPublicId)));
    }

    @PostMapping("/refunds/{refundPublicId}/cash/complete")
    public ResponseEntity<ApiResponse<RefundResponse>> completeCashRefund(
            @PathVariable String refundPublicId,
            @Valid @RequestBody CompleteCashRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã ghi nhận hoàn tiền mặt cho khách",
                refundService.completeCashRefund(
                        refundPublicId,
                        currentUserProvider.getCurrentUserId(),
                        request.getProviderReference(),
                        request.getNote())));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) ProviderCode provider,
            @RequestParam(required = false) ReconciliationStatus reconciliationStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=payment-transactions.csv")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(service.exportCsv(query, status, provider, reconciliationStatus, from, to));
    }

    @GetMapping("/webhooks")
    public ResponseEntity<ApiResponse<Page<PaymentWebhookEvent>>> webhooks(
            @RequestParam(required = false) WebhookProcessingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.webhooks(
                status, PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "receivedAt")))));
    }

    @PostMapping("/webhooks/{webhookId}/replay")
    public ResponseEntity<ApiResponse<String>> replayWebhook(@PathVariable Long webhookId) {
        service.replayWebhook(webhookId);
        return ResponseEntity.ok(ApiResponse.success("Đã đưa webhook vào xử lý lại", "OK"));
    }

    @GetMapping("/outbox")
    public ResponseEntity<ApiResponse<Page<PaymentOutboxEvent>>> outbox(
            @RequestParam(required = false) OutboxStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.outbox(
                status, PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @PostMapping("/outbox/{eventId}/replay")
    public ResponseEntity<ApiResponse<String>> replayOutbox(@PathVariable String eventId) {
        service.replayOutbox(eventId);
        return ResponseEntity.ok(ApiResponse.success("Đã đưa outbox vào hàng đợi lại", "OK"));
    }

    @GetMapping("/reconciliations")
    public ResponseEntity<ApiResponse<Page<PaymentReconciliationCase>>> reconciliations(
            @RequestParam(required = false) ReconciliationCaseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.reconciliations(
                status, PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "openedAt")))));
    }

    @PostMapping("/reconciliations/{publicId}/assign")
    public ResponseEntity<ApiResponse<PaymentReconciliationCase>> assign(
            @PathVariable String publicId,
            @Valid @RequestBody ReconciliationAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã phân công hồ sơ đối soát", service.assign(publicId, request)));
    }

    @PostMapping("/reconciliations/{publicId}/resolve")
    public ResponseEntity<ApiResponse<PaymentReconciliationCase>> resolve(
            @PathVariable String publicId,
            @Valid @RequestBody ReconciliationResolveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã đóng hồ sơ đối soát",
                service.resolve(publicId, currentUserProvider.getCurrentUserId(), request)));
    }
}
