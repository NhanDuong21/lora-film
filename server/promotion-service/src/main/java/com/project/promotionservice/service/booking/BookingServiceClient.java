package com.project.promotionservice.service.booking;

import com.project.promotionservice.dto.BookingResponse;

public interface BookingServiceClient {
    BookingResponse getBooking(Long bookingId, String authHeader);
}
