package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieSuggestionDto;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TmdbMovieSearchService {

    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185";

    private final TmdbClient tmdbClient;
    private final ObjectMapper objectMapper;
    private final MovieRepository movieRepository;

    public TmdbMovieSearchService(
            TmdbClient tmdbClient,
            ObjectMapper objectMapper,
            MovieRepository movieRepository) {
        this.tmdbClient = tmdbClient;
        this.objectMapper = objectMapper;
        this.movieRepository = movieRepository;
    }

    public List<TmdbMovieSuggestionDto> search(String rawQuery, int requestedLimit) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 2) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Vui lòng nhập ít nhất 2 ký tự để tìm phim.");
        }
        if (query.length() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Tên phim cần tìm không được dài quá 100 ký tự.");
        }
        int limit = Math.max(1, Math.min(requestedLimit, 10));

        try {
            JsonNode root = objectMapper.readTree(tmdbClient.searchMovies(query, limit));
            JsonNode resultNodes = findResultNodes(root);
            Map<Long, Candidate> candidates = new LinkedHashMap<>();
            if (resultNodes != null && resultNodes.isArray()) {
                for (JsonNode node : resultNodes) {
                    Candidate candidate = toCandidate(node);
                    if (candidate != null) {
                        candidates.putIfAbsent(candidate.tmdbId(), candidate);
                    }
                    if (candidates.size() >= limit) {
                        break;
                    }
                }
            }

            if (candidates.isEmpty()) {
                return List.of();
            }

            Map<Long, Movie> localMovies = movieRepository.findByTmdbIdIn(List.copyOf(candidates.keySet()))
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(Movie::getTmdbId, movie -> movie));

            return candidates.values().stream()
                    .map(candidate -> toSuggestion(candidate, localMovies.get(candidate.tmdbId())))
                    .toList();
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorCode.TMDB_PROVIDER_UNAVAILABLE,
                        "Nguồn TMDB chưa hỗ trợ tìm kiếm phim theo tên. Vui lòng cập nhật API tìm kiếm ở dịch vụ TMDB riêng.");
            }
            throw new BusinessException(ErrorCode.TMDB_PROVIDER_UNAVAILABLE,
                    "Không thể tìm kiếm phim trên nguồn TMDB lúc này. Vui lòng thử lại sau.");
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.TMDB_PROVIDER_UNAVAILABLE,
                    "Không thể tìm kiếm phim trên nguồn TMDB lúc này. Vui lòng thử lại sau.");
        }
    }

    private JsonNode findResultNodes(JsonNode root) {
        if (root == null || root.isNull()) return null;
        if (root.isArray()) return root;
        if (root.has("results")) return root.get("results");
        if (root.has("movies")) return root.get("movies");
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) return null;
        if (data.isArray()) return data;
        if (data.has("results")) return data.get("results");
        return data.get("movies");
    }

    private Candidate toCandidate(JsonNode node) {
        if (node == null || node.isNull()) return null;
        Long tmdbId = positiveLong(node, "tmdbId");
        if (tmdbId == null) tmdbId = positiveLong(node, "id");
        String title = text(node, "title");
        if (tmdbId == null || title == null) return null;
        return new Candidate(
                tmdbId,
                title,
                text(node, "originalTitle", "original_title"),
                parseDate(text(node, "releaseDate", "release_date")),
                posterUrl(text(node, "posterUrl", "posterPath", "poster_path")),
                text(node, "overview"));
    }

    private TmdbMovieSuggestionDto toSuggestion(Candidate candidate, Movie localMovie) {
        return new TmdbMovieSuggestionDto(
                candidate.tmdbId(), candidate.title(), candidate.originalTitle(),
                candidate.originalReleaseDate(), candidate.posterUrl(), candidate.overview(),
                localMovie != null,
                localMovie == null ? null : localMovie.getPublicId(),
                localMovie == null || localMovie.getStatus() == null ? null : localMovie.getStatus().name());
    }

    private Long positiveLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong() || value.asLong() <= 0) return null;
        return value.asLong();
    }

    private String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String posterUrl(String value) {
        if (value == null) return null;
        return value.startsWith("/") ? TMDB_IMAGE_BASE_URL + value : value;
    }

    private record Candidate(
            Long tmdbId,
            String title,
            String originalTitle,
            LocalDate originalReleaseDate,
            String posterUrl,
            String overview) {
    }
}
