package com.lorafilm.movie.showtime.dto;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieVersionDto;
import com.lorafilm.movie.showtime.dto.ShowtimeCinemaDto;
import com.lorafilm.movie.showtime.dto.ShowtimeAuditoriumDto;
import org.springframework.stereotype.Component;

@Component
public class ShowtimeMapper {

    public ShowtimeDto toDto(Showtime showtime) {
        if (showtime == null) {
            return null;
        }
        ShowtimeDto dto = new ShowtimeDto();
        dto.setShowtimePublicId(showtime.getPublicId());
        
        ShowtimeMovieDto movieDto = new ShowtimeMovieDto();
        movieDto.setPublicId(showtime.getMovie().getPublicId());
        movieDto.setSlug(showtime.getMovie().getSlug());
        movieDto.setTitle(showtime.getMovie().getTitle());
        dto.setMovie(movieDto);

        ShowtimeMovieVersionDto versionDto = new ShowtimeMovieVersionDto();
        versionDto.setPublicId(showtime.getMovieVersion().getPublicId());
        versionDto.setVersionName(showtime.getMovieVersion().getVersionName());
        versionDto.setFormat(showtime.getMovieVersion().getFormat() != null ? showtime.getMovieVersion().getFormat().name() : null);
        versionDto.setAudioLanguage(showtime.getMovieVersion().getAudioLanguage());
        versionDto.setSubtitleLanguage(showtime.getMovieVersion().getSubtitleLanguage());
        dto.setMovieVersion(versionDto);

        ShowtimeCinemaDto cinemaDto = new ShowtimeCinemaDto();
        cinemaDto.setPublicId(showtime.getCinema().getPublicId());
        cinemaDto.setSlug(showtime.getCinema().getSlug());
        cinemaDto.setName(showtime.getCinema().getName());
        cinemaDto.setTimezone(showtime.getCinema().getTimezone());
        dto.setCinema(cinemaDto);

        ShowtimeAuditoriumDto auditoriumDto = new ShowtimeAuditoriumDto();
        auditoriumDto.setPublicId(showtime.getAuditorium().getPublicId());
        auditoriumDto.setName(showtime.getAuditorium().getName());
        auditoriumDto.setScreenType(showtime.getAuditorium().getScreenType() != null ? showtime.getAuditorium().getScreenType().name() : null);
        auditoriumDto.setSoundType(showtime.getAuditorium().getSoundType() != null ? showtime.getAuditorium().getSoundType().name() : null);
        dto.setAuditorium(auditoriumDto);

        dto.setStartTime(showtime.getStartTime());
        dto.setEndTime(showtime.getEndTime());
        dto.setStatus(showtime.getStatus().name());
        
        return dto;
    }
}
