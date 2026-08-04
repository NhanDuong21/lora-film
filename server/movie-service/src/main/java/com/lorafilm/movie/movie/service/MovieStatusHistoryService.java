package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieStatusHistory;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieStatusHistoryResponse;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class MovieStatusHistoryService {
    private final MovieStatusHistoryRepository historyRepository;
    private final MovieRepository movieRepository;
    private final Clock clock;

    public MovieStatusHistoryService(MovieStatusHistoryRepository historyRepository,
                                     MovieRepository movieRepository,
                                     Clock clock) {
        this.historyRepository = historyRepository;
        this.movieRepository = movieRepository;
        this.clock = clock;
    }

    public void record(Movie movie, MovieStatus previousStatus, MovieStatus newStatus,
                       String reason, Long changedBy) {
        MovieStatusHistory history = new MovieStatusHistory();
        history.setMovie(movie);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setReason(normalize(reason));
        history.setChangedAt(Instant.now(clock));
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<MovieStatusHistoryResponse> getHistory(String moviePublicId) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
        return historyRepository.findByMovieIdOrderByChangedAtDescIdDesc(movie.getId()).stream()
                .map(item -> new MovieStatusHistoryResponse(
                        item.getPreviousStatus(), item.getNewStatus(), item.getReason(),
                        item.getChangedAt(), item.getChangedBy()))
                .toList();
    }

    private String normalize(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String normalized = reason.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
