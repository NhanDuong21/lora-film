package com.lorafilm.booking.booking.mapper;

import com.lorafilm.booking.booking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public com.lorafilm.booking.booking.dto.BookingAdminResponse toAdminResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        com.lorafilm.booking.booking.dto.BookingAdminResponse response = new com.lorafilm.booking.booking.dto.BookingAdminResponse();
        response.setId(booking.getId());
        response.setPublicId(booking.getPublicId());
        response.setBookingCode(booking.getBookingCode());
        response.setUserId(booking.getUserId());
        response.setShowtimeId(booking.getShowtimeId());
        response.setShowtimePublicId(booking.getShowtimePublicId());
        response.setMovieId(booking.getMovieId());
        response.setCinemaId(booking.getCinemaId());
        response.setCinemaPublicId(booking.getCinemaPublicId());
        response.setAuditoriumId(booking.getAuditoriumId());
        response.setTicketAmount(booking.getTicketAmount());
        response.setFoodAmount(booking.getFoodAmount());
        response.setServiceFee(booking.getServiceFee());
        response.setTaxAmount(booking.getTaxAmount());
        response.setPromotionDiscount(booking.getPromotionDiscount());
        response.setVoucherDiscount(booking.getVoucherDiscount());
        response.setFinalAmount(booking.getFinalAmount());
        response.setCurrency(booking.getCurrency());
        response.setBookingStatus(booking.getBookingStatus());
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setExpiresAt(booking.getExpiresAt());
        response.setAmountLockedAt(booking.getAmountLockedAt());
        response.setConfirmedAt(booking.getConfirmedAt());
        response.setCancelledAt(booking.getCancelledAt());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    public com.lorafilm.booking.booking.dto.BookingDetailResponse toAdminDetailResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        com.lorafilm.booking.booking.dto.BookingDetailResponse response = new com.lorafilm.booking.booking.dto.BookingDetailResponse();
        response.setId(booking.getId());
        response.setPublicId(booking.getPublicId());
        response.setBookingCode(booking.getBookingCode());
        response.setUserId(booking.getUserId());
        response.setShowtimeId(booking.getShowtimeId());
        response.setShowtimePublicId(booking.getShowtimePublicId());
        response.setMovieId(booking.getMovieId());
        response.setCinemaId(booking.getCinemaId());
        response.setCinemaPublicId(booking.getCinemaPublicId());
        response.setAuditoriumId(booking.getAuditoriumId());
        response.setTicketAmount(booking.getTicketAmount());
        response.setFoodAmount(booking.getFoodAmount());
        response.setServiceFee(booking.getServiceFee());
        response.setTaxAmount(booking.getTaxAmount());
        response.setPromotionDiscount(booking.getPromotionDiscount());
        response.setVoucherDiscount(booking.getVoucherDiscount());
        response.setFinalAmount(booking.getFinalAmount());
        response.setCurrency(booking.getCurrency());
        response.setBookingStatus(booking.getBookingStatus());
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setPaymentMethodSnapshot(booking.getPaymentMethodSnapshot());
        response.setPaymentProvider(booking.getPaymentProvider());
        response.setPaymentReference(booking.getPaymentReference());
        response.setExpiresAt(booking.getExpiresAt());
        response.setAmountLockedAt(booking.getAmountLockedAt());
        response.setConfirmedAt(booking.getConfirmedAt());
        response.setCompletedAt(booking.getCompletedAt());
        response.setCancelledAt(booking.getCancelledAt());
        response.setExpiredAt(booking.getExpiredAt());
        response.setRefundedAt(booking.getRefundedAt());
        response.setCancelReasonCode(booking.getCancelReasonCode());
        response.setCancelReasonDetail(booking.getCancelReasonDetail());
        response.setNote(booking.getNote());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    public com.lorafilm.booking.booking.dto.response.BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new com.lorafilm.booking.booking.dto.response.BookingResponse(
                booking.getPublicId(),
                booking.getBookingCode(),
                booking.getCounterCustomerAccountId(),
                booking.getCounterCustomerName(),
                booking.getCounterCustomerPhone(),
                booking.getCounterCustomerEmail(),
                booking.getShowtimePublicId(),
                booking.getBookingStatus(),
                booking.getFinalAmount(),
                booking.getVoucherDiscount(),
                booking.getPromotionReservationPublicId(),
                booking.getAppliedPromotionsJson(),
                booking.getScorePointsUsed(),
                booking.getScoreDiscount(),
                booking.getCurrency(),
                booking.getExpiresAt(),
                booking.getAmountLockedAt(),
                booking.getCreatedAt());
    }

    public com.lorafilm.booking.booking.dto.response.BookingSummaryResponse toSummaryResponse(
            Booking booking,
            com.lorafilm.booking.booking.dto.response.BookingPresentationResponse presentation,
            com.lorafilm.booking.booking.dto.response.BookingFoodResponse food) {
        if (booking == null) {
            return null;
        }
        return new com.lorafilm.booking.booking.dto.response.BookingSummaryResponse(
                booking.getPublicId(),
                booking.getBookingCode(),
                booking.getCounterCustomerAccountId(),
                booking.getCounterCustomerName(),
                booking.getCounterCustomerPhone(),
                booking.getCounterCustomerEmail(),
                booking.getShowtimeId(),
                booking.getShowtimePublicId(),
                booking.getBookingStatus(),
                booking.getTicketAmount(),
                booking.getFoodAmount(),
                booking.getFinalAmount(),
                booking.getCurrency(),
                booking.getExpiresAt(),
                booking.getCreatedAt(),
                presentation,
                food);
    }

    public com.lorafilm.booking.booking.dto.response.BookingDetailResponse toDetailResponse(
            Booking booking,
            com.lorafilm.booking.booking.dto.response.BookingPresentationResponse presentation,
            com.lorafilm.booking.booking.dto.response.BookingFoodResponse food) {
        if (booking == null) {
            return null;
        }
        return new com.lorafilm.booking.booking.dto.response.BookingDetailResponse(
                booking.getPublicId(),
                booking.getBookingCode(),
                booking.getUserId(),
                booking.getCounterCustomerAccountId(),
                booking.getCounterCustomerName(),
                booking.getCounterCustomerPhone(),
                booking.getCounterCustomerEmail(),
                booking.getShowtimeId(),
                booking.getShowtimePublicId(),
                booking.getMovieId(),
                booking.getCinemaId(),
                booking.getAuditoriumId(),
                booking.getTicketAmount(),
                booking.getFoodAmount(),
                booking.getServiceFee(),
                booking.getTaxAmount(),
                booking.getPromotionDiscount(),
                booking.getVoucherDiscount(),
                booking.getPromotionReservationPublicId(),
                booking.getAppliedPromotionsJson(),
                booking.getScorePointsUsed(),
                booking.getScoreDiscount(),
                booking.getFinalAmount(),
                booking.getCurrency(),
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                booking.getExpiresAt(),
                booking.getAmountLockedAt(),
                booking.getConfirmedAt(),
                booking.getCompletedAt(),
                booking.getCancelledAt(),
                booking.getExpiredAt(),
                booking.getRefundedAt(),
                booking.getCancelReasonCode(),
                booking.getCancelReasonDetail(),
                booking.getNote(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                presentation,
                food);
    }
}
