package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.dto.response.EligibilityReason;
import com.lorafilm.movie.autoschedule.dto.response.EligibleMovieResponse;
import com.lorafilm.movie.autoschedule.service.AutoScheduleEligibilityService;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.dto.MovieVersionResponse;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.validation.MovieShowtimeEligibilityPolicy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AutoScheduleEligibilityServiceImpl implements AutoScheduleEligibilityService {

    private final MovieRepository movieRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final MovieShowtimeEligibilityPolicy eligibilityPolicy;

    public AutoScheduleEligibilityServiceImpl(MovieRepository movieRepository,
                                              MovieVersionRepository movieVersionRepository,
                                              MovieMediaRepository movieMediaRepository,
                                              MovieShowtimeEligibilityPolicy eligibilityPolicy) {
        this.movieRepository = movieRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.eligibilityPolicy = eligibilityPolicy;
    }

    @Override
    public List<EligibleMovieResponse> getEligibleMovies(LocalDate fromDate, LocalDate toDate) {
        List<Movie> movies = movieRepository.findAll().stream()
                .filter(movie -> movie.getDeletedAt() == null)
                .toList();
        Map<Long, MovieMedia> primaryPosters = movies.isEmpty()
                ? Map.of()
                : movieMediaRepository.findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                                movies.stream().map(Movie::getId).toList(),
                                MovieMediaType.POSTER,
                                ActiveStatus.ACTIVE)
                        .stream()
                        .collect(Collectors.toMap(
                                media -> media.getMovie().getId(),
                                Function.identity(),
                                (first, ignored) -> first));

        return movies.stream()
                .map(movie -> evaluateMovie(
                        movie,
                        fromDate,
                        toDate,
                        primaryPosters.containsKey(movie.getId()) ? primaryPosters.get(movie.getId()).getUrl() : null))
                .toList();
    }

    private EligibleMovieResponse evaluateMovie(Movie movie,
                                                LocalDate fromDate,
                                                LocalDate toDate,
                                                String primaryPoster) {
        EligibleMovieResponse response = new EligibleMovieResponse();
        response.setMoviePublicId(movie.getPublicId());
        response.setTitle(movie.getTitle());
        response.setOriginalTitle(movie.getOriginalTitle());
        response.setSlug(movie.getSlug());
        response.setPrimaryPoster(primaryPoster);
        response.setDurationMinutes(movie.getDurationMinutes());
        response.setReleaseDate(movie.getReleaseDate());
        response.setEndDate(movie.getEndDate());
        response.setStatus(movie.getStatus());

        List<MovieVersion> versions = movieVersionRepository.findByMovieIdAndDeletedAtIsNull(movie.getId());
        List<EligibilityReason> reasons = eligibilityPolicy.evaluateRange(movie, versions, fromDate, toDate).stream()
                .map(issue -> new EligibilityReason(issue.code(), issue.message()))
                .collect(Collectors.toList());

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
