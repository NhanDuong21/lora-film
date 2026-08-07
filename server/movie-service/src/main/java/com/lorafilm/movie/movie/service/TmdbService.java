package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.tmdb.service.TmdbImportService;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Backward-compatible adapter for the former TMDB approve endpoint.
 *
 * <p>Approval is a movie lifecycle transition, not an ingestion operation. This
 * adapter therefore only ensures that the TMDB movie exists as DRAFT through
 * the canonical importer. Callers must use the movie status endpoint for the
 * subsequent DRAFT -> UPCOMING approval.</p>
 */
@Service
@Deprecated(forRemoval = true)
public class TmdbService {

    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);

    private final TmdbImportService tmdbImportService;
    private final MovieRepository movieRepository;
    private final MovieService movieService;

    public TmdbService(
            TmdbImportService tmdbImportService,
            MovieRepository movieRepository,
            MovieService movieService) {
        this.tmdbImportService = tmdbImportService;
        this.movieRepository = movieRepository;
        this.movieService = movieService;
    }

    @Deprecated(forRemoval = true)
    public MovieDto approveTmdbMovie(Integer tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new BusinessException(ErrorCode.TMDB_IMPORT_INVALID_PAYLOAD, "Mã phim TMDB phải là số nguyên dương.");
        }

        log.warn("Deprecated /api/admin/tmdb/approve called for TMDB ID {}; delegating to import-as-DRAFT", tmdbId);
        tmdbImportService.importMovieById(tmdbId.longValue());
        Movie movie = movieRepository.findByTmdbId(tmdbId.longValue())
                .filter(candidate -> candidate.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MOVIE_NOT_FOUND,
                        "TMDB movie was not imported because it is rejected or deleted"));
        return movieService.getMovieByIdentifier(movie.getPublicId());
    }
}
