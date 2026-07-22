package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;

public interface InternalBookingService {

    BookingAdminResponse confirmBooking(Long bookingId);

    BookingAdminResponse expireBooking(Long bookingId);

    BookingAdminResponse refundBooking(Long bookingId);

    BookingAdminResponse getBookingByCode(String bookingCode);
}
