package com.project.promotionservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingResponse {
    private Long bookingId;
    private String bookingCode;
    private Long showtimeId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
