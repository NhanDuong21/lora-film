package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.dto.request.RefundDecisionRequest;
import com.project.paymentservice.dto.response.AdminPaymentDetailResponse;
import com.project.paymentservice.dto.response.ManagerPaymentDetailResponse;
import com.project.paymentservice.dto.response.ManagerPaymentSummaryResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.dto.response.RefundResponse;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import com.project.paymentservice.enumtype.RefundStatus;
import com.project.paymentservice.security.CurrentUserProvider;
import com.project.paymentservice.security.ManagerCinemaScopeService;
import com.project.paymentservice.service.AdminPaymentService;
import com.project.paymentservice.service.RefundService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/payments")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerPaymentController {
    private final AdminPaymentService paymentService;
    private final RefundService refundService;
    private final ManagerCinemaScopeService cinemaScope;
    private final CurrentUserProvider currentUserProvider;

    public ManagerPaymentController(
            AdminPaymentService paymentService,
            RefundService refundService,
            ManagerCinemaScopeService cinemaScope,
            CurrentUserProvider currentUserProvider) {
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.cinemaScope = cinemaScope;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentDetailResponse>>> search(
            @RequestParam String cinemaPublicId,
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
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.success(paymentService.searchForCinema(
                cinema, query, status, provider, reconciliationStatus, from, to,
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ManagerPaymentSummaryResponse>> summary(
            @RequestParam String cinemaPublicId) {
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.success(paymentService.summaryForCinema(cinema)));
    }

    @GetMapping("/{paymentPublicId}")
    public ResponseEntity<ApiResponse<ManagerPaymentDetailResponse>> detail(
            @RequestParam String cinemaPublicId,
            @PathVariable String paymentPublicId) {
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        AdminPaymentDetailResponse detail = paymentService.detailForCinema(cinema, paymentPublicId);
        return ResponseEntity.ok(ApiResponse.success(new ManagerPaymentDetailResponse(
                detail.payment(), detail.analyticsSnapshot(), detail.refunds())));
    }

    @GetMapping("/refund-requests")
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> refundRequests(
            @RequestParam String cinemaPublicId,
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.success(refundService.listForCinema(
                cinema, status,
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "requestedAt")))));
    }

    @PostMapping("/refund-requests/{refundPublicId}/approve")
    public ResponseEntity<ApiResponse<RefundResponse>> approve(
            @RequestParam String cinemaPublicId,
            @PathVariable String refundPublicId,
            @Valid @RequestBody RefundDecisionRequest request) {
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã duyệt yêu cầu hoàn tiền",
                refundService.approve(refundPublicId, cinema,
                        currentUserProvider.getCurrentUserId(), request.getNote())));
    }

    @PostMapping("/refund-requests/{refundPublicId}/reject")
    public ResponseEntity<ApiResponse<RefundResponse>> reject(
            @RequestParam String cinemaPublicId,
            @PathVariable String refundPublicId,
            @Valid @RequestBody RefundDecisionRequest request) {
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã từ chối yêu cầu hoàn tiền",
                refundService.reject(refundPublicId, cinema,
                        currentUserProvider.getCurrentUserId(), request.getNote())));
    }
}
