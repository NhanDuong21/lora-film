package com.project.paymentservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.dto.request.CashCancelRequest;
import com.project.paymentservice.dto.request.CashCollectRequest;
import com.project.paymentservice.dto.request.CreateCashPaymentRequest;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.dto.request.MockCallbackRequest;
import com.project.paymentservice.dto.response.CancelPaymentResponse;
import com.project.paymentservice.dto.response.CashCancelResponse;
import com.project.paymentservice.dto.response.CashCollectResponse;
import com.project.paymentservice.dto.response.CreatePaymentResponse;
import com.project.paymentservice.dto.response.EmployeeBookingPaymentResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.dto.response.PaymentStatusResponse;
import com.project.paymentservice.entity.CashPaymentDetail;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentIdempotencyRecord;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.mapper.PaymentMapper;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.provider.ProviderSessionUncertainException;
import com.project.paymentservice.repository.CashPaymentDetailRepository;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.service.CanonicalHashUtil;
import com.project.paymentservice.service.PaymentIdempotencyService;
import com.project.paymentservice.service.PaymentOutboxService;
import com.project.paymentservice.service.PaymentService;
import com.project.paymentservice.service.PaymentTransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final String CREATE_PAYMENT = "CREATE_PAYMENT";
    private static final String CREATE_CASH = "CREATE_CASH_PAYMENT";
    private static final String CANCEL_PAYMENT = "CANCEL_PAYMENT";
    private static final String COLLECT_CASH = "COLLECT_CASH_PAYMENT";
    private static final String CANCEL_CASH = "CANCEL_CASH_PAYMENT";

    private final PaymentRepository paymentRepository;
    private final CashPaymentDetailRepository cashRepository;
    private final BookingPaymentClient bookingClient;
    private final PaymentProviderRegistry providerRegistry;
    private final PaymentTransactionService transactionService;
    private final PaymentIdempotencyService idempotencyService;
    private final PaymentOutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final PaymentRuntimeProperties runtimeProperties;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            CashPaymentDetailRepository cashRepository,
            BookingPaymentClient bookingClient,
            PaymentProviderRegistry providerRegistry,
            PaymentTransactionService transactionService,
            PaymentIdempotencyService idempotencyService,
            PaymentOutboxService outboxService,
            ObjectMapper objectMapper,
            PaymentRuntimeProperties runtimeProperties) {
        this.paymentRepository = paymentRepository;
        this.cashRepository = cashRepository;
        this.bookingClient = bookingClient;
        this.providerRegistry = providerRegistry;
        this.transactionService = transactionService;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public CreatePaymentResponse createPayment(
            Long accountId, String idempotencyKey, CreatePaymentRequest request) {
        return createPayment(accountId, idempotencyKey, request, "127.0.0.1");
    }

    @Override
    public CreatePaymentResponse createPayment(
            Long accountId,
            String idempotencyKey,
            CreatePaymentRequest request,
            String clientIp) {
        validateCreateIdentity(request.getBookingPublicId(), request.getBookingId());
        ProviderCode provider = parseOnlineProvider(request.getPaymentMethod());
        String reference = request.getBookingPublicId() != null
                ? request.getBookingPublicId() : String.valueOf(request.getBookingId());
        String hash = CanonicalHashUtil.hashCreatePayment(accountId, reference, provider.name());
        String ownerToken = UUID.randomUUID().toString();
        PaymentIdempotencyService.Reservation reservation =
                idempotencyService.reserve(accountId, CREATE_PAYMENT, idempotencyKey, hash, ownerToken);
        if (reservation.replay()) {
            return replay(reservation.record(), CreatePaymentResponse.class);
        }

        PaymentProvider adapter;
        BookingPaymentContext context;
        Payment payment;
        try {
            adapter = providerRegistry.getProvider(provider);
            context = getContext(request.getBookingPublicId(), request.getBookingId());
            validatePayableContext(context);
            if (!context.getAccountId().equals(accountId)) {
                throw new BusinessException("PAYMENT_ACCESS_DENIED",
                        "Bạn không sở hữu đơn đặt vé này", HttpStatus.FORBIDDEN);
            }
            payment = recoverOrReserve(
                    reservation, context, provider, accountId, ownerToken);
        } catch (BusinessException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    exception.getErrorCode(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    "INTERNAL_SERVER_ERROR", "Không thể khởi tạo giao dịch thanh toán");
            throw new BusinessException("INTERNAL_SERVER_ERROR",
                    "Không thể khởi tạo giao dịch thanh toán",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            PaymentSessionRequest sessionRequest = toSessionRequest(payment, clientIp);
            PaymentSession session = adapter.createSession(sessionRequest);
            payment = transactionService.finalizeProviderSession(
                    payment.getId(),
                    session,
                    normalSessionRecoveryAt(payment, session));
            CreatePaymentResponse response = PaymentMapper.toCreateResponse(payment, session.getPaymentUrl());
            idempotencyService.complete(
                    reservation.record().getId(), ownerToken, payment.getId(), 201, json(response));
            return response;
        } catch (ProviderSessionUncertainException exception) {
            payment = transactionService.markSessionUncertain(
                    payment.getId(),
                    exception,
                    Instant.now().plusSeconds(runtimeProperties.getSettlementHoldSeconds()));
            CreatePaymentResponse response = PaymentMapper.toCreateResponse(payment, null);
            idempotencyService.complete(
                    reservation.record().getId(), ownerToken, payment.getId(), 202, json(response));
            return response;
        } catch (BusinessException exception) {
            transactionService.markSessionFailure(
                    payment.getId(), exception.getErrorCode(), exception.getMessage());
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    exception.getErrorCode(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            transactionService.markSessionFailure(
                    payment.getId(), "PAYMENT_PROVIDER_UNAVAILABLE", exception.getMessage());
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    "PAYMENT_PROVIDER_UNAVAILABLE", "Không thể khởi tạo phiên thanh toán");
            throw new BusinessException("PAYMENT_PROVIDER_UNAVAILABLE",
                    "Không thể khởi tạo phiên thanh toán", HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * A normal provider session must not be queried while the customer is
     * still completing checkout. VNPay rate-limits repeated QueryDR calls and
     * reports them as duplicate requests, which can hide the terminal result.
     * Browser Return/IPN triggers an earlier authoritative check; this value is
     * only the lost-browser/lost-IPN safety net.
     */
    static Instant normalSessionRecoveryAt(Payment payment, PaymentSession session) {
        Instant sessionExpiry = session.getExpiresAt();
        Instant bookingExpiry = payment.getBookingExpiresAt();
        if (sessionExpiry == null) {
            return bookingExpiry;
        }
        if (bookingExpiry == null) {
            return sessionExpiry;
        }
        return sessionExpiry.isBefore(bookingExpiry) ? sessionExpiry : bookingExpiry;
    }

    @Override
    public PaymentDetailResponse getPayment(Long accountId, String paymentPublicId) {
        Payment payment = paymentRepository.findByPublicId(paymentPublicId)
                .orElseThrow(() -> notFound(paymentPublicId));
        requirePaymentOwner(payment, accountId);
        return detail(payment);
    }

    @Override
    public PaymentDetailResponse getPayment(Long accountId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> notFound(String.valueOf(paymentId)));
        requirePaymentOwner(payment, accountId);
        return detail(payment);
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(Long accountId, String paymentPublicId) {
        Payment payment = paymentRepository.findByPublicId(paymentPublicId)
                .orElseThrow(() -> notFound(paymentPublicId));
        requirePaymentOwner(payment, accountId);
        return status(payment);
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(Long accountId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> notFound(String.valueOf(paymentId)));
        requirePaymentOwner(payment, accountId);
        return status(payment);
    }

    @Override
    public Page<PaymentDetailResponse> getPaymentsByBooking(
            Long accountId, String bookingPublicId, Pageable pageable) {
        return paymentRepository.findByBookingPublicIdAndAccountId(
                bookingPublicId, accountId, pageable).map(this::detail);
    }

    @Override
    public Page<PaymentDetailResponse> getPaymentsByBooking(
            Long accountId, Long bookingId, Pageable pageable) {
        return paymentRepository.findByBookingIdAndAccountId(
                bookingId, accountId, pageable).map(this::detail);
    }

    @Override
    public CancelPaymentResponse cancelPayment(
            Long accountId, String idempotencyKey, String paymentPublicId) {
        String ownerToken = UUID.randomUUID().toString();
        String hash = CanonicalHashUtil.hashOperation(
                CANCEL_PAYMENT, accountId, paymentPublicId);
        PaymentIdempotencyService.Reservation reservation =
                idempotencyService.reserve(accountId, CANCEL_PAYMENT, idempotencyKey, hash, ownerToken);
        if (reservation.replay()) {
            return replay(reservation.record(), CancelPaymentResponse.class);
        }
        try {
            Payment payment = transactionService.cancelBeforeProviderSession(
                    paymentPublicId, accountId);
            CancelPaymentResponse response = new CancelPaymentResponse(
                    payment.getId(), payment.getStatus().name(), payment.getCancelledAt());
            response.setPaymentPublicId(payment.getPublicId());
            idempotencyService.complete(
                    reservation.record().getId(), ownerToken, payment.getId(), 200, json(response));
            return response;
        } catch (BusinessException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    exception.getErrorCode(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    "INTERNAL_SERVER_ERROR", "Không thể hủy giao dịch thanh toán");
            throw new BusinessException("INTERNAL_SERVER_ERROR",
                    "Không thể hủy giao dịch thanh toán",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CancelPaymentResponse cancelPayment(
            Long accountId, String idempotencyKey, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> notFound(String.valueOf(paymentId)));
        return cancelPayment(accountId, idempotencyKey, payment.getPublicId());
    }

    @Override
    public EmployeeBookingPaymentResponse lookupBookingForCash(String reference) {
        BookingPaymentContext context = looksLikeUuid(reference)
                ? bookingClient.getPaymentContext(reference)
                : bookingClient.getPaymentContextByCode(reference);
        validatePayableContext(context);
        return EmployeeBookingPaymentResponse.from(context);
    }

    @Override
    public PaymentDetailResponse getPaymentForEmployee(String paymentPublicId) {
        return detail(paymentRepository.findByPublicId(paymentPublicId)
                .orElseThrow(() -> notFound(paymentPublicId)));
    }

    @Override
    public PaymentDetailResponse getPaymentForEmployee(Long paymentId) {
        return detail(paymentRepository.findById(paymentId)
                .orElseThrow(() -> notFound(String.valueOf(paymentId))));
    }

    @Override
    public CreatePaymentResponse createCashPayment(
            Long employeeId, String idempotencyKey, CreateCashPaymentRequest request) {
        validateCreateIdentity(request.getBookingPublicId(), null, request.getBookingCode());
        BookingPaymentContext context = request.getBookingPublicId() != null
                ? bookingClient.getPaymentContext(request.getBookingPublicId())
                : bookingClient.getPaymentContextByCode(request.getBookingCode());
        validatePayableContext(context);
        String reference = request.getBookingPublicId() != null
                ? request.getBookingPublicId() : request.getBookingCode();
        String hash = CanonicalHashUtil.hashOperation(CREATE_CASH, employeeId, reference);
        String ownerToken = UUID.randomUUID().toString();
        PaymentIdempotencyService.Reservation reservation =
                idempotencyService.reserve(employeeId, CREATE_CASH, idempotencyKey, hash, ownerToken);
        if (reservation.replay()) {
            return replay(reservation.record(), CreatePaymentResponse.class);
        }
        try {
            Payment payment = recoverOrReserve(
                    reservation, context, ProviderCode.CASH, employeeId, ownerToken);
            CreatePaymentResponse response = PaymentMapper.toCreateResponse(payment, null);
            idempotencyService.complete(
                    reservation.record().getId(), ownerToken, payment.getId(), 201, json(response));
            return response;
        } catch (BusinessException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    exception.getErrorCode(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    "INTERNAL_SERVER_ERROR", "Không thể tạo giao dịch tiền mặt");
            throw new BusinessException("INTERNAL_SERVER_ERROR",
                    "Không thể tạo giao dịch tiền mặt", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CashCollectResponse collectCashPayment(
            Long employeeId,
            String idempotencyKey,
            String paymentPublicId,
            CashCollectRequest request) {
        String payload = paymentPublicId + "|" + request.getReceivedAmount()
                + "|" + (request.getNote() == null ? "" : request.getNote().trim());
        String hash = CanonicalHashUtil.hashOperation(COLLECT_CASH, employeeId, payload);
        String ownerToken = UUID.randomUUID().toString();
        PaymentIdempotencyService.Reservation reservation =
                idempotencyService.reserve(employeeId, COLLECT_CASH, idempotencyKey, hash, ownerToken);
        if (reservation.replay()) {
            return replay(reservation.record(), CashCollectResponse.class);
        }
        try {
            Payment existing = paymentRepository.findByPublicId(paymentPublicId)
                    .orElseThrow(() -> notFound(paymentPublicId));
            BookingPaymentContext fresh = bookingClient.getPaymentContext(existing.getBookingPublicId());
            Payment payment = transactionService.collectCash(
                    paymentPublicId, employeeId, request.getReceivedAmount(), request.getNote(), fresh);
            CashPaymentDetail detail = cashRepository.findById(payment.getId())
                    .orElseThrow(() -> new IllegalStateException("Cash detail missing"));
            CashCollectResponse response = new CashCollectResponse(
                    payment.getId(),
                    payment.getBookingId(),
                    payment.getPaymentMethod().name(),
                    payment.getStatus().name(),
                    payment.getAmount(),
                    detail.getReceivedAmount(),
                    detail.getChangeAmount(),
                    detail.getCollectedByAccountId(),
                    detail.getCollectedAt(),
                    outboxService.deliveryStatus(payment.getPublicId()));
            response.setPaymentPublicId(payment.getPublicId());
            response.setBookingPublicId(payment.getBookingPublicId());
            idempotencyService.complete(
                    reservation.record().getId(), ownerToken, payment.getId(), 200, json(response));
            return response;
        } catch (BusinessException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    exception.getErrorCode(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    "INTERNAL_SERVER_ERROR", "Không thể ghi nhận thu tiền mặt");
            throw new BusinessException("INTERNAL_SERVER_ERROR",
                    "Không thể ghi nhận thu tiền mặt", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CashCollectResponse collectCashPayment(
            Long employeeId, String idempotencyKey, Long paymentId, CashCollectRequest request) {
        return collectCashPayment(employeeId, idempotencyKey,
                paymentRepository.findById(paymentId)
                        .orElseThrow(() -> notFound(String.valueOf(paymentId))).getPublicId(),
                request);
    }

    @Override
    public CashCancelResponse cancelCashPayment(
            Long employeeId,
            String idempotencyKey,
            String paymentPublicId,
            CashCancelRequest request) {
        String reason = request == null ? null : request.getReason();
        String hash = CanonicalHashUtil.hashOperation(
                CANCEL_CASH, employeeId, paymentPublicId + "|" + (reason == null ? "" : reason));
        String ownerToken = UUID.randomUUID().toString();
        PaymentIdempotencyService.Reservation reservation =
                idempotencyService.reserve(employeeId, CANCEL_CASH, idempotencyKey, hash, ownerToken);
        if (reservation.replay()) {
            return replay(reservation.record(), CashCancelResponse.class);
        }
        try {
            Payment payment = transactionService.cancelCash(paymentPublicId, employeeId, reason);
            CashCancelResponse response = new CashCancelResponse(
                    payment.getId(), payment.getStatus().name(), employeeId,
                    payment.getCancelledAt(), outboxService.deliveryStatus(payment.getPublicId()));
            response.setPaymentPublicId(payment.getPublicId());
            idempotencyService.complete(
                    reservation.record().getId(), ownerToken, payment.getId(), 200, json(response));
            return response;
        } catch (BusinessException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    exception.getErrorCode(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            idempotencyService.fail(reservation.record().getId(), ownerToken,
                    "INTERNAL_SERVER_ERROR", "Không thể hủy giao dịch tiền mặt");
            throw new BusinessException("INTERNAL_SERVER_ERROR",
                    "Không thể hủy giao dịch tiền mặt", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CashCancelResponse cancelCashPayment(
            Long employeeId, String idempotencyKey, Long paymentId, CashCancelRequest request) {
        return cancelCashPayment(employeeId, idempotencyKey,
                paymentRepository.findById(paymentId)
                        .orElseThrow(() -> notFound(String.valueOf(paymentId))).getPublicId(),
                request);
    }

    @Override
    public void processMockCallback(
            Long accountId, String paymentPublicId, String simulatedStatus) {
        Payment payment = paymentRepository.findByPublicIdAndAccountId(paymentPublicId, accountId)
                .orElseThrow(() -> notFound(paymentPublicId));
        if (payment.getProviderCode() != ProviderCode.MOCK) {
            throw new BusinessException("PAYMENT_NOT_MOCK",
                    "Giao dịch không thuộc MOCK provider", HttpStatus.CONFLICT);
        }
        ProviderCallbackResult result = mockResult(payment, simulatedStatus);
        transactionService.applyProviderResult(ProviderCode.MOCK, result, null);
    }

    @Override
    public void processMockCallback(MockCallbackRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> notFound(String.valueOf(request.getPaymentId())));
        transactionService.applyProviderResult(
                ProviderCode.MOCK, mockResult(payment, request.getSimulatedStatus()), null);
    }

    private Payment recoverOrReserve(
            PaymentIdempotencyService.Reservation reservation,
            BookingPaymentContext context,
            ProviderCode provider,
            Long actorId,
            String ownerToken) {
        if (reservation.record().getPaymentId() != null) {
            return paymentRepository.findById(reservation.record().getPaymentId())
                    .orElseThrow(() -> new BusinessException(
                            "PAYMENT_RECOVERY_FAILED",
                            "Không thể khôi phục giao dịch đang xử lý",
                            HttpStatus.CONFLICT));
        }
        return transactionService.reserveAttempt(
                context, provider, actorId, reservation.record().getId(), ownerToken);
    }

    private PaymentSessionRequest toSessionRequest(Payment payment, String clientIp) {
        PaymentSessionRequest request = new PaymentSessionRequest();
        request.setPaymentId(payment.getId());
        request.setPaymentPublicId(payment.getPublicId());
        request.setPaymentTransactionCode(payment.getPaymentTransactionCode());
        request.setBookingId(payment.getBookingId());
        request.setBookingPublicId(payment.getBookingPublicId());
        request.setAmount(payment.getAmount());
        request.setCurrency(payment.getCurrency());
        request.setClientIp(clientIp);
        request.setOrderDescription("Thanh toan ve LoraFilm " + payment.getPaymentTransactionCode());
        request.setExpiresAt(payment.getBookingExpiresAt());
        return request;
    }

    private BookingPaymentContext getContext(String publicId, Long numericId) {
        return publicId != null
                ? bookingClient.getPaymentContext(publicId)
                : bookingClient.getPaymentContext(numericId);
    }

    private ProviderCode parseOnlineProvider(String value) {
        try {
            ProviderCode provider = ProviderCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (provider == ProviderCode.CASH) {
                throw new IllegalArgumentException();
            }
            return provider;
        } catch (Exception exception) {
            throw new BusinessException("PAYMENT_PROVIDER_INVALID",
                    "Phương thức thanh toán không hợp lệ", HttpStatus.BAD_REQUEST);
        }
    }

    private void validatePayableContext(BookingPaymentContext context) {
        Instant now = Instant.now();
        if (context == null
                || !Boolean.TRUE.equals(context.getPayable())
                || context.getBookingPublicId() == null
                || context.getAccountId() == null
                || context.getAmountLockedAt() == null
                || context.getExpiresAt() == null
                || !context.getExpiresAt().isAfter(now)
                || context.getAmount() == null
                || context.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || context.getCurrency() == null
                || context.getCurrency().length() != 3) {
            throw new BusinessException(
                    "BOOKING_NOT_PAYABLE",
                    "Đơn chưa được chốt số tiền, đã hết hạn hoặc không còn khả dụng để thanh toán",
                    HttpStatus.CONFLICT);
        }
    }

    private ProviderCallbackResult mockResult(Payment payment, String simulatedStatus) {
        String normalized = simulatedStatus == null
                ? "" : simulatedStatus.trim().toUpperCase(Locale.ROOT);
        if (!"SUCCESS".equals(normalized) && !"FAILED".equals(normalized)
                && !"CANCELLED".equals(normalized)) {
            throw new BusinessException("MOCK_RESULT_INVALID",
                    "Kết quả MOCK không hợp lệ", HttpStatus.BAD_REQUEST);
        }
        ProviderCallbackResult result = new ProviderCallbackResult();
        result.setSignatureValid(true);
        result.setDeduplicationKey("MOCK:" + payment.getPublicId() + ":" + normalized);
        result.setProviderOrderId(payment.getProviderOrderId());
        result.setExternalTransactionId("MOCK-TX-" + payment.getPublicId());
        result.setResult(normalized);
        result.setResponseCode("SUCCESS".equals(normalized) ? "00" : "99");
        result.setAmount(payment.getAmount());
        result.setCurrency(payment.getCurrency());
        result.setOccurredAt(Instant.now());
        result.setSanitizedPayload("{\"provider\":\"MOCK\"}");
        return result;
    }

    private PaymentDetailResponse detail(Payment payment) {
        PaymentDetailResponse response = PaymentMapper.toDetailResponse(payment);
        response.setBookingDeliveryStatus(outboxService.deliveryStatus(payment.getPublicId()));
        return response;
    }

    private PaymentStatusResponse status(Payment payment) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setPaymentId(payment.getId());
        response.setPaymentPublicId(payment.getPublicId());
        response.setBookingPublicId(payment.getBookingPublicId());
        response.setStatus(payment.getStatus().name());
        response.setReconciliationStatus(payment.getReconciliationStatus().name());
        response.setBookingDeliveryStatus(outboxService.deliveryStatus(payment.getPublicId()));
        return response;
    }

    private void validateCreateIdentity(String publicId, Long numericId) {
        validateCreateIdentity(publicId, numericId, null);
    }

    private void validateCreateIdentity(String publicId, Long numericId, String bookingCode) {
        int supplied = hasText(publicId) ? 1 : 0;
        supplied += numericId != null ? 1 : 0;
        supplied += hasText(bookingCode) ? 1 : 0;
        if (supplied != 1) {
            throw new BusinessException("BOOKING_IDENTITY_INVALID",
                    "Cần cung cấp đúng một định danh Booking", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean looksLikeUuid(String value) {
        if (!hasText(value)) {
            throw new BusinessException("BOOKING_REFERENCE_REQUIRED",
                    "Vui lòng nhập mã đơn hoặc Booking UUID", HttpStatus.BAD_REQUEST);
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private <T> T replay(PaymentIdempotencyRecord record, Class<T> type) {
        try {
            return objectMapper.readValue(record.getResponseBodySanitized(), type);
        } catch (Exception exception) {
            throw new BusinessException("IDEMPOTENCY_REPLAY_UNAVAILABLE",
                    "Không thể đọc lại kết quả yêu cầu trước", HttpStatus.CONFLICT);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize idempotency response", exception);
        }
    }

    private void requirePaymentOwner(Payment payment, Long accountId) {
        if (!payment.getAccountId().equals(accountId)) {
            throw new BusinessException("PAYMENT_ACCESS_DENIED",
                    "Bạn không có quyền truy cập giao dịch này", HttpStatus.FORBIDDEN);
        }
    }

    private BusinessException notFound(String identity) {
        return new BusinessException("PAYMENT_NOT_FOUND",
                "Không tìm thấy giao dịch: " + identity, HttpStatus.NOT_FOUND);
    }
}
