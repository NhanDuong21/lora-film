package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieSummaryResponse;
import com.lorafilm.movie.movie.repository.MovieHealthSpecifications;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieSpecification;
import com.lorafilm.movie.movie.repository.MovieStatusCountProjection;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
public class MovieSummaryQueryService {

    private final MovieRepository movieRepository;

    public MovieSummaryQueryService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional(readOnly = true)
    public MovieSummaryResponse getSummary() {
        Map<MovieStatus, Long> lifecycleCounts = new EnumMap<>(MovieStatus.class);
        for (MovieStatusCountProjection row : movieRepository.countNonDeletedMoviesByStatus()) {
            lifecycleCounts.put(row.getStatus(), row.getTotal());
        }

        long draft = lifecycleCounts.getOrDefault(MovieStatus.DRAFT, 0L);
        long upcoming = lifecycleCounts.getOrDefault(MovieStatus.UPCOMING, 0L);
        long nowShowing = lifecycleCounts.getOrDefault(MovieStatus.NOW_SHOWING, 0L);
        long ended = lifecycleCounts.getOrDefault(MovieStatus.ENDED, 0L);
        long inactive = lifecycleCounts.getOrDefault(MovieStatus.INACTIVE, 0L);
        long total = draft + upcoming + nowShowing + ended + inactive;

        Specification<Movie> notDeleted = MovieSpecification.isNotDeleted();
        long blocked = movieRepository.count(notDeleted.and(
                MovieHealthSpecifications.healthStatusEquals(MovieHealthStatus.BLOCKED)));
        long warning = movieRepository.count(notDeleted.and(
                MovieHealthSpecifications.healthStatusEquals(MovieHealthStatus.WARNING)));
        long missingPrimaryPoster = movieRepository.count(notDeleted.and(
                Specification.not(MovieHealthSpecifications.hasActivePrimaryPoster())));
        long missingActiveVersion = movieRepository.count(notDeleted.and(
                Specification.not(MovieHealthSpecifications.hasActiveVersion())));
        long withoutShowtime = movieRepository.count(notDeleted.and(MovieSpecification.hasShowtime(false)));
        long ready = total - blocked - warning;

        return new MovieSummaryResponse(
                total,
                draft,
                upcoming,
                nowShowing,
                ended,
                inactive,
                ready,
                warning,
                blocked,
                missingPrimaryPoster,
                missingActiveVersion,
                withoutShowtime);
    }
}
