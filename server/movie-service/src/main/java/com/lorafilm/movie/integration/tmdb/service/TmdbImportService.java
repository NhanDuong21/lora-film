package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.integration.tmdb.mapper.TmdbMovieMapper;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TmdbImportService {

    private static final Logger log = LoggerFactory.getLogger(TmdbImportService.class);
    private static final String SYNC_TYPE_BULK = "TMDB_BULK_EXPORT";

    private final TmdbClient tmdbClient;
    private final TmdbProperties properties;
    private final MovieRepository movieRepository;
    private final TmdbMovieMapper movieMapper;
    private final TmdbSyncStateRepository syncStateRepository;
    private final ObjectMapper objectMapper;

    public TmdbImportService(TmdbClient tmdbClient, TmdbProperties properties,
                             MovieRepository movieRepository, TmdbMovieMapper movieMapper,
                             TmdbSyncStateRepository syncStateRepository, ObjectMapper objectMapper) {
        this.tmdbClient = tmdbClient;
        this.properties = properties;
        this.movieRepository = movieRepository;
        this.movieMapper = movieMapper;
        this.syncStateRepository = syncStateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Scenario 1: Bulk Export loop
     */
    public void runBulkSync() {
        if (!properties.isSyncEnabled()) {
            log.info("TMDB Bulk Sync is disabled in properties.");
            return;
        }
        
        TmdbSyncState syncState = syncStateRepository.findBySyncType(SYNC_TYPE_BULK)
                .orElseGet(() -> {
                    TmdbSyncState state = new TmdbSyncState();
                    state.setSyncType(SYNC_TYPE_BULK);
                    state.setStatus("IDLE");
                    state.setCursor("0");
                    return syncStateRepository.save(state);
                });

        if ("IN_PROGRESS".equals(syncState.getStatus())) {
            log.warn("TMDB Bulk sync is already in progress. Skipping.");
            return;
        }

        try {
            syncState.setStatus("IN_PROGRESS");
            syncStateRepository.save(syncState);
            
            boolean hasMore = true;
            String currentCursor = syncState.getCursor();
            
            while (hasMore) {
                log.info("Fetching TMDB export with cursor {}", currentCursor);
                String responseBody = tmdbClient.fetchMoviesExport(currentCursor, properties.getBatchSize());
                TmdbMovieResponse response = objectMapper.readValue(responseBody, TmdbMovieResponse.class);
                
                if (response != null && response.getMovies() != null) {
                    for (TmdbMovieWrapperDto dto : response.getMovies()) {
                        try {
                            importMovie(dto);
                        } catch (Exception e) {
                            log.error("Failed to import bulk movie TMDB ID {}: {}", dto.getTmdbId(), e.getMessage());
                        }
                    }
                    currentCursor = response.getNextCursor();
                    hasMore = Boolean.TRUE.equals(response.getHasMore());
                    
                    syncState.setCursor(currentCursor);
                    syncState.setLastSyncTime(LocalDateTime.now());
                    syncStateRepository.save(syncState);
                } else {
                    hasMore = false;
                }
            }

            syncState.setStatus("COMPLETED");
            syncStateRepository.save(syncState);
            log.info("TMDB Bulk Sync completed successfully.");
        } catch (Exception e) {
            log.error("Error during TMDB Bulk sync process", e);
            syncState.setStatus("FAILED");
            syncStateRepository.save(syncState);
        }
    }

    /**
     * Scenario 2a: Daily Sync Latest
     */
    public void runDailyLatestSync() {
        try {
            String responseBody = tmdbClient.fetchLatestMovies();
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("movies")) {
                List<TmdbMovieWrapperDto> movies = objectMapper.readValue(
                    root.get("movies").toString(), 
                    new TypeReference<List<TmdbMovieWrapperDto>>(){}
                );
                for (TmdbMovieWrapperDto dto : movies) {
                    try {
                        importMovie(dto);
                    } catch (Exception e) {
                        log.error("Failed to import latest movie TMDB ID {}: {}", dto.getTmdbId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during TMDB Daily Latest sync", e);
        }
    }

    /**
     * Scenario 2b: Daily Sync Updated
     */
    public void runDailyUpdatedSync() {
        try {
            String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String responseBody = tmdbClient.fetchUpdatedMovies(yesterday);
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("movies")) {
                List<TmdbMovieWrapperDto> movies = objectMapper.readValue(
                    root.get("movies").toString(), 
                    new TypeReference<List<TmdbMovieWrapperDto>>(){}
                );
                for (TmdbMovieWrapperDto dto : movies) {
                    try {
                        importMovie(dto);
                    } catch (Exception e) {
                        log.error("Failed to import updated movie TMDB ID {}: {}", dto.getTmdbId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during TMDB Daily Updated sync", e);
        }
    }

    /**
     * Scenario 3: Sync Single Movie By ID
     */
    public void importMovieById(Long tmdbId) {
        try {
            String responseBody = tmdbClient.fetchMovieDetails(tmdbId);
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("success") && !root.get("success").asBoolean()) {
                throw new RuntimeException("TMDB Error: " + root.path("message").asText());
            }
            if (root.has("data")) {
                TmdbMovieWrapperDto dto = objectMapper.readValue(
                    root.get("data").toString(), 
                    TmdbMovieWrapperDto.class
                );
                importMovie(dto);
            }
        } catch (Exception e) {
            log.error("Error importing single movie {}: {}", tmdbId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importMovie(TmdbMovieWrapperDto wrapper) {
        if (wrapper == null) {
            log.warn("Wrapper is null");
            return;
        }
        if (wrapper.getMovie() == null) {
            log.warn("Wrapper movie is null for TMDB ID: {}", wrapper.getTmdbId());
            return;
        }
        
        // Quality check based on guide
        if (!"ACCEPT".equalsIgnoreCase(wrapper.getQualityStatus())) {
            log.info("SKIP TMDB ID {}: Quality Status is {}", wrapper.getTmdbId(), wrapper.getQualityStatus());
            return;
        }

        Movie existingMovie = movieRepository.findByTmdbId(wrapper.getTmdbId()).orElse(null);

        if (existingMovie == null) {
            // INSERT flow
            log.info("Attempting to insert TMDB ID: {}", wrapper.getTmdbId());
            Movie newMovie = movieMapper.toEntity(wrapper);
            movieRepository.save(newMovie);
            log.info("INSERTED TMDB Movie ID {}", wrapper.getTmdbId());
            // TODO: Extract and save Genres, Credits, Media, etc.
        } else {
            // UPDATE flow with timestamp check
            if (existingMovie.getTmdbLastUpdated() == null || 
               (wrapper.getLastUpdated() != null && wrapper.getLastUpdated().isAfter(existingMovie.getTmdbLastUpdated()))) {
                
                log.info("Attempting to update TMDB ID: {}", wrapper.getTmdbId());
                movieMapper.updateEntityFromDto(wrapper, existingMovie);
                movieRepository.save(existingMovie);
                log.info("UPDATED TMDB Movie ID {}", wrapper.getTmdbId());
                // TODO: Update relations
            } else {
                log.debug("SKIP TMDB ID {}: Movie data is up to date.", wrapper.getTmdbId());
            }
        }
    }
}
