package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieResponse;
import com.lorafilm.movie.integration.tmdb.mapper.TmdbMovieMapper;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TmdbImportService {

    private static final Logger log = LoggerFactory.getLogger(TmdbImportService.class);
    private static final String SYNC_TYPE = "TMDB_MOVIE_SYNC";

    private final TmdbClient tmdbClient;
    private final TmdbProperties properties;
    private final TmdbSyncStateRepository syncStateRepository;
    private final MovieRepository movieRepository;
    private final TmdbMovieMapper tmdbMovieMapper;
    private final ObjectMapper objectMapper;

    public TmdbImportService(TmdbClient tmdbClient, TmdbProperties properties,
                             TmdbSyncStateRepository syncStateRepository,
                             MovieRepository movieRepository,
                             TmdbMovieMapper tmdbMovieMapper,
                             ObjectMapper objectMapper) {
        this.tmdbClient = tmdbClient;
        this.properties = properties;
        this.syncStateRepository = syncStateRepository;
        this.movieRepository = movieRepository;
        this.tmdbMovieMapper = tmdbMovieMapper;
        this.objectMapper = objectMapper;
    }

    public void runSync() {
        if (!properties.isSyncEnabled()) {
            log.info("TMDB sync is disabled. Skipping.");
            return;
        }

        TmdbSyncState syncState = syncStateRepository.findBySyncType(SYNC_TYPE)
                .orElseGet(() -> {
                    TmdbSyncState state = new TmdbSyncState();
                    state.setSyncType(SYNC_TYPE);
                    state.setStatus("IDLE");
                    return syncStateRepository.save(state);
                });

        if ("IN_PROGRESS".equals(syncState.getStatus())) {
            log.warn("TMDB sync is already in progress. Skipping.");
            return;
        }

        try {
            syncState.setStatus("IN_PROGRESS");
            syncStateRepository.save(syncState);

            String responseBody = tmdbClient.fetchMovies(syncState.getCursor(), properties.getBatchSize());
            TmdbMovieResponse response = objectMapper.readValue(responseBody, TmdbMovieResponse.class);

            if (response != null && response.getMovies() != null) {
                for (TmdbMovieDto dto : response.getMovies()) {
                    try {
                        importMovie(dto);
                    } catch (Exception e) {
                        log.error("Failed to import movie TMDB ID {}: {}", dto.getId(), e.getMessage());
                    }
                }

                syncState.setCursor(response.getNextCursor());
                syncState.setLastSyncTime(LocalDateTime.now());
            }

            syncState.setStatus("COMPLETED");
        } catch (Exception e) {
            log.error("Error during TMDB sync process", e);
            syncState.setStatus("FAILED");
        } finally {
            syncStateRepository.save(syncState);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importMovie(TmdbMovieDto dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("TMDB ID cannot be null");
        }

        Movie movie = movieRepository.findByTmdbId(dto.getId()).orElse(null);

        if (movie == null) {
            log.info("Inserting new movie: {}", dto.getTitle());
            movie = tmdbMovieMapper.toEntity(dto);
            movieRepository.save(movie);
        } else {
            log.info("Updating existing movie: {}", dto.getTitle());
            tmdbMovieMapper.updateEntityFromDto(dto, movie);
            movieRepository.save(movie);
        }
    }
}
