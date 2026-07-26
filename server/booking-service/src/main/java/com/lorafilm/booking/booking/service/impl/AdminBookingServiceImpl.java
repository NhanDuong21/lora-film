package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.BookingSnapshotDto;
import com.lorafilm.booking.booking.dto.BookingStatusHistoryDto;
import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.booking.dto.UpdateBookingStatusRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingSpecification;
import com.lorafilm.booking.booking.service.AdminBookingService;
import com.lorafilm.booking.booking.service.BookingSnapshotService;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.booking.service.BookingLifecycleService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.response.PagedResponse;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class AdminBookingServiceImpl implements AdminBookingService {

    private static final Logger log = LoggerFactory.getLogger(AdminBookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingStatusTransitionService statusTransitionService;
    private final BookingStatusHistoryService historyService;
    private final BookingAuditService auditService;
    private final BookingOperationLogService operationLogService;
    private final BookingOutboxService outboxService;
    private final BookingTicketService ticketService;
    private final BookingSnapshotService snapshotService;
    private final BookingMetricsManager bookingMetricsManager;
    private final SeatReservationService reservationService;
    private final BookingLifecycleService lifecycleService;

    @Autowired
    public AdminBookingServiceImpl(BookingRepository bookingRepository,
                                  BookingMapper bookingMapper,
                                  BookingStatusTransitionService statusTransitionService,
                                  BookingStatusHistoryService historyService,
                                  BookingAuditService auditService,
                                  BookingOperationLogService operationLogService,
                                  BookingOutboxService outboxService,
                                  BookingTicketService ticketService,
                                  BookingSnapshotService snapshotService,
                                  BookingMetricsManager bookingMetricsManager,
                                  SeatReservationService reservationService,
                                  BookingLifecycleService lifecycleService) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.statusTransitionService = statusTransitionService;
        this.historyService = historyService;
        this.auditService = auditService;
        this.operationLogService = operationLogService;
        this.outboxService = outboxService;
        this.ticketService = ticketService;
        this.snapshotService = snapshotService;
        this.bookingMetricsManager = bookingMetricsManager;
        this.reservationService = reservationService;
        this.lifecycleService = lifecycleService;
    }

    /** Backwards-compatible constructor for existing unit callers. */
    public AdminBookingServiceImpl(BookingRepository bookingRepository,
                                  BookingMapper bookingMapper,
                                  BookingStatusTransitionService statusTransitionService,
                                  BookingStatusHistoryService historyService,
                                  BookingAuditService auditService,
                                  BookingOperationLogService operationLogService,
                                  BookingOutboxService outboxService,
                                  BookingTicketService ticketService,
                                  BookingSnapshotService snapshotService,
                                  BookingMetricsManager bookingMetricsManager) {
        this(bookingRepository, bookingMapper, statusTransitionService, historyService,
                auditService, operationLogService, outboxService, ticketService,
                snapshotService, bookingMetricsManager, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BookingAdminResponse> findBookings(BookingFilterRequest filter) {
        int page = filter != null ? filter.getPage() : 0;
        int size = filter != null && filter.getSize() > 0 ? filter.getSize() : 20;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookingPage = bookingRepository.findAll(BookingSpecification.filterBy(filter), pageable);

        List<BookingAdminResponse> content = bookingPage.map(bookingMapper::toAdminResponse).getContent();

        return new PagedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(String publicId) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking public ID cannot be null or empty");
        }

        MDC.put("bookingId", publicId);
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BookingNotFoundException(java.util.UUID.fromString(publicId)));

        BookingDetailResponse detailResponse = bookingMapper.toAdminDetailResponse(booking);
        Long dbBookingId = booking.getId();

        try {
            BookingSnapshotDto snapshot = snapshotService.findByBooking(dbBookingId);
            detailResponse.setSnapshot(snapshot);
        } catch (BusinessException e) {
            detailResponse.setSnapshot(null);
        }

        try {
            List<BookingTicketDto> tickets = ticketService.findByBooking(dbBookingId);
            detailResponse.setTickets(tickets);
        } catch (BusinessException e) {
            detailResponse.setTickets(Collections.emptyList());
        }

        try {
            List<BookingStatusHistoryDto> statusHistories = historyService.findByBooking(dbBookingId);
            detailResponse.setStatusHistories(statusHistories);
        } catch (BusinessException e) {
            detailResponse.setStatusHistories(Collections.emptyList());
        }

        return detailResponse;
    }

    @Override
    @Transactional
    public BookingAdminResponse updateBookingStatus(String publicId, UpdateBookingStatusRequest request) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking public ID cannot be null or empty");
        }
        if (request == null || request.getStatus() == null) {
            throw new BusinessException("INVALID_REQUEST", "Target status is required");
        }

        MDC.put("bookingId", publicId);
        MDC.put("action", "ADMIN_CHANGE_STATUS");
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BookingNotFoundException(java.util.UUID.fromString(publicId)));

        BookingStatus oldStatus = booking.getBookingStatus();
        BookingStatus newStatus = request.getStatus();

        if (newStatus == BookingStatus.CONFIRMED) {
            throw new BusinessException("CONFIRM_VIA_PAYMENT_RESULT_REQUIRED",
                    "Booking confirmation is performed only by a validated Payment result",
                    org.springframework.http.HttpStatus.GONE);
        }
        if (lifecycleService != null) {
            Booking saved = lifecycleService.transition(booking, newStatus,
                    request.getReason(), request.getSource());
            if (request.getNote() != null) {
                saved.setNote(request.getNote());
                saved = bookingRepository.save(saved);
            }
            return bookingMapper.toAdminResponse(saved);
        }
        statusTransitionService.validateTransition(oldStatus, newStatus);

        Instant now = Instant.now();
        if (newStatus == BookingStatus.CANCELLED) {
            booking.cancel("ADMIN_CANCEL", request.getReason(), now);
        } else {
            booking.changeStatus(newStatus, now);
        }

        if (request.getNote() != null) {
            booking.setNote(request.getNote());
        }

        Booking savedBooking = bookingRepository.save(booking);

        if (reservationService != null
                && (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED)) {
            reservationService.handleBookingStatusChange(savedBooking.getId(), newStatus,
                    request.getReason() != null ? request.getReason() : "Admin lifecycle status change");
        }

        historyService.saveHistory(savedBooking, oldStatus.name(), newStatus.name(), request.getReason(), request.getSource(), "ADMIN");
        auditService.logAudit(savedBooking.getId(), "ADMIN", "CHANGE_STATUS", "bookingStatus", oldStatus.name(), newStatus.name(), null, null, null, null);
        operationLogService.logOperation(savedBooking.getId(), "CHANGE_STATUS", "ADMIN", true, 0L, null, null, request.getReason());
        outboxService.createOutboxEvent("BOOKING", savedBooking.getId(), "BOOKING_" + newStatus.name(), savedBooking);

        // Preserve ticket rows and mark them with the appropriate terminal
        // state for audit/reconciliation.
        if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED || newStatus == BookingStatus.REFUNDED) {
            try {
                if (newStatus == BookingStatus.REFUNDED) {
                    ticketService.refundTickets(savedBooking.getId());
                } else {
                    ticketService.deleteTickets(savedBooking.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to update tickets for bookingId: {}", savedBooking.getId(), e);
            }
        }

        // Increment Metrics
        if (newStatus == BookingStatus.CONFIRMED) {
            bookingMetricsManager.incrementBookingConfirmed();
            bookingMetricsManager.incrementPaymentSuccess();
        } else if (newStatus == BookingStatus.EXPIRED) {
            bookingMetricsManager.incrementBookingExpired();
            bookingMetricsManager.incrementPaymentFailed();
        } else if (newStatus == BookingStatus.CANCELLED) {
            bookingMetricsManager.incrementBookingCancelled();
        }

        return bookingMapper.toAdminResponse(savedBooking);
    }
}
