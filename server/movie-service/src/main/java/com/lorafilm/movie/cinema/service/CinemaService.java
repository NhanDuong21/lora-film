package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.common.dto.PageResponse;

public interface CinemaService {
    PageResponse<CinemaDto> getCinemas(String city, String district, String keyword, int page, int size);
    CinemaDto getCinemaBySlug(String slug);
}
