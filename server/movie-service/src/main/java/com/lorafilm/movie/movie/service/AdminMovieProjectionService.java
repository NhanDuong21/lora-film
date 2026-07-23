package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.dto.MovieReadinessDto;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminMovieProjectionService {

    private final MovieVersionRepository movieVersionRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieMapper movieMapper;
    private final MovieReadinessEvaluator readinessEvaluator;

    public AdminMovieProjectionService(
            MovieVersionRepository movieVersionRepository,
            MovieMediaRepository movieMediaRepository,
            ShowtimeRepository showtimeRepository,
            MovieGenreRepository movieGenreRepository,
            MovieMapper movieMapper,
            MovieReadinessEvaluator readinessEvaluator) {
        this.movieVersionRepository = movieVersionRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.showtimeRepository = showtimeRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMapper = movieMapper;
        this.readinessEvaluator = readinessEvaluator;
    }

    @Transactional(readOnly = true)
    public PageResponse<MovieDto> enrichMovies(Page<Movie> moviePage) {
        if (moviePage.isEmpty()) {
            return new PageResponse<>(
                    Collections.emptyList(),
                    moviePage.getNumber(),
                    moviePage.getSize(),
                    moviePage.getTotalElements(),
                    moviePage.getTotalPages(),
                    moviePage.isLast()
            );
        }

        List<Long> movieIds = moviePage.stream().map(Movie::getId).collect(Collectors.toList());

        // Batch queries
        Map<Long, Long> activeVersionCounts = parseCounts(movieVersionRepository.countActiveVersionsByMovieIds(movieIds));
        Map<Long, Long> mediaCounts = parseCounts(movieMediaRepository.countMediaByMovieIds(movieIds));
        Map<Long, Long> showtimeCounts = parseCounts(showtimeRepository.countShowtimesByMovieIds(movieIds));
        Map<Long, Long> primaryPosterCounts = parseCounts(movieMediaRepository.countPrimaryPostersByMovieIds(movieIds));

        // Note: For genres, since there's no count needed for readiness, we fetch them via mapping
        // We will do N queries for genres if not batched, let's batch genres as well.
        Map<Long, List<String>> movieGenres = new HashMap<>();
        movieGenreRepository.findByMovieIdIn(movieIds).forEach(mg -> {
            movieGenres.computeIfAbsent(mg.getMovie().getId(), k -> new java.util.ArrayList<>())
                       .add(mg.getGenre().getName());
        });

        Map<Long, String> primaryPosters = new HashMap<>();
        movieMediaRepository.findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                movieIds, 
                com.lorafilm.movie.movie.domain.enums.MovieMediaType.POSTER, 
                com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE
        ).forEach(m -> primaryPosters.put(m.getMovie().getId(), m.getUrl()));

        List<MovieDto> dtos = moviePage.stream().map(movie -> {
            List<String> genres = movieGenres.getOrDefault(movie.getId(), Collections.emptyList());
            String primaryPosterUrl = primaryPosters.get(movie.getId());
            MovieDto dto = movieMapper.toDto(movie, genres, primaryPosterUrl);
            
            // Enrich fields
            dto.setSource(movie.getTmdbId() != null ? "TMDB" : "MANUAL");
            dto.setTmdbId(movie.getTmdbId());
            dto.setTmdbLastUpdated(movie.getTmdbLastUpdated());
            
            long activeVerCount = activeVersionCounts.getOrDefault(movie.getId(), 0L);
            long primPosterCount = primaryPosterCounts.getOrDefault(movie.getId(), 0L);
            
            dto.setActiveVersionCount(activeVerCount);
            dto.setMediaCount(mediaCounts.getOrDefault(movie.getId(), 0L));
            dto.setShowtimeCount(showtimeCounts.getOrDefault(movie.getId(), 0L));
            
            MovieHealthFacts healthFacts = MovieHealthFacts.from(dto, activeVerCount > 0, primPosterCount > 0);
            MovieReadinessDto readiness = readinessEvaluator.evaluate(healthFacts);
            dto.setReadiness(readiness);
            
            return dto;
        }).collect(Collectors.toList());

        return new PageResponse<>(
                dtos,
                moviePage.getNumber(),
                moviePage.getSize(),
                moviePage.getTotalElements(),
                moviePage.getTotalPages(),
                moviePage.isLast()
        );
    }

    private Map<Long, Long> parseCounts(List<Object[]> results) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] result : results) {
            Long movieId = ((Number) result[0]).longValue();
            Long count = ((Number) result[1]).longValue();
            map.put(movieId, count);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public <T extends MovieDto> T enrichMovieDetail(Movie movie, T dto) {
        dto.setSource(movie.getTmdbId() != null ? "TMDB" : "MANUAL");
        dto.setTmdbId(movie.getTmdbId());
        dto.setTmdbLastUpdated(movie.getTmdbLastUpdated());
        
        long activeVerCount = movieVersionRepository.countActiveVersions(movie.getId());
        long primPosterCount = movieMediaRepository.existsPrimaryPoster(movie.getId()) ? 1L : 0L;
        
        dto.setActiveVersionCount(activeVerCount);
        dto.setMediaCount(movieMediaRepository.countMedia(movie.getId())); 
        dto.setShowtimeCount(showtimeRepository.countShowtimes(movie.getId()));
        
        MovieHealthFacts healthFacts = MovieHealthFacts.from(dto, activeVerCount > 0, primPosterCount > 0);
        MovieReadinessDto readiness = readinessEvaluator.evaluate(healthFacts);
        dto.setReadiness(readiness);
        
        return dto;
    }
}
