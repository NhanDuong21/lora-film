package com.lorafilm.movie.integration.tmdb.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.time.ZoneId;

public class TmdbBulkSyncRequest {

    private TmdbSyncScope scope = TmdbSyncScope.FUTURE;
    private LocalDate releaseDateFrom;
    private LocalDate releaseDateTo;

    @Min(value = 1, message = "Số phim tối đa phải lớn hơn 0.")
    @Max(value = 5000, message = "Mỗi lần chỉ được nhập tối đa 5.000 phim.")
    private Integer maxMovies = 500;

    public TmdbSyncScope getScope() {
        return scope;
    }

    public void setScope(TmdbSyncScope scope) {
        this.scope = scope;
    }

    public LocalDate getReleaseDateFrom() {
        return releaseDateFrom;
    }

    public void setReleaseDateFrom(LocalDate releaseDateFrom) {
        this.releaseDateFrom = releaseDateFrom;
    }

    public LocalDate getReleaseDateTo() {
        return releaseDateTo;
    }

    public void setReleaseDateTo(LocalDate releaseDateTo) {
        this.releaseDateTo = releaseDateTo;
    }

    public Integer getMaxMovies() {
        return maxMovies;
    }

    public void setMaxMovies(Integer maxMovies) {
        this.maxMovies = maxMovies;
    }

    public static TmdbBulkSyncRequest futureDefault() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        TmdbBulkSyncRequest request = new TmdbBulkSyncRequest();
        request.setScope(TmdbSyncScope.FUTURE);
        request.setReleaseDateFrom(today.plusDays(1));
        request.setReleaseDateTo(today.plusYears(1));
        request.setMaxMovies(500);
        return request;
    }
}
