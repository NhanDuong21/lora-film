package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingPaymentContextDto;
import com.lorafilm.booking.booking.dto.BookingPaymentResultRequestDto;
import com.lorafilm.booking.booking.dto.BookingPaymentResultResponseDto;

public interface InternalBookingService {

    BookingAdminResponse confirmBooking(String publicId);

    BookingAdminResponse expireBooking(String publicId);

    BookingAdminResponse refundBooking(String publicId);

    BookingAdminResponse getBookingByCode(String bookingCode);

    BookingPaymentContextDto getPaymentContext(Long bookingId);

    BookingPaymentResultResponseDto processPaymentResult(Long bookingId, BookingPaymentResultRequestDto request);
}
