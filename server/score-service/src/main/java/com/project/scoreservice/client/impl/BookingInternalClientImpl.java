package com.project.scoreservice.client.impl;
 
import com.project.scoreservice.client.BookingContext;
import com.project.scoreservice.client.BookingInternalClient;
import org.springframework.stereotype.Component;
 
import java.time.LocalDateTime;
 
@Component
public class BookingInternalClientImpl implements BookingInternalClient {
 
    @Override
    public BookingContext getBookingContext(Long bookingId) {
        // Stub implementation for integration boundaries testing.
        // In actual production deployment, this would perform a REST call to booking-service.
        
        // Simulating booking not found / service down
        if (bookingId == 9999L) {
            return null;
        }
 
        BookingContext context = new BookingContext();
        context.setBookingId(bookingId);
        context.setUserId(15L); // Matches default test customer ID
        context.setStatus("PENDING_PAYMENT");
        context.setExpiresAt(LocalDateTime.now().plusHours(1));
        context.setRedeemAllowed(true);
 
        // Custom scenario simulations:
        if (bookingId == 1002L) {
            context.setUserId(16L); // owner mismatch
        } else if (bookingId == 1003L) {
            context.setStatus("CANCELLED"); // invalid status
            context.setRedeemAllowed(false);
        } else if (bookingId == 1004L) {
            context.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // expired
        }
 
        return context;
    }
}
