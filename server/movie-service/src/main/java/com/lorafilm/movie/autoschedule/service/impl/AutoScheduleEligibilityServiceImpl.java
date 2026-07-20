package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.dto.response.EligibilityReason;
import com.lorafilm.movie.autoschedule.dto.response.EligibleMovieResponse;
import com.lorafilm.movie.autoschedule.service.AutoScheduleEligibilityService;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieVersionResponse;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AutoScheduleEligibilityServiceImpl implements AutoScheduleEligibilityService {

    private final MovieRepository movieRepository;
    private final MovieVersionRepository movieVersionRepository;

    public AutoScheduleEligibilityServiceImpl(MovieRepository movieRepository, MovieVersionRepository movieVersionRepository) {
        this.movieRepository = movieRepository;
        this.movieVersionRepository = movieVersionRepository;
    }

    @Override
    public List<EligibleMovieResponse> getEligibleMovies(LocalDate fromDate, LocalDate toDate) {
        List<Movie> allMovies = movieRepository.findAll();
        
        return allMovies.stream()
                .filter(movie -> movie.getDeletedAt() == null)
                .map(movie -> evaluateMovie(movie, fromDate, toDate))
                .collect(Collectors.toList());
    }

    private EligibleMovieResponse evaluateMovie(Movie movie, LocalDate fromDate, LocalDate toDate) {
        EligibleMovieResponse response = new EligibleMovieResponse();
        response.setMoviePublicId(movie.getPublicId());
        response.setTitle(movie.getTitle());
        response.setOriginalTitle(movie.getOriginalTitle());
        response.setSlug(movie.getSlug());
        response.setDurationMinutes(movie.getDurationMinutes());
        response.setReleaseDate(movie.getReleaseDate());
        response.setEndDate(movie.getEndDate());
        response.setStatus(movie.getStatus());

        List<EligibilityReason> reasons = new ArrayList<>();

        if (movie.getStatus() != MovieStatus.NOW_SHOWING && movie.getStatus() != MovieStatus.UPCOMING) {
            reasons.add(new EligibilityReason("MOVIE_STATUS_NOT_ELIGIBLE", "Movie must be NOW_SHOWING or UPCOMING"));
        }

        if (movie.getDurationMinutes() == null || movie.getDurationMinutes() <= 10) {
            reasons.add(new EligibilityReason("MOVIE_DURATION_INVALID", "Movie duration is invalid"));
        }

        List<MovieVersion> versions = movieVersionRepository.findByMovieIdAndDeletedAtIsNull(movie.getId());
        List<MovieVersion> activeVersions = versions.stream()
                .filter(v -> v.getStatus() == ActiveStatus.ACTIVE)
                .collect(Collectors.toList());

        if (activeVersions.isEmpty()) {
            reasons.add(new EligibilityReason("NO_ACTIVE_MOVIE_VERSION", "Movie has no active version"));
        }

        if (fromDate != null && toDate != null) {
            if (movie.getReleaseDate() != null && movie.getReleaseDate().isAfter(toDate)) {
                reasons.add(new EligibilityReason("OUTSIDE_RELEASE_WINDOW", "Schedule range is before movie release date"));
            }
            if (movie.getEndDate() != null && movie.getEndDate().isBefore(fromDate)) {
                reasons.add(new EligibilityReason("OUTSIDE_RELEASE_WINDOW", "Schedule range is after movie end date"));
            }
        }

        response.setReasons(reasons);
        response.setEligible(reasons.isEmpty());

        List<MovieVersionResponse> versionResponses = versions.stream()
                .map(v -> {
                    MovieVersionResponse vr = new MovieVersionResponse();
                    vr.setPublicId(v.getPublicId());
                    vr.setVersionName(v.getVersionName());
                    vr.setFormat(v.getFormat());
                    vr.setAudioLanguage(v.getAudioLanguage());
                    vr.setSubtitleLanguage(v.getSubtitleLanguage());
                    vr.setDubLanguage(v.getDubLanguage());
                    vr.setStatus(v.getStatus());
                    return vr;
                })
                .collect(Collectors.toList());
        response.setVersions(versionResponses);

        return response;
    }
}
