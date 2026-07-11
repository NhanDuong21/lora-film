package com.project.promotionservice.service.booking;

import com.project.promotionservice.exception.BusinessException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Profile("test")
public class StubBookingInternalClient implements BookingInternalClient {

    @Override
    public BookingContext getBookingContext(Long bookingId) {
        if (bookingId == 9999L) {
            throw new BusinessException("Booking not found", "PROMOTION_BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        BookingContext context = new BookingContext();
        context.setBookingId(bookingId);
        context.setUserId(15L); // Default test customer ID
        context.setStatus("PENDING_PAYMENT");
        context.setExpiresAt(LocalDateTime.now().plusDays(1));
        context.setAmount(BigDecimal.valueOf(240000));

        if (bookingId == 1002L) {
            context.setUserId(16L); // owner mismatch
        } else if (bookingId == 1003L) {
            context.setStatus("CANCELLED"); // invalid status
        } else if (bookingId == 1004L) {
            context.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // expired
        } else if (bookingId == 1005L) {
            context.setStatus("CONFIRMED"); // invalid status
        } else if (bookingId == 1006L) {
            context.setStatus("EXPIRED"); // invalid status
        } else if (bookingId == 1007L) {
            context.setStatus("REFUNDED"); // invalid status
        }

        return context;
    }
}
