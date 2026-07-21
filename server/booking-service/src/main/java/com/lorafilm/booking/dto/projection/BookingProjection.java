package com.lorafilm.booking.dto.projection;

import com.lorafilm.booking.domain.enums.BookingStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface BookingProjection {
    Long getId();
    UUID getPublicId();
    String getBookingCode();
    Long getUserId();
    BigDecimal getTotalAmount();
    BookingStatus getStatus();
}
