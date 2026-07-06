package com.project.promotionservice.service.booking;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingContext {
    private Long bookingId;
    private Long userId;
    private String status;
    private LocalDateTime expiresAt;
    private BigDecimal amount;
}
