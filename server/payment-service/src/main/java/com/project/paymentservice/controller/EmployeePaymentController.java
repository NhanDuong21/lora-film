package com.project.paymentservice.controller;

import com.project.paymentservice.common.ApiResponse;
import com.project.paymentservice.client.user.EmployeeCinemaScopeClient;
import com.project.paymentservice.dto.request.CashCancelRequest;
import com.project.paymentservice.dto.request.CashCollectRequest;
import com.project.paymentservice.dto.request.CompleteCashRefundRequest;
import com.project.paymentservice.dto.request.CloseCounterCashSessionRequest;
import com.project.paymentservice.dto.request.CreateCashPaymentRequest;
import com.project.paymentservice.dto.request.CreateRefundRequest;
import com.project.paymentservice.dto.request.OpenCounterCashSessionRequest;
import com.project.paymentservice.dto.response.CashCancelResponse;
import com.project.paymentservice.dto.response.CashCollectResponse;
import com.project.paymentservice.dto.response.CounterCashSessionResponse;
import com.project.paymentservice.dto.response.CreatePaymentResponse;
import com.project.paymentservice.dto.response.EmployeeBookingPaymentResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.dto.response.RefundResponse;
import com.project.paymentservice.enumtype.RefundStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.security.CurrentUserProvider;
import com.project.paymentservice.service.AdminPaymentService;
import com.project.paymentservice.service.CounterCashSessionService;
import com.project.paymentservice.service.PaymentService;
import com.project.paymentservice.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/employee/payments")
public class EmployeePaymentController {
    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;
    private final AdminPaymentService adminPaymentService;
    private final RefundService refundService;
    private final EmployeeCinemaScopeClient employeeCinemaScopeClient;
    private final CounterCashSessionService counterCashSessionService;

    public EmployeePaymentController(
            PaymentService paymentService,
            CurrentUserProvider currentUserProvider,
            AdminPaymentService adminPaymentService,
            RefundService refundService,
            EmployeeCinemaScopeClient employeeCinemaScopeClient,
            CounterCashSessionService counterCashSessionService) {
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
        this.adminPaymentService = adminPaymentService;
        this.refundService = refundService;
        this.employeeCinemaScopeClient = employeeCinemaScopeClient;
        this.counterCashSessionService = counterCashSessionService;
    }

    @GetMapping("/counter-sessions/current")
    public ResponseEntity<ApiResponse<CounterCashSessionResponse>> currentCounterSession() {
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinemaScopeClient.requireActiveCinema(employeeId);
        return ResponseEntity.ok(ApiResponse.success(
                counterCashSessionService.current(employeeId, cinemaPublicId)));
    }

    @GetMapping("/counter-sessions/history")
    public ResponseEntity<ApiResponse<List<CounterCashSessionResponse>>> counterSessionHistory() {
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinemaScopeClient.requireActiveCinema(employeeId);
        return ResponseEntity.ok(ApiResponse.success(
                counterCashSessionService.history(employeeId, cinemaPublicId)));
    }

    @PostMapping("/counter-sessions")
    public ResponseEntity<ApiResponse<CounterCashSessionResponse>> openCounterSession(
            @Valid @RequestBody OpenCounterCashSessionRequest request) {
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinemaScopeClient.requireActiveCinema(employeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Đã mở ca thu ngân",
                counterCashSessionService.open(employeeId, cinemaPublicId, request)));
    }

    @PostMapping("/counter-sessions/{sessionPublicId:[a-fA-F0-9-]{36}}/close")
    public ResponseEntity<ApiResponse<CounterCashSessionResponse>> closeCounterSession(
            @PathVariable String sessionPublicId,
            @Valid @RequestBody CloseCounterCashSessionRequest request) {
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinemaScopeClient.requireActiveCinema(employeeId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã chốt ca và ghi nhận bàn giao",
                counterCashSessionService.close(
                        employeeId, cinemaPublicId, sessionPublicId, request)));
    }

    @GetMapping("/refund-candidate")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> refundCandidate(
            @RequestParam String reference) {
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinemaScopeClient.requireActiveCinema(employeeId);
        return ResponseEntity.ok(ApiResponse.success(
                adminPaymentService.refundCandidateForCinema(cinemaPublicId, reference)));
    }

    @PostMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}/refund-requests")
    public ResponseEntity<ApiResponse<RefundResponse>> createRefundRequest(
            @PathVariable String paymentPublicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateRefundRequest request) {
        requireKey(idempotencyKey);
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinemaScopeClient.requireActiveCinema(employeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Đã gửi yêu cầu hoàn tiền cho quản lý rạp",
                refundService.createEmployeeRefundRequest(
                        paymentPublicId, idempotencyKey, employeeId, cinemaPublicId, request)));
    }

    @GetMapping("/refund-requests/cash-pending")
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> cashRefundsPendingAtCounter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinemaScopeClient.requireActiveCinema(employeeId);
        return ResponseEntity.ok(ApiResponse.success(refundService.listForCinema(
                cinemaPublicId,
                RefundStatus.REQUIRES_ACTION,
                PageRequest.of(page, Math.min(size, 100),
                        Sort.by(Sort.Direction.DESC, "requestedAt")))));
    }

    @PostMapping("/refund-requests/{refundPublicId}/cash/complete")
    public ResponseEntity<ApiResponse<RefundResponse>> completeCashRefund(
            @PathVariable String refundPublicId,
            @Valid @RequestBody CompleteCashRefundRequest request) {
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinemaScopeClient.requireActiveCinema(employeeId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xác nhận trả tiền mặt cho khách",
                refundService.completeEmployeeCashRefund(
                        refundPublicId,
                        employeeId,
                        cinemaPublicId,
                        request.getProviderReference(),
                        request.getNote())));
    }

    @GetMapping("/booking")
    public ResponseEntity<ApiResponse<EmployeeBookingPaymentResponse>> lookupBooking(
            @RequestParam String reference) {
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinema(employeeId);
        EmployeeBookingPaymentResponse booking = paymentService.lookupBookingForCash(reference);
        requireBookingCinema(cinemaPublicId, booking.getCinemaPublicId());
        return ResponseEntity.ok(ApiResponse.success(
                booking));
    }

    @PostMapping("/cash")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createCash(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateCashPaymentRequest request) {
        requireKey(idempotencyKey);
        Long employeeId = currentUserProvider.getCurrentUserId();
        String cinemaPublicId = employeeCinema(employeeId);
        EmployeeBookingPaymentResponse booking = paymentService.lookupBookingForCash(
                request.getBookingPublicId() != null
                        ? request.getBookingPublicId() : request.getBookingCode());
        requireBookingCinema(cinemaPublicId, booking.getCinemaPublicId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Đã tạo giao dịch tiền mặt",
                paymentService.createCashPayment(
                        employeeId, idempotencyKey, request)));
    }

    @GetMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPayment(
            @PathVariable String paymentPublicId) {
        PaymentDetailResponse detail = paymentService.getPaymentForEmployee(paymentPublicId);
        requirePaymentCinema(currentUserProvider.getCurrentUserId(), paymentPublicId);
        return ResponseEntity.ok(ApiResponse.success(
                detail));
    }

    @PostMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}/cash/collect")
    public ResponseEntity<ApiResponse<CashCollectResponse>> collectCash(
            @PathVariable String paymentPublicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CashCollectRequest request) {
        requireKey(idempotencyKey);
        Long employeeId = currentUserProvider.getCurrentUserId();
        requirePaymentCinema(employeeId, paymentPublicId);
        CashCollectResponse response = paymentService.collectCashPayment(
                employeeId, idempotencyKey, paymentPublicId, request);
        return ResponseEntity.status("PENDING".equals(response.getBookingDeliveryStatus())
                ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(ApiResponse.success("Đã ghi nhận thu tiền mặt", response));
    }

    @PostMapping("/{paymentPublicId:[a-fA-F0-9-]{36}}/cash/cancel")
    public ResponseEntity<ApiResponse<CashCancelResponse>> cancelCash(
            @PathVariable String paymentPublicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) CashCancelRequest request) {
        requireKey(idempotencyKey);
        Long employeeId = currentUserProvider.getCurrentUserId();
        requirePaymentCinema(employeeId, paymentPublicId);
        CashCancelRequest actual = request == null ? new CashCancelRequest() : request;
        return ResponseEntity.ok(ApiResponse.success("Đã hủy giao dịch tiền mặt",
                paymentService.cancelCashPayment(
                        employeeId,
                        idempotencyKey,
                        paymentPublicId,
                        actual)));
    }

    @PostMapping("/{paymentId:\\d+}/cash/collect")
    public ResponseEntity<ApiResponse<CashCollectResponse>> collectCompat(
            @PathVariable Long paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CashCollectRequest request) {
        requireKey(idempotencyKey);
        Long employeeId = currentUserProvider.getCurrentUserId();
        PaymentDetailResponse detail = paymentService.getPaymentForEmployee(paymentId);
        requirePaymentCinema(employeeId, detail.getPaymentPublicId());
        return ResponseEntity.ok(ApiResponse.success(paymentService.collectCashPayment(
                employeeId, idempotencyKey, paymentId, request)));
    }

    @PostMapping("/{paymentId:\\d+}/cash/cancel")
    public ResponseEntity<ApiResponse<CashCancelResponse>> cancelCompat(
            @PathVariable Long paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody(required = false) CashCancelRequest request) {
        requireKey(idempotencyKey);
        Long employeeId = currentUserProvider.getCurrentUserId();
        PaymentDetailResponse detail = paymentService.getPaymentForEmployee(paymentId);
        requirePaymentCinema(employeeId, detail.getPaymentPublicId());
        return ResponseEntity.ok(ApiResponse.success(paymentService.cancelCashPayment(
                employeeId,
                idempotencyKey,
                paymentId,
                request == null ? new CashCancelRequest() : request)));
    }

    private void requireKey(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency-Key là bắt buộc", HttpStatus.BAD_REQUEST);
        }
    }

    private String employeeCinema(Long employeeId) {
        boolean admin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return admin ? null : employeeCinemaScopeClient.requireActiveCinema(employeeId);
    }

    private void requirePaymentCinema(Long employeeId, String paymentPublicId) {
        String cinemaPublicId = employeeCinema(employeeId);
        if (cinemaPublicId != null) {
            adminPaymentService.detailForCinema(cinemaPublicId, paymentPublicId);
        }
    }

    private void requireBookingCinema(String expectedCinema, String actualCinema) {
        if (expectedCinema == null) return;
        if (actualCinema == null || !expectedCinema.equalsIgnoreCase(actualCinema)) {
            throw new BusinessException(
                    "EMPLOYEE_PAYMENT_CINEMA_SCOPE_DENIED",
                    "Đơn đặt vé không thuộc rạp mà nhân viên đang được phân công.",
                    HttpStatus.FORBIDDEN);
        }
    }
}
