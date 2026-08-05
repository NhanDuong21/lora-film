package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.dto.CreateMovieVersionRequest;
import com.lorafilm.movie.movie.dto.MovieVersionResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieVersionRequest;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MovieVersionServiceImpl implements MovieVersionService {

    private final MovieVersionRepository movieVersionRepository;
    private final MovieRepository movieRepository;
    private final CurrentUserProvider currentUserProvider;
    private final MovieOperationalGuard operationalGuard;

    public MovieVersionServiceImpl(MovieVersionRepository movieVersionRepository,
                                   MovieRepository movieRepository,
                                   CurrentUserProvider currentUserProvider,
                                   MovieOperationalGuard operationalGuard) {
        this.movieVersionRepository = movieVersionRepository;
        this.movieRepository = movieRepository;
        this.currentUserProvider = currentUserProvider;
        this.operationalGuard = operationalGuard;
    }

    @Override
    @Transactional
    public MovieVersionResponse createVersion(String moviePublicId, CreateMovieVersionRequest request) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, ErrorCode.MOVIE_NOT_FOUND.getMessage()));

        String audioLanguage = normalizeLanguage(request.getAudioLanguage());
        String subtitleLanguage = normalizeLanguage(request.getSubtitleLanguage());
        String dubLanguage = normalizeLanguage(request.getDubLanguage());

        boolean exists = movieVersionRepository.existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguage(
                movie.getId(),
                request.getFormat(),
                audioLanguage,
                subtitleLanguage,
                dubLanguage
        );

        if (exists) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_DUPLICATED, ErrorCode.MOVIE_VERSION_DUPLICATED.getMessage());
        }

        MovieVersion version = new MovieVersion();
        version.setPublicId(UUID.randomUUID().toString());
        version.setMovie(movie);
        version.setVersionName(request.getVersionName().trim());
        version.setFormat(request.getFormat());
        version.setAudioLanguage(audioLanguage);
        version.setSubtitleLanguage(subtitleLanguage);
        version.setDubLanguage(dubLanguage);
        version.setStatus(request.getStatus() != null ? request.getStatus() : ActiveStatus.ACTIVE);

        MovieVersion savedVersion = movieVersionRepository.save(version);
        return mapToResponse(savedVersion);
    }

    @Override
    @Transactional
    public MovieVersionResponse updateVersion(String versionPublicId, UpdateMovieVersionRequest request) {
        MovieVersion version = movieVersionRepository.findByPublicIdAndDeletedAtIsNull(versionPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_VERSION_NOT_FOUND, ErrorCode.MOVIE_VERSION_NOT_FOUND.getMessage()));

        operationalGuard.assertVersionEditable(version);

        String audioLanguage = normalizeLanguage(request.getAudioLanguage());
        String subtitleLanguage = normalizeLanguage(request.getSubtitleLanguage());
        String dubLanguage = normalizeLanguage(request.getDubLanguage());

        boolean exists = movieVersionRepository.existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguageAndIdNot(
                version.getMovie().getId(),
                request.getFormat(),
                audioLanguage,
                subtitleLanguage,
                dubLanguage,
                version.getId()
        );

        if (exists) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_DUPLICATED, ErrorCode.MOVIE_VERSION_DUPLICATED.getMessage());
        }

        version.setVersionName(request.getVersionName().trim());
        version.setFormat(request.getFormat());
        version.setAudioLanguage(audioLanguage);
        version.setSubtitleLanguage(subtitleLanguage);
        version.setDubLanguage(dubLanguage);
        version.setStatus(request.getStatus());

        MovieVersion updatedVersion = movieVersionRepository.save(version);
        return mapToResponse(updatedVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieVersionResponse> getActiveVersionsByMovie(String moviePublicIdOrSlug) {
        Optional<Movie> movieOpt = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicIdOrSlug);
        if (movieOpt.isEmpty()) {
            movieOpt = movieRepository.findBySlugAndDeletedAtIsNull(moviePublicIdOrSlug);
        }
        Movie movie = movieOpt.orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, ErrorCode.MOVIE_NOT_FOUND.getMessage()));

        List<MovieVersion> versions = movieVersionRepository.findByMovieIdAndStatusAndDeletedAtIsNull(movie.getId(), ActiveStatus.ACTIVE);
        List<MovieVersionResponse> responses = new ArrayList<>();
        for (MovieVersion v : versions) {
            responses.add(mapToResponse(v));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieVersionResponse> getAllVersionsByMovie(String moviePublicId) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, ErrorCode.MOVIE_NOT_FOUND.getMessage()));

        List<MovieVersion> versions = movieVersionRepository.findByMovieIdAndDeletedAtIsNull(movie.getId());
        List<MovieVersionResponse> responses = new ArrayList<>();
        for (MovieVersion v : versions) {
            responses.add(mapToResponse(v));
        }
        return responses;
    }

    private String normalizeLanguage(String lang) {
        if (lang == null) {
            return null;
        }
        String trimmed = lang.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public MovieVersionResponse getVersion(String versionPublicId) {
        MovieVersion version = movieVersionRepository.findByPublicIdAndDeletedAtIsNull(versionPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_VERSION_NOT_FOUND, ErrorCode.MOVIE_VERSION_NOT_FOUND.getMessage()));
        return mapToResponse(version);
    }

    @Override
    @Transactional
    public void deleteVersion(String versionPublicId) {
        MovieVersion version = movieVersionRepository.findByPublicIdAndDeletedAtIsNull(versionPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_VERSION_NOT_FOUND, ErrorCode.MOVIE_VERSION_NOT_FOUND.getMessage()));

        operationalGuard.assertVersionEditable(version);

        version.performSoftDelete(currentUserProvider.getCurrentUserId());
        movieVersionRepository.save(version);
    }

    private MovieVersionResponse mapToResponse(MovieVersion version) {
        return new MovieVersionResponse(
                version.getPublicId(),
                version.getVersionName(),
                version.getFormat(),
                version.getAudioLanguage(),
                version.getSubtitleLanguage(),
                version.getDubLanguage(),
                version.getStatus(),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }
}
