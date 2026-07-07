package com.project.promotionservice.service.booking;

import com.project.promotionservice.dto.BookingResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class StubBookingServiceClient implements BookingServiceClient {

    @Override
    public BookingResponse getBooking(Long bookingId, String authHeader) {
        return BookingResponse.builder()
                .bookingId(bookingId)
                .status("PENDING_PAYMENT")
                .build();
    }
}
