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
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.booking.service.InternalBookingService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
    private final BookingTicketService ticketService;

    public InternalBookingServiceImpl(BookingRepository bookingRepository,
                                       BookingMapper bookingMapper,
                                       BookingStatusTransitionService statusTransitionService,
                                       BookingStatusHistoryService historyService,
                                       BookingAuditService auditService,
                                       BookingOperationLogService operationLogService,
                                       BookingOutboxService outboxService,
                                       BookingMetricsManager bookingMetricsManager,
                                       BookingTicketService ticketService) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.statusTransitionService = statusTransitionService;
        this.historyService = historyService;
        this.auditService = auditService;
        this.operationLogService = operationLogService;
        this.outboxService = outboxService;
        this.bookingMetricsManager = bookingMetricsManager;
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
        
        if (targetStatus == BookingStatus.CONFIRMED) {
            ticketService.generateTicketsForConfirmedBooking(savedBooking.getId());
            if (savedBooking.getFoodOrder() != null) {
                com.lorafilm.booking.food.event.FoodOrderConfirmedEvent foodEvent = new com.lorafilm.booking.food.event.FoodOrderConfirmedEvent(
                        savedBooking.getId().toString(),
                        savedBooking.getFoodOrder().getPublicId(),
                        savedBooking.getFoodOrder().getFinalAmount()
                );
                outboxService.createOutboxEvent("FoodOrder", savedBooking.getFoodOrder().getId(), "FOOD_ORDER_CONFIRMED", foodEvent);
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
}
