package com.lorafilm.movie.showtime.dto.response;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.stereotype.Component;

@Component
public class AdminShowtimeMapper {

    public AdminShowtimeResponse toAdminResponse(Showtime showtime) {
        if (showtime == null) return null;

        AdminShowtimeResponse response = new AdminShowtimeResponse();
        response.setShowtimePublicId(showtime.getPublicId());
        response.setStartTime(showtime.getStartTime());
        response.setEndTime(showtime.getEndTime());
        response.setServiceDate(showtime.getServiceDate());
        response.setStatus(showtime.getStatus() != null ? showtime.getStatus().name() : null);
        response.setBookingOpenTime(showtime.getBookingOpenTime());
        response.setBookingCloseTime(showtime.getBookingCloseTime());
        response.setCancellationReason(showtime.getCancellationReason());
        response.setBatchId(showtime.getBatchId());
        response.setSource(showtime.getSource() != null ? showtime.getSource().name() : null);
        response.setCreatedAt(showtime.getCreatedAt());
        response.setUpdatedAt(showtime.getUpdatedAt());
        response.setVersion(showtime.getVersion());

        if (showtime.getMovie() != null) {
            AdminShowtimeResponse.MovieSummary movieSummary = new AdminShowtimeResponse.MovieSummary();
            movieSummary.setPublicId(showtime.getMovie().getPublicId());
            movieSummary.setSlug(showtime.getMovie().getSlug());
            movieSummary.setTitle(showtime.getMovie().getTitle());
            response.setMovie(movieSummary);
        }

        if (showtime.getMovieVersion() != null) {
            AdminShowtimeResponse.MovieVersionSummary versionSummary = new AdminShowtimeResponse.MovieVersionSummary();
            versionSummary.setPublicId(showtime.getMovieVersion().getPublicId());
            versionSummary.setVersionName(showtime.getMovieVersion().getVersionName());
            versionSummary.setFormat(showtime.getMovieVersion().getFormat() != null ? showtime.getMovieVersion().getFormat().getValue() : null);
            versionSummary.setAudioLanguage(showtime.getMovieVersion().getAudioLanguage());
            versionSummary.setSubtitleLanguage(showtime.getMovieVersion().getSubtitleLanguage());
            response.setMovieVersion(versionSummary);
        }

        if (showtime.getCinema() != null) {
            AdminShowtimeResponse.CinemaSummary cinemaSummary = new AdminShowtimeResponse.CinemaSummary();
            cinemaSummary.setPublicId(showtime.getCinema().getPublicId());
            cinemaSummary.setSlug(showtime.getCinema().getSlug());
            cinemaSummary.setName(showtime.getCinema().getName());
            cinemaSummary.setTimezone(showtime.getCinema().getTimezone());
            response.setCinema(cinemaSummary);
        }

        if (showtime.getAuditorium() != null) {
            AdminShowtimeResponse.AuditoriumSummary audSummary = new AdminShowtimeResponse.AuditoriumSummary();
            audSummary.setPublicId(showtime.getAuditorium().getPublicId());
            audSummary.setName(showtime.getAuditorium().getName());
            audSummary.setScreenType(showtime.getAuditorium().getScreenType() != null ? showtime.getAuditorium().getScreenType().getValue() : null);
            audSummary.setSoundType(showtime.getAuditorium().getSoundType() != null ? showtime.getAuditorium().getSoundType().name() : null);
            audSummary.setCleaningBufferMinutes(showtime.getAuditorium().getCleaningBufferMinutes());
            response.setAuditorium(audSummary);
        }

        return response;
    }
}
