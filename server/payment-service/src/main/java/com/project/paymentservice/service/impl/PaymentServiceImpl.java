package com.project.paymentservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.dto.request.MockCallbackRequest;
import com.project.paymentservice.dto.response.CancelPaymentResponse;
import com.project.paymentservice.dto.response.CreatePaymentResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.dto.response.PaymentStatusResponse;
import com.project.paymentservice.entity.BookingPaymentGuard;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import com.project.paymentservice.entity.PaymentIdempotencyRecord;
import com.project.paymentservice.enumtype.*;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.mapper.PaymentMapper;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.PaymentSessionRequest;
import com.project.paymentservice.repository.*;
import com.project.paymentservice.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String SOURCE = "PaymentService";
    private static final int MAX_TX_CODE_RETRIES = 5;

    private final PaymentRepository paymentRepository;
    private final BookingPaymentGuardRepository guardRepository;
    private final PaymentIdempotencyRecordRepository idempotencyRepository;
    private final PaymentAnalyticsSnapshotRepository snapshotRepository;
    private final BookingPaymentClient bookingClient;
    private final PaymentProviderRegistry providerRegistry;
    private final TransactionCodeGenerator transactionCodeGenerator;
    private final PaymentLogService paymentLogService;
    private final PaymentStateTransitionService stateTransitionService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingPaymentGuardRepository guardRepository,
            PaymentIdempotencyRecordRepository idempotencyRepository,
            PaymentAnalyticsSnapshotRepository snapshotRepository,
            BookingPaymentClient bookingClient,
            PaymentProviderRegistry providerRegistry,
            TransactionCodeGenerator transactionCodeGenerator,
            PaymentLogService paymentLogService,
            PaymentStateTransitionService stateTransitionService,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.guardRepository = guardRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.snapshotRepository = snapshotRepository;
        this.bookingClient = bookingClient;
        this.providerRegistry = providerRegistry;
        this.transactionCodeGenerator = transactionCodeGenerator;
        this.paymentLogService = paymentLogService;
        this.stateTransitionService = stateTransitionService;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CreatePaymentResponse createPayment(Long accountId, String idempotencyKey, CreatePaymentRequest request) {
        // Validate payment method
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Unsupported payment method: " + request.getPaymentMethod(), HttpStatus.BAD_REQUEST);
        }
        if (method == PaymentMethod.CASH) {
            throw new BusinessException("VALIDATION_ERROR",
                    "CASH payment cannot be created through customer API", HttpStatus.BAD_REQUEST);
        }
        if (method != PaymentMethod.MOCK) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Only MOCK payment method is supported in this version", HttpStatus.BAD_REQUEST);
        }

        // Verify provider is available
        PaymentProvider provider = providerRegistry.getProvider(method);

        String requestHash = CanonicalHashUtil.hashCreatePayment(accountId, request.getBookingId(), method.name());

        // Phase A — Reserve idempotency (short transaction)
        IdempotencyResult idempResult = transactionTemplate.execute(status -> {
            return reserveIdempotency(accountId, "CREATE_PAYMENT", idempotencyKey, requestHash);
        });

        if (idempResult != null && idempResult.isReplay()) {
            return deserializeReplay(idempResult.record, CreatePaymentResponse.class);
        }

        // Phase B — Fetch Booking Context (no DB transaction)
        BookingPaymentContext context;
        try {
            context = bookingClient.getPaymentContext(request.getBookingId());
        } catch (BusinessException e) {
            // Mark idempotency as FAILED
            markIdempotencyFailed(accountId, "CREATE_PAYMENT", idempotencyKey, e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            markIdempotencyFailed(accountId, "CREATE_PAYMENT", idempotencyKey, "BOOKING_SERVICE_UNAVAILABLE", e.getMessage());
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                    "Failed to fetch booking context", HttpStatus.SERVICE_UNAVAILABLE);
        }

        // Phase C — Validate ownership
        if (!context.getAccountId().equals(accountId)) {
            markIdempotencyFailed(accountId, "CREATE_PAYMENT", idempotencyKey, "FORBIDDEN", "Ownership mismatch");
            throw new BusinessException("FORBIDDEN", "You do not own this booking", HttpStatus.FORBIDDEN);
        }

        // Phase D — Reserve Payment attempt (short transaction)
        Payment payment = transactionTemplate.execute(status -> {
            return reservePaymentAttempt(accountId, request.getBookingId(), method, context);
        });

        // Phase E — Create provider session (no DB transaction)
        PaymentSession session;
        try {
            PaymentSessionRequest sessionReq = new PaymentSessionRequest();
            sessionReq.setPaymentId(payment.getId());
            sessionReq.setPaymentTransactionCode(payment.getPaymentTransactionCode());
            sessionReq.setBookingId(request.getBookingId());
            sessionReq.setAmount(payment.getAmount());
            sessionReq.setCurrency(payment.getCurrency());
            session = provider.createSession(sessionReq);
        } catch (Exception e) {
            logger.error("Provider session creation failed for paymentId={}", payment.getId(), e);
            // Mark Payment FAILED, clear Guard, complete idempotency as FAILED
            handleSessionCreationFailure(payment, accountId, idempotencyKey);
            throw new BusinessException("PAYMENT_SESSION_CREATION_FAILED",
                    "Failed to create payment session", HttpStatus.BAD_GATEWAY);
        }

        // Phase F — Finalize session/idempotency (short transaction)
        CreatePaymentResponse response = transactionTemplate.execute(status -> {
            Payment p = paymentRepository.findById(payment.getId()).orElseThrow();

            p.setProviderOrderId(session.getProviderOrderId());
            p.setProviderSessionId(session.getProviderSessionId());

            // For MOCK we keep PENDING (per contract: NULL -> PENDING, callback transitions to SUCCESS/FAILED)
            // The contract shows MOCK callback does PROCESSING -> SUCCESS/FAILED
            // But CREATE does PENDING (Phase C) -> potentially PROCESSING (Phase E for online)
            // For MOCK: stay PENDING until callback
            p.setSettlementHoldUntil(LocalDateTime.now().plusMinutes(15));
            paymentRepository.save(p);

            paymentLogService.log(p.getId(), PaymentLogEventType.PROVIDER_SESSION_CREATED, SOURCE,
                    ActorType.SYSTEM, null, PaymentStatus.PENDING, PaymentStatus.PENDING,
                    "MOCK session created", "providerOrderId=" + session.getProviderOrderId());

            CreatePaymentResponse resp = PaymentMapper.toCreateResponse(p, session.getPaymentUrl());

            // Complete idempotency
            completeIdempotency(accountId, "CREATE_PAYMENT", idempotencyKey, 201, resp, p.getId());

            return resp;
        });

        return response;
    }

    @Override
    public PaymentDetailResponse getPayment(Long accountId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                        "Payment not found", HttpStatus.NOT_FOUND));

        if (!payment.getAccountId().equals(accountId)) {
            throw new BusinessException("FORBIDDEN", "You do not own this payment", HttpStatus.FORBIDDEN);
        }

        return PaymentMapper.toDetailResponse(payment);
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(Long accountId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                        "Payment not found", HttpStatus.NOT_FOUND));

        if (!payment.getAccountId().equals(accountId)) {
            throw new BusinessException("FORBIDDEN", "You do not own this payment", HttpStatus.FORBIDDEN);
        }

        return new PaymentStatusResponse(payment.getId(),
                payment.getStatus().name(), payment.getReconciliationStatus().name());
    }

    @Override
    public Page<PaymentDetailResponse> getPaymentsByBooking(Long accountId, Long bookingId, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByBookingIdAndAccountId(bookingId, accountId, pageable);
        return payments.map(PaymentMapper::toDetailResponse);
    }

    @Override
    public CancelPaymentResponse cancelPayment(Long accountId, String idempotencyKey, Long paymentId) {
        String requestHash = CanonicalHashUtil.hashCancelPayment(accountId, paymentId);

        // Phase A — Reserve idempotency
        IdempotencyResult idempResult = transactionTemplate.execute(status -> {
            return reserveIdempotency(accountId, "CANCEL_PAYMENT", idempotencyKey, requestHash);
        });

        if (idempResult != null && idempResult.isReplay()) {
            return deserializeReplay(idempResult.record, CancelPaymentResponse.class);
        }

        // Execute cancel in short transaction
        CancelPaymentResponse response = transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                            "Payment not found", HttpStatus.NOT_FOUND));

            if (!payment.getAccountId().equals(accountId)) {
                throw new BusinessException("FORBIDDEN", "You do not own this payment", HttpStatus.FORBIDDEN);
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new BusinessException("PAYMENT_CANNOT_BE_CANCELLED",
                        "Payment can only be cancelled when PENDING", HttpStatus.CONFLICT);
            }

            PaymentStatus previousStatus = payment.getStatus();
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setCancelledAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Clear guard active pointer
            BookingPaymentGuard guard = guardRepository.findByBookingIdForUpdate(payment.getBookingId())
                    .orElse(null);
            if (guard != null && payment.getId().equals(guard.getActivePaymentId())) {
                guard.setActivePaymentId(null);
                guardRepository.save(guard);
            }

            paymentLogService.log(payment.getId(), PaymentLogEventType.PAYMENT_CANCELLED, SOURCE,
                    ActorType.CUSTOMER, accountId, previousStatus, PaymentStatus.CANCELLED,
                    "Payment cancelled by customer", null);

            CancelPaymentResponse resp = new CancelPaymentResponse(
                    payment.getId(), PaymentStatus.CANCELLED.name(), payment.getCancelledAt());

            completeIdempotency(accountId, "CANCEL_PAYMENT", idempotencyKey, 200, resp, payment.getId());

            return resp;
        });

        return response;
    }

    @Override
    public void processMockCallback(MockCallbackRequest request) {
        PaymentStatus targetStatus;
        try {
            targetStatus = PaymentStatus.valueOf(request.getSimulatedStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Invalid simulatedStatus: " + request.getSimulatedStatus(), HttpStatus.BAD_REQUEST);
        }

        if (targetStatus != PaymentStatus.SUCCESS && targetStatus != PaymentStatus.FAILED) {
            throw new BusinessException("VALIDATION_ERROR",
                    "simulatedStatus must be SUCCESS or FAILED", HttpStatus.BAD_REQUEST);
        }

        transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findById(request.getPaymentId())
                    .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                            "Payment not found: " + request.getPaymentId(), HttpStatus.NOT_FOUND));

            PaymentStatus currentStatus = payment.getStatus();

            // Handle idempotent duplicate callback
            if (currentStatus == targetStatus) {
                logger.info("Duplicate MOCK callback for paymentId={}, already in status={}",
                        payment.getId(), currentStatus);
                return null;
            }

            // Check for late success
            boolean isLateSuccess = stateTransitionService.isLateSuccess(currentStatus)
                    && targetStatus == PaymentStatus.SUCCESS;

            if (!stateTransitionService.isTransitionAllowed(currentStatus, targetStatus)) {
                logger.warn("Invalid state transition for paymentId={}: {} -> {}",
                        payment.getId(), currentStatus, targetStatus);
                throw new BusinessException("VALIDATION_ERROR",
                        "Invalid state transition: " + currentStatus + " -> " + targetStatus,
                        HttpStatus.BAD_REQUEST);
            }

            PaymentStatus previousStatus = payment.getStatus();
            payment.setStatus(targetStatus);

            if (targetStatus == PaymentStatus.SUCCESS) {
                payment.setSucceededAt(LocalDateTime.now());
                if (isLateSuccess) {
                    payment.setReconciliationStatus(ReconciliationStatus.REQUIRED);
                    payment.setReconciliationReason("Late success after " + previousStatus
                            + " via MOCK callback");

                    paymentLogService.log(payment.getId(), PaymentLogEventType.LATE_SUCCESS_DETECTED,
                            SOURCE, ActorType.PROVIDER, null, previousStatus, targetStatus,
                            "Late success detected", null);
                    paymentLogService.log(payment.getId(), PaymentLogEventType.RECONCILIATION_REQUIRED,
                            SOURCE, ActorType.SYSTEM, null, previousStatus, targetStatus,
                            "Reconciliation required due to late success", null);
                }
            } else if (targetStatus == PaymentStatus.FAILED) {
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureCode("MOCK_SIMULATED_FAILURE");
                payment.setFailureMessageSanitized("Simulated failure via MOCK callback");
            }

            paymentRepository.save(payment);

            // Update guard
            BookingPaymentGuard guard = guardRepository.findByBookingIdForUpdate(payment.getBookingId())
                    .orElse(null);
            if (guard != null) {
                if (targetStatus == PaymentStatus.SUCCESS && !isLateSuccess) {
                    guard.setActivePaymentId(null);
                    if (guard.getSuccessfulPaymentId() == null) {
                        guard.setSuccessfulPaymentId(payment.getId());
                    }
                } else if (targetStatus == PaymentStatus.FAILED) {
                    if (payment.getId().equals(guard.getActivePaymentId())) {
                        guard.setActivePaymentId(null);
                    }
                } else if (isLateSuccess) {
                    // Late success: don't overwrite existing successful payment
                    if (guard.getSuccessfulPaymentId() == null) {
                        guard.setSuccessfulPaymentId(payment.getId());
                    }
                }
                guardRepository.save(guard);
            }

            PaymentLogEventType eventType = targetStatus == PaymentStatus.SUCCESS
                    ? PaymentLogEventType.PAYMENT_SUCCEEDED : PaymentLogEventType.PAYMENT_FAILED;
            paymentLogService.log(payment.getId(), eventType, "MockCallback",
                    ActorType.PROVIDER, null, previousStatus, targetStatus,
                    "MOCK callback processed", null);

            return null;
        });
    }

    // ========== Private helpers ==========

    private IdempotencyResult reserveIdempotency(Long accountId, String operation,
                                                  String idempotencyKey, String requestHash) {
        var existing = idempotencyRepository
                .findAndLockByAccountIdAndOperationAndIdempotencyKey(accountId, operation, idempotencyKey);

        if (existing.isPresent()) {
            PaymentIdempotencyRecord record = existing.get();

            if (record.getProcessingStatus() == IdempotencyProcessingStatus.COMPLETED) {
                if (record.getRequestHash().equals(requestHash)) {
                    return new IdempotencyResult(record, true);
                } else {
                    throw new BusinessException("IDEMPOTENCY_KEY_REUSED",
                            "Idempotency key reused with different request", HttpStatus.CONFLICT);
                }
            }

            if (record.getProcessingStatus() == IdempotencyProcessingStatus.FAILED) {
                if (record.getRequestHash().equals(requestHash)) {
                    return new IdempotencyResult(record, true);
                } else {
                    throw new BusinessException("IDEMPOTENCY_KEY_REUSED",
                            "Idempotency key reused with different request", HttpStatus.CONFLICT);
                }
            }

            if (record.getProcessingStatus() == IdempotencyProcessingStatus.PROCESSING) {
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new BusinessException("IDEMPOTENCY_KEY_REUSED",
                            "Idempotency key reused with different request", HttpStatus.CONFLICT);
                }
                throw new BusinessException("IDEMPOTENCY_REQUEST_IN_PROGRESS",
                        "Request with this idempotency key is already in progress", HttpStatus.CONFLICT);
            }
        }

        // Insert new PROCESSING record
        PaymentIdempotencyRecord newRecord = new PaymentIdempotencyRecord();
        newRecord.setAccountId(accountId);
        newRecord.setOperation(operation);
        newRecord.setIdempotencyKey(idempotencyKey);
        newRecord.setRequestHash(requestHash);
        newRecord.setProcessingStatus(IdempotencyProcessingStatus.PROCESSING);
        newRecord.setLockedAt(LocalDateTime.now());
        newRecord.setExpiresAt(LocalDateTime.now().plusHours(24));

        try {
            idempotencyRepository.saveAndFlush(newRecord);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Race condition: another thread just inserted
            throw new BusinessException("IDEMPOTENCY_REQUEST_IN_PROGRESS",
                    "Request with this idempotency key is already in progress", HttpStatus.CONFLICT);
        }

        return new IdempotencyResult(newRecord, false);
    }

    private Payment reservePaymentAttempt(Long accountId, Long bookingId,
                                          PaymentMethod method, BookingPaymentContext context) {
        // Insert guard if absent
        guardRepository.insertIfAbsent(bookingId);

        // Lock guard
        BookingPaymentGuard guard = guardRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                        "Failed to acquire payment guard", HttpStatus.SERVICE_UNAVAILABLE));

        // Check successful payment
        if (guard.getSuccessfulPaymentId() != null) {
            throw new BusinessException("PAYMENT_ACTIVE_ATTEMPT_EXISTS",
                    "A successful payment already exists for this booking", HttpStatus.CONFLICT);
        }

        // Check active payment
        if (guard.getActivePaymentId() != null) {
            Payment activePayment = paymentRepository.findById(guard.getActivePaymentId()).orElse(null);
            if (activePayment != null && stateTransitionService.isActive(activePayment.getStatus())) {
                // Check settlement hold
                if (activePayment.getSettlementHoldUntil() != null
                        && activePayment.getSettlementHoldUntil().isAfter(LocalDateTime.now())) {
                    throw new BusinessException("PAYMENT_RETRY_TEMPORARILY_BLOCKED",
                            "Payment retry is temporarily blocked due to settlement hold",
                            HttpStatus.CONFLICT);
                }
                throw new BusinessException("PAYMENT_ACTIVE_ATTEMPT_EXISTS",
                        "An active payment attempt already exists", HttpStatus.CONFLICT);
            } else if (activePayment != null && stateTransitionService.isTerminal(activePayment.getStatus())) {
                // Stale pointer: clear it
                guard.setActivePaymentId(null);
            }
        }

        // Check settlement hold on guard level (latest relevant payment)
        // Already handled above

        // Allocate attempt number
        int attemptNumber = guard.getNextAttemptNumber();

        // Generate transaction code with collision retry
        String txCode = null;
        for (int i = 0; i < MAX_TX_CODE_RETRIES; i++) {
            String candidate = transactionCodeGenerator.generate(bookingId);
            if (paymentRepository.findByPaymentTransactionCode(candidate).isEmpty()) {
                txCode = candidate;
                break;
            }
        }
        if (txCode == null) {
            throw new BusinessException("INTERNAL_SERVER_ERROR",
                    "Failed to generate unique transaction code", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Create Payment
        Payment payment = new Payment();
        payment.setPaymentTransactionCode(txCode);
        payment.setBookingId(bookingId);
        payment.setAccountId(accountId);
        payment.setAttemptNumber(attemptNumber);
        payment.setAmount(context.getAmount());
        payment.setCurrency(context.getCurrency());
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExpiresAt(context.getExpiresAt());
        payment = paymentRepository.saveAndFlush(payment);

        // Create analytics snapshot
        PaymentAnalyticsSnapshot snapshot = new PaymentAnalyticsSnapshot();
        snapshot.setPaymentId(payment.getId());
        snapshot.setMovieId(context.getAnalyticsSnapshot().getMovieId());
        snapshot.setMovieTitle(context.getAnalyticsSnapshot().getMovieTitle());
        snapshot.setTicketCount(context.getAnalyticsSnapshot().getTicketCount());
        snapshotRepository.save(snapshot);

        // Log
        paymentLogService.log(payment.getId(), PaymentLogEventType.PAYMENT_CREATED, SOURCE,
                ActorType.CUSTOMER, accountId, null, PaymentStatus.PENDING,
                "Payment created", "bookingId=" + bookingId + ",attempt=" + attemptNumber);

        // Update guard
        guard.setActivePaymentId(payment.getId());
        guard.setNextAttemptNumber(attemptNumber + 1);
        guardRepository.save(guard);

        return payment;
    }

    private void handleSessionCreationFailure(Payment payment, Long accountId, String idempotencyKey) {
        transactionTemplate.execute(status -> {
            Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
            p.setStatus(PaymentStatus.FAILED);
            p.setFailedAt(LocalDateTime.now());
            p.setFailureCode("PAYMENT_SESSION_CREATION_FAILED");
            p.setFailureMessageSanitized("Provider session creation failed");
            paymentRepository.save(p);

            BookingPaymentGuard guard = guardRepository.findByBookingIdForUpdate(p.getBookingId()).orElse(null);
            if (guard != null && p.getId().equals(guard.getActivePaymentId())) {
                guard.setActivePaymentId(null);
                guardRepository.save(guard);
            }

            paymentLogService.log(p.getId(), PaymentLogEventType.PAYMENT_FAILED, SOURCE,
                    ActorType.SYSTEM, null, PaymentStatus.PENDING, PaymentStatus.FAILED,
                    "Session creation failed", null);

            markIdempotencyFailed(accountId, "CREATE_PAYMENT", idempotencyKey,
                    "PAYMENT_SESSION_CREATION_FAILED", "Provider session creation failed");

            return null;
        });
    }

    private void markIdempotencyFailed(Long accountId, String operation, String idempotencyKey,
                                       String errorCode, String errorMessage) {
        try {
            transactionTemplate.execute(status -> {
                var record = idempotencyRepository
                        .findAndLockByAccountIdAndOperationAndIdempotencyKey(accountId, operation, idempotencyKey);
                if (record.isPresent()) {
                    PaymentIdempotencyRecord r = record.get();
                    r.setProcessingStatus(IdempotencyProcessingStatus.FAILED);
                    r.setErrorCode(errorCode);
                    r.setLastError(errorMessage);
                    r.setResponseStatus(resolveHttpStatus(errorCode));
                    try {
                        r.setResponseBodySanitized(objectMapper.writeValueAsString(
                                new com.project.paymentservice.common.ApiResponse<>(
                                        false, errorMessage, errorCode, null, null)));
                    } catch (Exception ignored) {
                    }
                    idempotencyRepository.save(r);
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to mark idempotency as FAILED for key={}", idempotencyKey, e);
        }
    }

    private void completeIdempotency(Long accountId, String operation, String idempotencyKey,
                                      int httpStatus, Object responseData, Long paymentId) {
        var record = idempotencyRepository
                .findAndLockByAccountIdAndOperationAndIdempotencyKey(accountId, operation, idempotencyKey);
        if (record.isPresent()) {
            PaymentIdempotencyRecord r = record.get();
            r.setProcessingStatus(IdempotencyProcessingStatus.COMPLETED);
            r.setPaymentId(paymentId);
            r.setResponseStatus(httpStatus);
            try {
                r.setResponseBodySanitized(objectMapper.writeValueAsString(
                        new com.project.paymentservice.common.ApiResponse<>(
                                true, "Success", null, responseData, null)));
            } catch (Exception ignored) {
            }
            idempotencyRepository.save(r);
        }
    }

    private <T> T deserializeReplay(PaymentIdempotencyRecord record, Class<T> responseType) {
        if (record.getProcessingStatus() == IdempotencyProcessingStatus.FAILED) {
            throw new BusinessException(
                    record.getErrorCode() != null ? record.getErrorCode() : "INTERNAL_SERVER_ERROR",
                    record.getLastError() != null ? record.getLastError() : "Previous request failed",
                    HttpStatus.valueOf(record.getResponseStatus() != null ? record.getResponseStatus() : 500));
        }

        try {
            String body = record.getResponseBodySanitized();
            if (body != null) {
                var apiResp = objectMapper.readTree(body);
                var dataNode = apiResp.get("data");
                if (dataNode != null && !dataNode.isNull()) {
                    return objectMapper.treeToValue(dataNode, responseType);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to deserialize idempotency replay for key={}", record.getIdempotencyKey(), e);
        }
        return null;
    }

    private int resolveHttpStatus(String errorCode) {
        if (errorCode == null) return 500;
        return switch (errorCode) {
            case "FORBIDDEN" -> 403;
            case "PAYMENT_NOT_FOUND", "BOOKING_NOT_FOUND" -> 404;
            case "BOOKING_NOT_PAYABLE", "PAYMENT_ACTIVE_ATTEMPT_EXISTS",
                 "PAYMENT_RETRY_TEMPORARILY_BLOCKED" -> 409;
            case "PAYMENT_SESSION_CREATION_FAILED" -> 502;
            case "BOOKING_SERVICE_UNAVAILABLE" -> 503;
            default -> 400;
        };
    }

    private static class IdempotencyResult {
        final PaymentIdempotencyRecord record;
        final boolean replay;

        IdempotencyResult(PaymentIdempotencyRecord record, boolean replay) {
            this.record = record;
            this.replay = replay;
        }

        boolean isReplay() {
            return replay;
        }
    }
}
