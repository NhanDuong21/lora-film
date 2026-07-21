package com.lorafilm.booking.booking.mapper;

import com.lorafilm.booking.booking.dto.response.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.dto.response.BookingSummaryResponse;
import com.lorafilm.booking.booking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getPublicId(),
                booking.getBookingCode(),
                booking.getBookingStatus(),
                booking.getFinalAmount(),
                booking.getCurrency(),
                booking.getExpiresAt(),
                booking.getCreatedAt());
    }

    public BookingSummaryResponse toSummaryResponse(Booking booking) {
        return new BookingSummaryResponse(
                booking.getPublicId(),
                booking.getBookingCode(),
                booking.getShowtimeId(),
                booking.getBookingStatus(),
                booking.getFinalAmount(),
                booking.getCurrency(),
                booking.getExpiresAt(),
                booking.getCreatedAt());
    }

    public BookingDetailResponse toDetailResponse(Booking booking) {
        return new BookingDetailResponse(
                booking.getPublicId(),
                booking.getBookingCode(),
                booking.getUserId(),
                booking.getShowtimeId(),
                booking.getMovieId(),
                booking.getCinemaId(),
                booking.getAuditoriumId(),
                booking.getTicketAmount(),
                booking.getFoodAmount(),
                booking.getServiceFee(),
                booking.getTaxAmount(),
                booking.getPromotionDiscount(),
                booking.getVoucherDiscount(),
                booking.getFinalAmount(),
                booking.getCurrency(),
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                booking.getExpiresAt(),
                booking.getConfirmedAt(),
                booking.getCompletedAt(),
                booking.getCancelledAt(),
                booking.getExpiredAt(),
                booking.getRefundedAt(),
                booking.getCancelReasonCode(),
                booking.getCancelReasonDetail(),
                booking.getNote(),
                booking.getCreatedAt(),
                booking.getUpdatedAt());
    }
}
