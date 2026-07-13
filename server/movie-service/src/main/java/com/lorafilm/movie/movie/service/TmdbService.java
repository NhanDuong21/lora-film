package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.dto.tmdb.TmdbGenreDto;
import com.lorafilm.movie.movie.dto.tmdb.TmdbMovieDetailResponse;
import com.lorafilm.movie.movie.dto.tmdb.TmdbProductionCountryDto;
import com.lorafilm.movie.movie.repository.GenreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TmdbService {

    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private final RestTemplate restTemplate;
    private final AdminMovieService adminMovieService;
    private final GenreRepository genreRepository;

    public TmdbService(AdminMovieService adminMovieService, GenreRepository genreRepository) {
        this.restTemplate = new RestTemplate();
        this.adminMovieService = adminMovieService;
        this.genreRepository = genreRepository;
    }

    public TmdbMovieDetailResponse getMovieDetail(Integer tmdbId) {
        String url = String.format("%s/movie/%d?api_key=%s", tmdbApiUrl, tmdbId, tmdbApiKey);
        try {
            return restTemplate.getForObject(url, TmdbMovieDetailResponse.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phim trên TMDB", null);
        }
    }

    @Transactional
    public MovieDto approveTmdbMovie(Integer tmdbId) {
        TmdbMovieDetailResponse tmdbMovie = getMovieDetail(tmdbId);

        MovieRequest request = new MovieRequest();
        request.setTitle(tmdbMovie.getTitle());
        request.setOriginalTitle(tmdbMovie.getOriginalTitle());
        request.setSynopsis(tmdbMovie.getOverview());
        
        int duration = tmdbMovie.getRuntime() != null ? tmdbMovie.getRuntime() : 0;
        if (duration <= 0) {
            duration = 1;
        }
        request.setDurationMinutes(duration);

        if (Boolean.TRUE.equals(tmdbMovie.getAdult())) {
            request.setAgeRating(AgeRating.T18);
        } else {
            request.setAgeRating(AgeRating.P);
        }

        request.setReleaseDate(tmdbMovie.getReleaseDate() != null ? tmdbMovie.getReleaseDate() : LocalDate.now());
        
        if (tmdbMovie.getProductionCountries() != null && !tmdbMovie.getProductionCountries().isEmpty()) {
            TmdbProductionCountryDto country = tmdbMovie.getProductionCountries().get(0);
            request.setCountry(country.getName());
        }

        MovieDto createdMovie = adminMovieService.createMovie(request);

        if (tmdbMovie.getGenres() != null && !tmdbMovie.getGenres().isEmpty()) {
            List<String> genrePublicIds = new ArrayList<>();
            for (TmdbGenreDto tmdbGenre : tmdbMovie.getGenres()) {
                String slug = generateSlug(tmdbGenre.getName());
                Optional<Genre> existingGenre = genreRepository.findByActiveSlugAndDeletedAtIsNull(slug);
                
                if (existingGenre.isPresent()) {
                    genrePublicIds.add(existingGenre.get().getPublicId());
                } else {
                    Genre newGenre = new Genre();
                    newGenre.setPublicId(UUID.randomUUID().toString());
                    newGenre.setName(tmdbGenre.getName());
                    newGenre.setSlug(slug);
                    Genre savedGenre = genreRepository.save(newGenre);
                    genrePublicIds.add(savedGenre.getPublicId());
                }
            }
            adminMovieService.assignGenres(createdMovie.getPublicId(), genrePublicIds);
        }

        return createdMovie;
    }
    
    private String generateSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return noAccents.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-").replaceAll("-+", "-");
    }
}
