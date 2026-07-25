package com.lorafilm.booking.booking.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.dto.BookingPriceSnapshotPayload;
import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.dto.response.InternalPaymentContextResponse;
import com.lorafilm.booking.booking.dto.response.InternalPaymentResultResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.InternalBookingPaymentService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import com.lorafilm.booking.payment.enums.PaymentEventStatus;
import com.lorafilm.booking.payment.enums.PaymentEventType;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
public class InternalBookingPaymentServiceImpl implements InternalBookingPaymentService {

    private final BookingRepository bookingRepository;
    private final BookingPriceSnapshotRepository priceSnapshotRepository;
    private final BookingPaymentEventRepository paymentEventRepository;
    private final BookingStatusHistoryService historyService;
    private final ObjectMapper objectMapper;

    public InternalBookingPaymentServiceImpl(BookingRepository bookingRepository,
                                             BookingPriceSnapshotRepository priceSnapshotRepository,
                                             BookingPaymentEventRepository paymentEventRepository,
                                             BookingStatusHistoryService historyService,
                                             ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.historyService = historyService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public InternalPaymentContextResponse getPaymentContext(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BookingNotFoundException(String.valueOf(bookingId)));
        Instant now = Instant.now();
        boolean payable = booking.getBookingStatus() == BookingStatus.PENDING_PAYMENT
                && booking.getExpiresAt() != null
                && booking.getExpiresAt().isAfter(now)
                && booking.getFinalAmount() != null
                && booking.getFinalAmount().signum() > 0;
        if (!payable) {
            throw new BusinessException(
                    "BOOKING_NOT_PAYABLE",
                    "Booking is not pending payment or its payment deadline has passed",
                    HttpStatus.CONFLICT);
        }
        BookingPriceSnapshotPayload snapshot = readSnapshot(bookingId);
        return new InternalPaymentContextResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getBookingStatus().name(),
                true,
                booking.getFinalAmount(),
                booking.getCurrency(),
                LocalDateTime.ofInstant(booking.getExpiresAt(), ZoneOffset.UTC),
                new InternalPaymentContextResponse.AnalyticsSnapshot(
                        snapshot.movieId(), snapshot.movieTitle(), snapshot.seats().size()));
    }

    @Override
    @Transactional
    public InternalPaymentResultResponse recordPaymentResult(Long bookingId,
                                                             InternalPaymentResultRequest request) {
        String result = normalizeResult(request.result());
        Booking booking = bookingRepository.findByIdForPaymentUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.valueOf(bookingId)));
        BookingPaymentEvent replay = paymentEventRepository.findByPublicId(request.eventId()).orElse(null);
        if (replay != null) {
            if (!replay.getBooking().getId().equals(bookingId)
                    || replay.getAmount().compareTo(request.amount()) != 0
                    || !replay.getCurrency().equals(request.currency())
                    || !Objects.equals(replay.getPaymentId(), request.paymentId())
                    || replay.getEventType() != eventType(result)
                    || !Objects.equals(replay.getPaymentMethod(), request.paymentMethod())) {
                throw new BusinessException(
                        "PAYMENT_EVENT_ID_REUSED",
                        "Payment event ID was reused with a different payload",
                        HttpStatus.CONFLICT);
            }
            return response(booking, request.eventId(), true);
        }

        validateAmountAndCurrency(booking, request);
        Instant occurredAt = request.occurredAt() == null
                ? Instant.now() : request.occurredAt().toInstant(ZoneOffset.UTC);

        if ("SUCCESS".equals(result)) {
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                throw new BusinessException(
                        "BOOKING_ALREADY_PAID",
                        "Booking is already confirmed by another payment",
                        HttpStatus.CONFLICT);
            }
            if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT
                    || booking.getExpiresAt() == null
                    || !occurredAt.isBefore(booking.getExpiresAt())) {
                throw new BusinessException(
                        "BOOKING_NOT_PAYABLE",
                        "Expired or non-pending Booking cannot be confirmed",
                        HttpStatus.CONFLICT);
            }
            booking.changeStatus(BookingStatus.CONFIRMED, occurredAt);
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            booking.setPaymentReference(reference(request));
            booking.setPaymentMethodSnapshot(request.paymentMethod());
            historyService.saveHistory(
                    booking, BookingStatus.PENDING_PAYMENT.name(), BookingStatus.CONFIRMED.name(),
                    "Authoritative payment result", "PAYMENT_SERVICE", String.valueOf(request.paymentId()));
        } else if ("FAILED".equals(result) || "CANCELLED".equals(result) || "TIMEOUT".equals(result)) {
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                throw new BusinessException(
                        "BOOKING_ALREADY_PAID",
                        "A failed payment cannot replace a successful payment",
                        HttpStatus.CONFLICT);
            }
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        bookingRepository.save(booking);
        BookingPaymentEvent event = toEvent(booking, request, result, occurredAt);
        paymentEventRepository.save(event);
        return response(booking, request.eventId(), false);
    }

    private BookingPaymentEvent toEvent(Booking booking,
                                        InternalPaymentResultRequest request,
                                        String result,
                                        Instant occurredAt) {
        BookingPaymentEvent event = new BookingPaymentEvent();
        event.setPublicId(request.eventId());
        event.setBooking(booking);
        event.setPaymentId(request.paymentId());
        event.setTransactionId(request.paymentTransactionCode());
        event.setGatewayTransactionId(request.externalTransactionId());
        event.setPaymentMethod(request.paymentMethod());
        event.setEventType(eventType(result));
        event.setAmount(request.amount());
        event.setCurrency(request.currency());
        event.setStatus("SUCCESS".equals(result) ? PaymentEventStatus.SUCCESS
                : "PENDING".equals(result) ? PaymentEventStatus.PENDING : PaymentEventStatus.FAILED);
        event.setOccurredAt(occurredAt);
        try {
            event.setRequestPayload(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    "PAYMENT_RESULT_INVALID",
                    "Cannot serialize payment result",
                    HttpStatus.BAD_REQUEST);
        }
        return event;
    }

    private PaymentEventType eventType(String result) {
        return switch (result) {
            case "SUCCESS" -> PaymentEventType.PAYMENT_SUCCESS;
            case "FAILED" -> PaymentEventType.PAYMENT_FAILED;
            case "TIMEOUT" -> PaymentEventType.PAYMENT_TIMEOUT;
            case "CANCELLED" -> PaymentEventType.PAYMENT_CANCELLED;
            default -> PaymentEventType.PAYMENT_PENDING;
        };
    }

    private String normalizeResult(String result) {
        String normalized = result == null ? "" : result.trim().toUpperCase();
        if (!List.of("SUCCESS", "FAILED", "CANCELLED", "TIMEOUT", "PENDING").contains(normalized)) {
            throw new BusinessException(
                    "PAYMENT_RESULT_INVALID",
                    "Unsupported payment result: " + result,
                    HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private void validateAmountAndCurrency(Booking booking, InternalPaymentResultRequest request) {
        BigDecimal amount = request.amount();
        if (amount == null || booking.getFinalAmount().compareTo(amount) != 0
                || request.currency() == null || !booking.getCurrency().equals(request.currency())) {
            throw new BusinessException(
                    "PAYMENT_AMOUNT_MISMATCH",
                    "Payment result amount/currency does not match the Booking",
                    HttpStatus.CONFLICT);
        }
    }

    private BookingPriceSnapshotPayload readSnapshot(Long bookingId) {
        BookingPriceSnapshot snapshot = priceSnapshotRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(
                        "BOOKING_PRICE_SNAPSHOT_MISSING",
                        "Authoritative Booking price snapshot is missing",
                        HttpStatus.CONFLICT));
        try {
            return objectMapper.readValue(
                    snapshot.getPricingBreakdownJson(), BookingPriceSnapshotPayload.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    "BOOKING_PRICE_SNAPSHOT_INVALID",
                    "Authoritative Booking price snapshot is unreadable",
                    HttpStatus.CONFLICT);
        }
    }

    private String reference(InternalPaymentResultRequest request) {
        return request.paymentTransactionCode() == null || request.paymentTransactionCode().isBlank()
                ? String.valueOf(request.paymentId()) : request.paymentTransactionCode();
    }

    private InternalPaymentResultResponse response(Booking booking, String eventId, boolean idempotent) {
        return new InternalPaymentResultResponse(
                booking.getId(), eventId, booking.getBookingStatus().name(),
                booking.getPaymentStatus().name(), idempotent);
    }
}
