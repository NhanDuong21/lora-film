package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;

import java.time.LocalDate;

public interface AdminShowtimeQueryService {
    PageResponse<AdminShowtimeResponse> getAdminShowtimes(
            String cinemaSlug,
            String movieSlug,
            ShowtimeStatus status,
            LocalDate date,
            int page,
            int size
    );

    AdminShowtimeResponse getAdminShowtimeByPublicId(String publicId);
}
