package com.project.analyticsservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MovieDateRangeAggregateProjection {
    Long getMovieId();
    String getMovieTitle();
    Long getTotalTicketsSold();
    BigDecimal getTotalRevenue();
    LocalDateTime getLastUpdatedAt();
}
