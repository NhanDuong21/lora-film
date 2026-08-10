package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.CreateMovieMediaRequest;
import com.lorafilm.movie.movie.dto.MovieMediaResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieMediaRequest;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MovieMediaServiceImpl implements MovieMediaService {

    private final MovieMediaRepository movieMediaRepository;
    private final MovieRepository movieRepository;
    private final CurrentUserProvider currentUserProvider;
    private final MovieOperationalGuard operationalGuard;

    public MovieMediaServiceImpl(MovieMediaRepository movieMediaRepository,
                                 MovieRepository movieRepository,
                                 CurrentUserProvider currentUserProvider,
                                 MovieOperationalGuard operationalGuard) {
        this.movieMediaRepository = movieMediaRepository;
        this.movieRepository = movieRepository;
        this.currentUserProvider = currentUserProvider;
        this.operationalGuard = operationalGuard;
    }

    @Override
    @Transactional
    public MovieMediaResponse createMedia(String moviePublicId, CreateMovieMediaRequest request) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));

        if (Boolean.TRUE.equals(request.getIsPrimary()) &&
                request.getMediaType() != MovieMediaType.POSTER &&
                request.getMediaType() != MovieMediaType.BANNER) {
            throw new BusinessException(ErrorCode.MOVIE_PRIMARY_MEDIA_INVALID);
        }

        ActiveStatus targetStatus = request.getStatus() != null
                ? request.getStatus() : ActiveStatus.ACTIVE;
        if (Boolean.TRUE.equals(request.getIsPrimary()) && targetStatus == ActiveStatus.ACTIVE) {
            movieMediaRepository.resetPrimaryMedia(movie.getId(), request.getMediaType());
        }

        MovieMedia media = new MovieMedia();
        media.setPublicId(UUID.randomUUID().toString());
        media.setMovie(movie);
        media.setMediaType(request.getMediaType());
        media.setUrl(request.getUrl().trim());
        media.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        media.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        media.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);
        media.setStatus(targetStatus);

        MovieMedia savedMedia = movieMediaRepository.save(media);
        return mapToResponse(savedMedia);
    }

    @Override
    @Transactional
    public MovieMediaResponse updateMedia(String mediaPublicId, UpdateMovieMediaRequest request) {
        MovieMedia media = movieMediaRepository.findByPublicIdAndDeletedAtIsNull(mediaPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_MEDIA_NOT_FOUND));

        operationalGuard.assertPrimaryPosterPreserved(media, request);

        if (Boolean.TRUE.equals(request.getIsPrimary()) &&
                request.getMediaType() != MovieMediaType.POSTER &&
                request.getMediaType() != MovieMediaType.BANNER) {
            throw new BusinessException(ErrorCode.MOVIE_PRIMARY_MEDIA_INVALID);
        }

        ActiveStatus targetStatus = request.getStatus() != null
                ? request.getStatus() : ActiveStatus.ACTIVE;
        if (Boolean.TRUE.equals(request.getIsPrimary()) && targetStatus == ActiveStatus.ACTIVE) {
            movieMediaRepository.resetPrimaryMedia(media.getMovie().getId(), request.getMediaType());
        }

        media.setMediaType(request.getMediaType());
        media.setUrl(request.getUrl().trim());
        media.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        media.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        media.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);
        media.setStatus(targetStatus);

        MovieMedia savedMedia = movieMediaRepository.save(media);
        return mapToResponse(savedMedia);
    }

    @Override
    @Transactional
    public void deleteMedia(String mediaPublicId) {
        MovieMedia media = movieMediaRepository.findByPublicIdAndDeletedAtIsNull(mediaPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_MEDIA_NOT_FOUND));

        operationalGuard.assertPrimaryPosterCanBeRemoved(media);

        media.performSoftDelete(currentUserProvider.getCurrentUserId());
        movieMediaRepository.save(media);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieMediaResponse> getMovieMedia(String moviePublicId) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));

        List<MovieMedia> mediaList = movieMediaRepository.findByMovieIdAndDeletedAtIsNull(movie.getId());
        List<MovieMediaResponse> responseList = new ArrayList<>();
        for (MovieMedia m : mediaList) {
            responseList.add(mapToResponse(m));
        }
        return responseList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieMediaResponse> getCustomerMedia(String moviePublicIdOrSlug) {
        Optional<Movie> movieOpt = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicIdOrSlug);
        if (movieOpt.isEmpty()) {
            movieOpt = movieRepository.findBySlugAndDeletedAtIsNull(moviePublicIdOrSlug);
        }
        Movie movie = movieOpt.orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));

        if (movie.getStatus() == MovieStatus.DRAFT || movie.getStatus() == MovieStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }

        List<MovieMedia> mediaList = movieMediaRepository.findByMovieIdAndStatusAndDeletedAtIsNull(movie.getId(), ActiveStatus.ACTIVE);
        List<MovieMediaResponse> responseList = new ArrayList<>();
        for (MovieMedia m : mediaList) {
            responseList.add(mapToResponse(m));
        }
        return responseList;
    }

    @Override
    @Transactional(readOnly = true)
    public MovieMediaResponse getMedia(String mediaPublicId) {
        MovieMedia media = movieMediaRepository.findByPublicIdAndDeletedAtIsNull(mediaPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_MEDIA_NOT_FOUND));
        return mapToResponse(media);
    }

    private MovieMediaResponse mapToResponse(MovieMedia media) {
        return new MovieMediaResponse(
                media.getPublicId(),
                media.getMediaType(),
                media.getUrl(),
                media.getTitle(),
                media.getDisplayOrder(),
                media.getIsPrimary(),
                media.getStatus()
        );
    }
}
