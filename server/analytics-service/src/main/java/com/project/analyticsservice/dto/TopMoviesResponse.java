package com.project.analyticsservice.dto;

import java.util.List;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopMoviesResponse {
    private String metric;
    private String mode;
    private AnalyticsPeriodResponse period;
    private String currency;
    private List<TopMovieItemResponse> movies;
    private LocalDateTime lastUpdatedAt;
}
