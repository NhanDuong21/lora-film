package com.project.analyticsservice.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieRevenueTrendItemResponse {
    private String statDate;
    private Integer ticketsSold;
    private BigDecimal revenue;
}
