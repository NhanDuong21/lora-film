package com.lorafilm.movie.showtime.dto;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.stereotype.Component;

@Component
public class ShowtimeMapper {

    public ShowtimeDto toDto(Showtime showtime) {
        if (showtime == null) {
            return null;
        }
        ShowtimeDto dto = new ShowtimeDto();
        dto.setPublicId(showtime.getPublicId());
        dto.setMovieSlug(showtime.getMovie().getSlug());
        dto.setMovieTitle(showtime.getMovie().getTitle());
        dto.setCinemaSlug(showtime.getCinema().getSlug());
        dto.setCinemaName(showtime.getCinema().getName());
        dto.setAuditoriumName(showtime.getAuditorium().getName());
        dto.setMovieVersionName(showtime.getMovieVersion().getVersionName());
        dto.setStartTime(showtime.getStartTime());
        dto.setEndTime(showtime.getEndTime());
        dto.setStatus(showtime.getStatus().name());
        return dto;
    }
}
