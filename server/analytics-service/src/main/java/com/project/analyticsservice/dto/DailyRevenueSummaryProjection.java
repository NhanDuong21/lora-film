package com.project.analyticsservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface DailyRevenueSummaryProjection {
    BigDecimal getTotalRevenue();
    Long getTotalBookingsCount();
    Long getCancelledBookingsCount();
    Long getTotalTicketsSold();
    LocalDateTime getLastUpdatedAt();
}
