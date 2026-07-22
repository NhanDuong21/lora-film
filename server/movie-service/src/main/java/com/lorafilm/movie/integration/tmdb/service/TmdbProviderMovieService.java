package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TmdbProviderMovieService {

    private static final Logger log = LoggerFactory.getLogger(TmdbProviderMovieService.class);

    private final TmdbClient tmdbClient;
    private final ObjectMapper objectMapper;

    public TmdbProviderMovieService(TmdbClient tmdbClient, ObjectMapper objectMapper) {
        this.tmdbClient = tmdbClient;
        this.objectMapper = objectMapper;
    }

    public TmdbMovieWrapperDto fetchMovie(Long tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new BusinessException(ErrorCode.TMDB_IMPORT_INVALID_PAYLOAD, "tmdbId must be a positive number");
        }
        try {
            JsonNode root = objectMapper.readTree(tmdbClient.fetchMovieDetails(tmdbId));
            if (root.has("success") && !root.get("success").asBoolean()) {
                throw new BusinessException(
                        ErrorCode.TMDB_PROVIDER_UNAVAILABLE,
                        "TMDB provider rejected the request: " + root.path("message").asText("Unknown provider error"));
            }
            if (!root.hasNonNull("data")) {
                throw new BusinessException(ErrorCode.TMDB_PROVIDER_RESPONSE_INVALID, "TMDB response does not contain movie data");
            }

            TmdbMovieWrapperDto wrapper = objectMapper.treeToValue(root.get("data"), TmdbMovieWrapperDto.class);
            validateIdentity(tmdbId, wrapper);
            return wrapper;
        } catch (BusinessException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            log.error("Invalid TMDB response for movie {}", tmdbId, exception);
            throw new BusinessException(ErrorCode.TMDB_PROVIDER_RESPONSE_INVALID, "Unable to parse TMDB movie response");
        } catch (Exception exception) {
            log.error("TMDB provider request failed for movie {}", tmdbId, exception);
            throw new BusinessException(ErrorCode.TMDB_PROVIDER_UNAVAILABLE, "Unable to fetch TMDB movie");
        }
    }

    public void validateIdentity(Long expectedTmdbId, TmdbMovieWrapperDto wrapper) {
        if (wrapper == null || wrapper.getMovie() == null || wrapper.getTmdbId() == null || wrapper.getTmdbId() <= 0) {
            throw new BusinessException(ErrorCode.TMDB_IMPORT_INVALID_PAYLOAD, "TMDB payload must contain a positive tmdbId and movie data");
        }
        if (expectedTmdbId != null && !expectedTmdbId.equals(wrapper.getTmdbId())) {
            throw new BusinessException(ErrorCode.TMDB_IMPORT_INVALID_PAYLOAD, "TMDB response identity does not match the requested movie");
        }
        Long nestedTmdbId = wrapper.getMovie().getTmdbId();
        if (nestedTmdbId != null && !wrapper.getTmdbId().equals(nestedTmdbId)) {
            throw new BusinessException(ErrorCode.TMDB_IMPORT_INVALID_PAYLOAD, "TMDB wrapper and movie identities do not match");
        }
    }
}
