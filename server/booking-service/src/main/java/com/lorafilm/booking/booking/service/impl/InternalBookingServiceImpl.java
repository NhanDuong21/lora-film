package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingPaymentContextDto;
import com.lorafilm.booking.booking.dto.BookingPaymentResultRequestDto;
import com.lorafilm.booking.booking.dto.BookingPaymentResultResponseDto;
import com.lorafilm.booking.booking.dto.BookingSnapshotDto;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingSnapshotService;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.booking.service.InternalBookingService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class InternalBookingServiceImpl implements InternalBookingService {

    private static final Logger log = LoggerFactory.getLogger(InternalBookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingStatusTransitionService statusTransitionService;
    private final BookingStatusHistoryService historyService;
    private final BookingAuditService auditService;
    private final BookingOperationLogService operationLogService;
    private final BookingOutboxService outboxService;
    private final BookingMetricsManager bookingMetricsManager;
    private final BookingPaymentEventRepository paymentEventRepository;
    private final BookingSnapshotService snapshotService;
    private final BookingTicketService ticketService;

    public InternalBookingServiceImpl(BookingRepository bookingRepository,
                                       BookingMapper bookingMapper,
                                       BookingStatusTransitionService statusTransitionService,
                                       BookingStatusHistoryService historyService,
                                       BookingAuditService auditService,
                                       BookingOperationLogService operationLogService,
                                       BookingOutboxService outboxService,
                                       BookingMetricsManager bookingMetricsManager,
                                       BookingPaymentEventRepository paymentEventRepository,
                                       BookingSnapshotService snapshotService,
                                       BookingTicketService ticketService) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.statusTransitionService = statusTransitionService;
        this.historyService = historyService;
        this.auditService = auditService;
        this.operationLogService = operationLogService;
        this.outboxService = outboxService;
        this.bookingMetricsManager = bookingMetricsManager;
        this.paymentEventRepository = paymentEventRepository;
        this.snapshotService = snapshotService;
        this.ticketService = ticketService;
    }

    @Override
    @Transactional
    public BookingAdminResponse confirmBooking(String publicId) {
        return processStatusChange(publicId, BookingStatus.CONFIRMED, PaymentStatus.SUCCESS, "CONFIRM_BOOKING", "Internal Payment Confirmation");
    }

    @Override
    @Transactional
    public BookingAdminResponse expireBooking(String publicId) {
        return processStatusChange(publicId, BookingStatus.EXPIRED, PaymentStatus.FAILED, "EXPIRE_BOOKING", "Internal Timeout Expiration");
    }

    @Override
    @Transactional
    public BookingAdminResponse refundBooking(String publicId) {
        return processStatusChange(publicId, BookingStatus.REFUNDED, PaymentStatus.REFUNDED, "REFUND_BOOKING", "Internal Booking Refund");
    }

    @Override
    @Transactional(readOnly = true)
    public BookingAdminResponse getBookingByCode(String bookingCode) {
        if (bookingCode == null || bookingCode.trim().isEmpty()) {
            throw new BusinessException("INVALID_BOOKING_CODE", "Booking code cannot be null or empty");
        }

        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BookingNotFoundException(bookingCode));

        MDC.put("bookingId", booking.getPublicId());
        return bookingMapper.toAdminResponse(booking);
    }

    private BookingAdminResponse processStatusChange(String publicId, BookingStatus targetStatus, PaymentStatus targetPaymentStatus, String operationType, String reason) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking public ID cannot be null or empty");
        }

        MDC.put("bookingId", publicId);
        MDC.put("action", "CHANGE_BOOKING_STATUS");
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BookingNotFoundException(java.util.UUID.fromString(publicId)));

        BookingStatus oldStatus = booking.getBookingStatus();
        statusTransitionService.validateTransition(oldStatus, targetStatus);

        Instant now = Instant.now();
        booking.changeStatus(targetStatus, now);
        if (targetPaymentStatus != null) {
            booking.setPaymentStatus(targetPaymentStatus);
        }

        Booking savedBooking = bookingRepository.save(booking);

        historyService.saveHistory(savedBooking, oldStatus.name(), targetStatus.name(), reason, "INTERNAL_SERVICE", "SYSTEM");
        auditService.logAudit(savedBooking.getId(), "SYSTEM", operationType, "bookingStatus", oldStatus.name(), targetStatus.name(), null, null, null, null);
        operationLogService.logOperation(savedBooking.getId(), operationType, "SYSTEM", true, 0L, null, null, reason);
        outboxService.createOutboxEvent("BOOKING", savedBooking.getId(), "BOOKING_" + targetStatus.name(), savedBooking);

        // Cancel/Delete tickets if cancelling/refunding/expiring
        if (targetStatus == BookingStatus.CANCELLED || targetStatus == BookingStatus.EXPIRED || targetStatus == BookingStatus.REFUNDED) {
            try {
                ticketService.deleteTickets(savedBooking.getId());
            } catch (Exception e) {
                log.warn("Failed to delete/cancel tickets for bookingId: {}", savedBooking.getId(), e);
            }
        }

        // Increment Metrics
        if (targetStatus == BookingStatus.CONFIRMED) {
            bookingMetricsManager.incrementBookingConfirmed();
            bookingMetricsManager.incrementPaymentSuccess();
        } else if (targetStatus == BookingStatus.EXPIRED) {
            bookingMetricsManager.incrementBookingExpired();
            bookingMetricsManager.incrementPaymentFailed();
        } else if (targetStatus == BookingStatus.CANCELLED) {
            bookingMetricsManager.incrementBookingCancelled();
        }

        return bookingMapper.toAdminResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingPaymentContextDto getPaymentContext(Long bookingId) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        BookingPaymentContextDto dto = new BookingPaymentContextDto();
        dto.setBookingId(booking.getId());
        dto.setAccountId(booking.getUserId());
        dto.setBookingStatus(booking.getBookingStatus().name());
        
        boolean payable = booking.getBookingStatus() == BookingStatus.PENDING_PAYMENT 
                && booking.getExpiresAt().isAfter(Instant.now());
        dto.setPayable(payable);
        dto.setAmount(booking.getFinalAmount());
        dto.setCurrency(booking.getCurrency());
        dto.setExpiresAt(booking.getExpiresAt());

        try {
            BookingSnapshotDto snapshot = snapshotService.findByBooking(bookingId);
            BookingPaymentContextDto.AnalyticsSnapshot analytics = new BookingPaymentContextDto.AnalyticsSnapshot();
            analytics.setMovieId(snapshot.getMovieId());
            analytics.setMovieTitle(snapshot.getMovieTitle());
            analytics.setTicketCount(snapshot.getSeatCount());
            dto.setAnalyticsSnapshot(analytics);
        } catch (Exception e) {
            // Fallback if snapshot is missing
            BookingPaymentContextDto.AnalyticsSnapshot analytics = new BookingPaymentContextDto.AnalyticsSnapshot();
            analytics.setMovieId(booking.getMovieId());
            analytics.setMovieTitle("Movie Title");
            analytics.setTicketCount(0);
            dto.setAnalyticsSnapshot(analytics);
        }

        return dto;
    }

    @Override
    @Transactional
    public BookingPaymentResultResponseDto processPaymentResult(Long bookingId, BookingPaymentResultRequestDto request) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        if (request == null) {
            throw new BusinessException("INVALID_REQUEST", "Request body cannot be null");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        String eventId = request.getEventId();
        
        // 1. Check duplicate eventId
        Optional<com.lorafilm.booking.payment.entity.BookingPaymentEvent> duplicateEvent = 
                paymentEventRepository.findByPublicId(eventId);
        if (duplicateEvent.isPresent()) {
            return new BookingPaymentResultResponseDto(eventId, false, true, "ALREADY_PROCESSED");
        }

        // 2. Check if booking is already confirmed / completed / refunded
        BookingStatus status = booking.getBookingStatus();
        if (status == BookingStatus.CONFIRMED || status == BookingStatus.COMPLETED || status == BookingStatus.REFUNDED) {
            return new BookingPaymentResultResponseDto(eventId, false, false, "ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT");
        }

        // 3. Process payment status
        com.lorafilm.booking.payment.entity.BookingPaymentEvent event = new com.lorafilm.booking.payment.entity.BookingPaymentEvent();
        event.setPublicId(eventId);
        event.setBooking(booking);
        event.setPaymentId(request.getPaymentId());
        event.setTransactionId(request.getPaymentTransactionCode());
        event.setGatewayTransactionId(request.getExternalTransactionId());
        event.setPaymentProvider(request.getPaymentMethod());
        event.setPaymentMethod(request.getPaymentMethod());
        
        boolean isSuccess = "SUCCESS".equalsIgnoreCase(request.getResult());
        event.setEventType(isSuccess ? com.lorafilm.booking.payment.enums.PaymentEventType.PAYMENT_SUCCESS : com.lorafilm.booking.payment.enums.PaymentEventType.PAYMENT_FAILED);
        event.setAmount(request.getAmount());
        event.setCurrency(request.getCurrency() != null ? request.getCurrency() : "VND");
        event.setStatus(isSuccess ? com.lorafilm.booking.payment.enums.PaymentEventStatus.SUCCESS : com.lorafilm.booking.payment.enums.PaymentEventStatus.FAILED);
        event.setOccurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now());
        
        paymentEventRepository.save(event);

        if (isSuccess) {
            booking.setPaymentProvider(request.getPaymentMethod());
            booking.setPaymentReference(request.getPaymentTransactionCode());
            booking.setPaymentMethodSnapshot(request.getPaymentMethod());
            bookingRepository.save(booking);

            // Trigger confirmBooking
            confirmBooking(booking.getPublicId());
            
            return new BookingPaymentResultResponseDto(eventId, true, false, "BOOKING_CONFIRMED");
        } else {
            // If payment failed, trigger expireBooking
            expireBooking(booking.getPublicId());
            return new BookingPaymentResultResponseDto(eventId, false, false, "BOOKING_EXPIRED");
        }
    }
}

