package com.project.analyticsservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieRevenueDetailResponse {
    private Long movieId;
    private String movieTitle;
    private String mode;
    private String startDate;
    private String endDate;
    private Integer totalTicketsSold;
    private BigDecimal totalRevenue;
    private BigDecimal averageRevenuePerTicket;
    private String currency;
    private LocalDateTime updatedAt;
}
