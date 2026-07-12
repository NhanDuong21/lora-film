package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.showtime.dto.SeatLayoutDto;
import com.lorafilm.movie.showtime.dto.ShowtimeDto;

import java.time.LocalDate;

public interface ShowtimeQueryService {
    PageResponse<ShowtimeDto> getShowtimes(String movieSlug, String cinemaSlug, String city, LocalDate date, String format, String audioLanguage, String subtitleLanguage, int page, int size);
    ShowtimeDto getShowtimeByPublicId(String publicId);
    SeatLayoutDto getSeatLayout(String publicId);
}
