package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.*;
import com.project.paymentservice.dto.response.*;
import com.project.paymentservice.entity.AccountingAuditEvent;
import com.project.paymentservice.enumtype.CashVerificationStatus;
import com.project.paymentservice.enumtype.SettlementBatchStatus;
import com.project.paymentservice.service.AccountingOperationsService;
import com.project.paymentservice.service.RefundService;
import com.project.paymentservice.security.AccountingScopeService;
import com.project.paymentservice.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/admin/payments/accounting")
public class AccountingOperationsController {
    private final AccountingOperationsService service;
    private final RefundService refundService;
    private final AccountingScopeService scopeService;
    private final CurrentUserProvider currentUserProvider;

    public AccountingOperationsController(
            AccountingOperationsService service,
            RefundService refundService,
            AccountingScopeService scopeService,
            CurrentUserProvider currentUserProvider) {
        this.service = service;
        this.refundService = refundService;
        this.scopeService = scopeService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AccountingOverviewResponse>> overview(
            @RequestParam(required = false) String cinemaPublicId) {
        return ResponseEntity.ok(ApiResponse.success(service.overview(cinemaPublicId)));
    }

    @GetMapping("/settlements")
    public ResponseEntity<ApiResponse<Page<SettlementBatchResponse>>> settlements(
            @RequestParam(required = false) String cinemaPublicId,
            @RequestParam(required = false) SettlementBatchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.settlements(
                cinemaPublicId, status, PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/settlements/{publicId}")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> settlement(
            @PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.success(service.settlement(publicId)));
    }

    @PostMapping("/settlements")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> importSettlement(
            @Valid @RequestBody SettlementBatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã nhập và tự động đối chiếu lô settlement.",
                service.importSettlement(request)));
    }

    @PostMapping("/settlements/{publicId}/lock")
    public ResponseEntity<ApiResponse<SettlementBatchResponse>> lockSettlement(
            @PathVariable String publicId,
            @Valid @RequestBody SettlementLockRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã khóa lô settlement.",
                service.lockSettlement(publicId, request.expectedVersion(), request.note())));
    }

    @GetMapping("/cash-sessions")
    public ResponseEntity<ApiResponse<Page<CashControlResponse>>> cashSessions(
            @RequestParam(required = false) String cinemaPublicId,
            @RequestParam(required = false) CashVerificationStatus verificationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.cashSessions(
                cinemaPublicId, verificationStatus,
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "closedAt")))));
    }

    @PostMapping("/cash-sessions/{publicId}/verify")
    public ResponseEntity<ApiResponse<CashControlResponse>> verifyCashSession(
            @PathVariable String publicId,
            @Valid @RequestBody CashSessionVerificationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xác minh và lưu biên bản chốt ca.",
                service.verifyCashSession(publicId, request)));
    }

    @PostMapping("/refunds/{paymentPublicId}/requests")
    public ResponseEntity<ApiResponse<com.project.paymentservice.dto.response.RefundResponse>> requestRefund(
            @PathVariable String paymentPublicId,
            @RequestParam(required = false) String cinemaPublicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateRefundRequest request) {
        String cinema = scopeService.resolveCinema(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã gửi đề nghị hoàn tiền để người kiểm soát duyệt.",
                refundService.createAccountingRefundRequest(
                        paymentPublicId, idempotencyKey,
                        currentUserProvider.getCurrentUserId(), cinema, request)));
    }

    @PostMapping("/refunds/{refundPublicId}/approve")
    public ResponseEntity<ApiResponse<com.project.paymentservice.dto.response.RefundResponse>> approveRefund(
            @PathVariable String refundPublicId,
            @RequestParam(required = false) String cinemaPublicId,
            @Valid @RequestBody RefundDecisionRequest request) {
        String cinema = scopeService.resolveCinema(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã duyệt đề nghị hoàn tiền.",
                refundService.approve(refundPublicId, cinema,
                        currentUserProvider.getCurrentUserId(), request.getNote())));
    }

    @PostMapping("/refunds/{refundPublicId}/reject")
    public ResponseEntity<ApiResponse<com.project.paymentservice.dto.response.RefundResponse>> rejectRefund(
            @PathVariable String refundPublicId,
            @RequestParam(required = false) String cinemaPublicId,
            @Valid @RequestBody RefundDecisionRequest request) {
        String cinema = scopeService.resolveCinema(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã từ chối đề nghị hoàn tiền.",
                refundService.reject(refundPublicId, cinema,
                        currentUserProvider.getCurrentUserId(), request.getNote())));
    }

    @GetMapping("/periods")
    public ResponseEntity<ApiResponse<Page<AccountingPeriodResponse>>> periods(
            @RequestParam(required = false) String cinemaPublicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.periods(
                cinemaPublicId, PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "periodStart")))));
    }

    @PostMapping("/periods")
    public ResponseEntity<ApiResponse<AccountingPeriodResponse>> createPeriod(
            @Valid @RequestBody AccountingPeriodRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã mở kỳ kế toán.", service.createPeriod(request)));
    }

    @PostMapping("/periods/{publicId}/actions")
    public ResponseEntity<ApiResponse<AccountingPeriodResponse>> periodAction(
            @PathVariable String publicId,
            @Valid @RequestBody AccountingPeriodActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã cập nhật trạng thái kỳ kế toán.",
                service.applyPeriodAction(publicId, request)));
    }

    @GetMapping("/audit-events")
    public ResponseEntity<ApiResponse<Page<AccountingAuditEvent>>> auditEvents(
            @RequestParam(required = false) String aggregateType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.auditEvents(
                aggregateType, PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")))));
    }
}
