package com.lorafilm.booking.payment.event;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.audit.entity.BookingAuditLog;
import com.lorafilm.booking.audit.entity.BookingOperationLog;
import com.lorafilm.booking.audit.repository.BookingAuditLogRepository;
import com.lorafilm.booking.audit.repository.BookingOperationLogRepository;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingStatusHistory;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingStatusHistoryRepository;
import com.lorafilm.booking.booking.repository.BookingTicketRepository;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.food.event.FoodOrderConfirmedEvent;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import com.lorafilm.booking.payment.enums.PaymentEventStatus;
import com.lorafilm.booking.payment.enums.PaymentEventType;
import com.lorafilm.booking.payment.event.contract.PaymentEvent;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import com.lorafilm.booking.reservation.service.SeatReservationService;

@Service
public class PaymentEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProcessor.class);

    private final BookingRepository bookingRepository;
    private final BookingPaymentEventRepository paymentEventRepository;
    private final BookingStatusHistoryRepository statusHistoryRepository;
    private final BookingAuditLogRepository auditLogRepository;
    private final BookingOperationLogRepository operationLogRepository;
    private final BookingOutboxService outboxService;
    private final SeatReservationService seatReservationService;
    private final BookingTicketService bookingTicketService;
    private final BookingSnapshotRepository bookingSnapshotRepository;
    private final BookingTicketRepository bookingTicketRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventProcessor(
            BookingRepository bookingRepository,
            BookingPaymentEventRepository paymentEventRepository,
            BookingStatusHistoryRepository statusHistoryRepository,
            BookingAuditLogRepository auditLogRepository,
            BookingOperationLogRepository operationLogRepository,
            BookingOutboxService outboxService,
            SeatReservationService seatReservationService,
            BookingTicketService bookingTicketService,
            BookingSnapshotRepository bookingSnapshotRepository,
            BookingTicketRepository bookingTicketRepository,
            ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.operationLogRepository = operationLogRepository;
        this.outboxService = outboxService;
        this.seatReservationService = seatReservationService;
        this.bookingTicketService = bookingTicketService;
        this.bookingSnapshotRepository = bookingSnapshotRepository;
        this.bookingTicketRepository = bookingTicketRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(PaymentEvent event, String rawPayload) {
        long startTime = System.currentTimeMillis();
        Long bookingId = event.payload().bookingId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("BOOKING_NOT_FOUND", "Booking not found with ID: " + bookingId));

        PaymentEventType dbEventType = mapToDbEventType(event.eventType());
        PaymentEventStatus dbEventStatus = mapToDbEventStatus(event.payload().paymentStatus());

        // 1. Persist immutable Payment Event Snapshot
        BookingPaymentEvent paymentEventSnapshot = new BookingPaymentEvent();
        paymentEventSnapshot.setPublicId(event.eventId());
        paymentEventSnapshot.setBooking(booking);
        paymentEventSnapshot.setPaymentId(event.payload().paymentId());
        paymentEventSnapshot.setTransactionId(event.payload().transactionCode());
        paymentEventSnapshot.setGatewayTransactionId(event.payload().externalTransactionId());
        paymentEventSnapshot.setPaymentProvider(event.producer());
        paymentEventSnapshot.setPaymentMethod(event.payload().paymentMethod());
        paymentEventSnapshot.setEventType(dbEventType);
        paymentEventSnapshot.setAmount(event.payload().amount());
        paymentEventSnapshot.setCurrency(event.payload().currency());
        paymentEventSnapshot.setRequestPayload(rawPayload);
        paymentEventSnapshot.setStatus(dbEventStatus);
        paymentEventSnapshot.setOccurredAt(event.occurredAt());
        paymentEventRepository.save(paymentEventSnapshot);

        log.debug("Persisted BookingPaymentEvent snapshot: {} for booking: {}", event.eventId(), bookingId);

        BookingStatus oldStatus = booking.getBookingStatus();
        BookingStatus targetBookingStatus = null;
        PaymentStatus targetPaymentStatus = null;
        String reason = null;

        switch (event.eventType()) {
            case "PAYMENT_SUCCESS":
                targetBookingStatus = BookingStatus.CONFIRMED;
                targetPaymentStatus = PaymentStatus.SUCCESS;
                reason = "Payment completed successfully.";
                break;
            case "PAYMENT_FAILED":
                targetBookingStatus = BookingStatus.CANCELLED;
                targetPaymentStatus = PaymentStatus.FAILED;
                reason = event.payload().errorMessage() != null ? event.payload().errorMessage() : "Payment transaction failed.";
                break;
            case "PAYMENT_CANCELLED":
                targetBookingStatus = BookingStatus.CANCELLED;
                targetPaymentStatus = PaymentStatus.FAILED;
                reason = "Payment cancelled by client.";
                break;
            case "PAYMENT_EXPIRED":
                targetBookingStatus = BookingStatus.EXPIRED;
                targetPaymentStatus = PaymentStatus.FAILED;
                reason = "Payment window expired.";
                break;
            case "PAYMENT_PENDING":
                targetPaymentStatus = PaymentStatus.PENDING;
                reason = "Payment is pending processing.";
                break;
            case "PAYMENT_REFUNDED":
                // Refund is owned by Payment Service. Only record snapshot, do NOT modify booking state automatically.
                log.info("Recorded PAYMENT_REFUNDED snapshot. No automatic changes made to booking state.");
                break;
            default:
                log.warn("Unhandle event type: {}", event.eventType());
                break;
        }

        // 2. Execute Booking Business Logic & updates if status transition is required
        if (targetBookingStatus != null && targetBookingStatus != oldStatus) {
            booking.changeStatus(targetBookingStatus, event.occurredAt());
            if (targetPaymentStatus != null) {
                booking.setPaymentStatus(targetPaymentStatus);
            }
            if (targetBookingStatus == BookingStatus.CONFIRMED) {
                booking.setConfirmedAt(event.occurredAt());
            } else if (targetBookingStatus == BookingStatus.CANCELLED) {
                booking.setCancelledAt(event.occurredAt());
                booking.setCancelReasonCode("PAYMENT_FAIL");
                booking.setCancelReasonDetail(reason);
            } else if (targetBookingStatus == BookingStatus.EXPIRED) {
                booking.setExpiredAt(event.occurredAt());
            }
            Booking savedBooking = bookingRepository.save(booking);

            // Sync with food order status is handled automatically inside booking.changeStatus()
            if (savedBooking.getBookingStatus() == BookingStatus.CONFIRMED) {
                bookingTicketService.generateTicketsForConfirmedBooking(savedBooking.getId());
                if (savedBooking.getFoodOrder() != null) {
                    FoodOrderConfirmedEvent foodEvent = new FoodOrderConfirmedEvent(
                            savedBooking.getId().toString(),
                            savedBooking.getFoodOrder().getPublicId(),
                            savedBooking.getFoodOrder().getFinalAmount()
                    );
                    outboxService.createOutboxEvent("FoodOrder", savedBooking.getFoodOrder().getId(), "FOOD_ORDER_CONFIRMED", foodEvent);
                }

                // Sync seat reservations status
                if (targetBookingStatus == BookingStatus.CANCELLED || targetBookingStatus == BookingStatus.EXPIRED) {
                    seatReservationService.handleBookingStatusChange(savedBooking.getId(), targetBookingStatus, reason);
                }

                // 3. Persist Booking Status History
                BookingStatusHistory history = new BookingStatusHistory();
                history.setBooking(savedBooking);
                history.setFromStatus(oldStatus.name());
                history.setToStatus(targetBookingStatus.name());
                history.setReason(reason);
                history.setSource("PAYMENT_EVENT_CONSUMER");
                history.setChangedBy(event.producer());
                statusHistoryRepository.save(history);

                // 4. Persist Audit Log
                BookingAuditLog auditLog = new BookingAuditLog();
                auditLog.setPublicId(UUID.randomUUID().toString());
                auditLog.setBookingId(savedBooking.getId());
                auditLog.setActor(event.producer());
                auditLog.setAction("UPDATE_STATUS_VIA_PAYMENT");
                auditLog.setFieldName("bookingStatus");
                auditLog.setOldValue(oldStatus.name());
                auditLog.setNewValue(targetBookingStatus.name());
                auditLog.setRequestId(event.correlationId());
                auditLog.setTraceId(event.eventId());
                auditLogRepository.save(auditLog);

                // 5. Create Outbox Event
                String outboxEventType = "BOOKING_" + targetBookingStatus.name();
                outboxService.createOutboxEvent("BOOKING", savedBooking.getId(), outboxEventType, savedBooking);
            }

            // 6. Persist Operation Log
            BookingOperationLog opLog = new BookingOperationLog();
            opLog.setPublicId(UUID.randomUUID().toString());
            opLog.setBookingId(bookingId);
            opLog.setOperationType("PROCESS_PAYMENT_EVENT_" + event.eventType());
            opLog.setRequestId(event.correlationId());
            opLog.setTraceId(event.eventId());
            opLog.setActor(event.producer());
            opLog.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            opLog.setSuccess(true);
            operationLogRepository.save(opLog);
        }
    }

    private PaymentEventType mapToDbEventType(String eventType) {
        return switch (eventType) {
            case "PAYMENT_REQUESTED" ->
                PaymentEventType.PAYMENT_CREATED;
            case "PAYMENT_PENDING" ->
                PaymentEventType.PAYMENT_PENDING;
            case "PAYMENT_SUCCESS" ->
                PaymentEventType.PAYMENT_SUCCESS;
            case "PAYMENT_FAILED" ->
                PaymentEventType.PAYMENT_FAILED;
            case "PAYMENT_CANCELLED" ->
                PaymentEventType.PAYMENT_CANCELLED;
            case "PAYMENT_EXPIRED" ->
                PaymentEventType.PAYMENT_TIMEOUT;
            case "PAYMENT_REFUNDED" ->
                PaymentEventType.REFUND_SUCCESS;
            default ->
                PaymentEventType.valueOf(eventType);
        };
    }

    private PaymentEventStatus mapToDbEventStatus(String status) {
        if (status == null) {
            return PaymentEventStatus.PENDING;
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS" ->
                PaymentEventStatus.SUCCESS;
            case "FAILED", "EXPIRED", "CANCELLED" ->
                PaymentEventStatus.FAILED;
            default ->
                PaymentEventStatus.PENDING;
        };
    }

}
