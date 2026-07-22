package com.lorafilm.booking.booking.mapper;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingAdminResponse toAdminResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingAdminResponse response = new BookingAdminResponse();
        response.setId(booking.getId());
        response.setPublicId(booking.getPublicId());
        response.setBookingCode(booking.getBookingCode());
        response.setUserId(booking.getUserId());
        response.setShowtimeId(booking.getShowtimeId());
        response.setMovieId(booking.getMovieId());
        response.setCinemaId(booking.getCinemaId());
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
        response.setConfirmedAt(booking.getConfirmedAt());
        response.setCancelledAt(booking.getCancelledAt());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    public BookingDetailResponse toDetailResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingDetailResponse response = new BookingDetailResponse();
        response.setId(booking.getId());
        response.setPublicId(booking.getPublicId());
        response.setBookingCode(booking.getBookingCode());
        response.setUserId(booking.getUserId());
        response.setShowtimeId(booking.getShowtimeId());
        response.setMovieId(booking.getMovieId());
        response.setCinemaId(booking.getCinemaId());
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
}
