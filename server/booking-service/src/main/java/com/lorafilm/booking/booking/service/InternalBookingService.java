package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;

public interface InternalBookingService {

    BookingAdminResponse confirmBooking(String publicId);

    BookingAdminResponse expireBooking(String publicId);

    BookingAdminResponse refundBooking(String publicId);

    BookingAdminResponse getBookingByCode(String bookingCode);
}
