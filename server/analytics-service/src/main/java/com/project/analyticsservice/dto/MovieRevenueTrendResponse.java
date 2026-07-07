package com.project.analyticsservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieRevenueTrendResponse {
    private Long movieId;
    private String movieTitle;
    private String startDate;
    private String endDate;
    private String currency;
    private List<MovieRevenueTrendItemResponse> statistics;
}
