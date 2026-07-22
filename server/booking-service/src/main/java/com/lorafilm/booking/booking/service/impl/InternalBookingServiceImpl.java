package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.booking.service.InternalBookingService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InternalBookingServiceImpl implements InternalBookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingStatusTransitionService statusTransitionService;
    private final BookingStatusHistoryService historyService;
    private final BookingAuditService auditService;
    private final BookingOperationLogService operationLogService;
    private final BookingOutboxService outboxService;

    public InternalBookingServiceImpl(BookingRepository bookingRepository,
                                       BookingMapper bookingMapper,
                                       BookingStatusTransitionService statusTransitionService,
                                       BookingStatusHistoryService historyService,
                                       BookingAuditService auditService,
                                       BookingOperationLogService operationLogService,
                                       BookingOutboxService outboxService) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.statusTransitionService = statusTransitionService;
        this.historyService = historyService;
        this.auditService = auditService;
        this.operationLogService = operationLogService;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    public BookingAdminResponse confirmBooking(Long bookingId) {
        return processStatusChange(bookingId, BookingStatus.CONFIRMED, PaymentStatus.SUCCESS, "CONFIRM_BOOKING", "Internal Payment Confirmation");
    }

    @Override
    @Transactional
    public BookingAdminResponse expireBooking(Long bookingId) {
        return processStatusChange(bookingId, BookingStatus.EXPIRED, PaymentStatus.FAILED, "EXPIRE_BOOKING", "Internal Timeout Expiration");
    }

    @Override
    @Transactional
    public BookingAdminResponse refundBooking(Long bookingId) {
        return processStatusChange(bookingId, BookingStatus.REFUNDED, PaymentStatus.REFUNDED, "REFUND_BOOKING", "Internal Booking Refund");
    }

    @Override
    @Transactional(readOnly = true)
    public BookingAdminResponse getBookingByCode(String bookingCode) {
        if (bookingCode == null || bookingCode.trim().isEmpty()) {
            throw new BusinessException("INVALID_BOOKING_CODE", "Booking code cannot be null or empty");
        }

        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BookingNotFoundException(bookingCode));

        return bookingMapper.toAdminResponse(booking);
    }

    private BookingAdminResponse processStatusChange(Long bookingId, BookingStatus targetStatus, PaymentStatus targetPaymentStatus, String operationType, String reason) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

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

        return bookingMapper.toAdminResponse(savedBooking);
    }
}
