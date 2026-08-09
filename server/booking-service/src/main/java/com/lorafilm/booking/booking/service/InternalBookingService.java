package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.response.EmergencyShowtimeClosureResponse;

public interface InternalBookingService {

    BookingAdminResponse confirmBooking(String publicId);

    BookingAdminResponse expireBooking(String publicId);

    BookingAdminResponse refundBooking(String publicId);

    BookingAdminResponse getBookingByCode(String bookingCode);

    EmergencyShowtimeClosureResponse closeShowtimeForEmergency(String showtimePublicId, String reason);
}
