package com.lorafilm.movie.movie.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.MovieCredit;
import com.lorafilm.movie.movie.domain.entity.MovieProductionCompany;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieDetailDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieSpecification;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final MovieProductionCompanyRepository movieProductionCompanyRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final MovieMapper movieMapper;

    public MovieServiceImpl(MovieRepository movieRepository,
            MovieGenreRepository movieGenreRepository,
            MovieMediaRepository movieMediaRepository,
            MovieCreditRepository movieCreditRepository,
            MovieProductionCompanyRepository movieProductionCompanyRepository,
            MovieVersionRepository movieVersionRepository,
            MovieMapper movieMapper) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.movieCreditRepository = movieCreditRepository;
        this.movieProductionCompanyRepository = movieProductionCompanyRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.movieMapper = movieMapper;
    }

    @Override
    public PageResponse<MovieDto> getMovies(String status, Long genreId, String keyword, String city, Long cinemaId, java.time.LocalDate date, int page, int size, String sort) {
        Specification<Movie> spec = Specification.where(MovieSpecification.isNotDeleted());

        if (status != null && !status.isEmpty()) {
            try {
                MovieStatus parsedStatus = MovieStatus.valueOf(status.toUpperCase());
                spec = spec.and(MovieSpecification.hasStatus(parsedStatus));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        } else {
            spec = spec.and(MovieSpecification.isPubliclyVisible());
        }

        if (genreId != null) {
            spec = spec.and(MovieSpecification.hasGenreId(genreId));
        }

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and(MovieSpecification.hasKeyword(keyword));
        }

        if (city != null && !city.isEmpty()) {
            spec = spec.and(MovieSpecification.hasShowtimeInCity(city));
        }

        if (cinemaId != null) {
            spec = spec.and(MovieSpecification.hasShowtimeInCinema(cinemaId));
        }

        if (date != null) {
            spec = spec.and(MovieSpecification.hasShowtimeOnDate(date));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("releaseDate").descending());
        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        List<Long> movieIds = moviePage.getContent().stream()
                .map(Movie::getId)
                .collect(Collectors.toList());

        List<MovieGenre> allGenres = movieIds.isEmpty() ? List.of() : movieGenreRepository.findByMovieIdIn(movieIds);
        List<MovieMedia> allPrimaryPosters = movieIds.isEmpty() ? List.of() : movieMediaRepository.findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(movieIds, MovieMediaType.POSTER, ActiveStatus.ACTIVE);

        Map<Long, List<String>> genresMap = allGenres.stream()
                .collect(Collectors.groupingBy(
                        mg -> mg.getMovie().getId(),
                        Collectors.mapping(mg -> mg.getGenre().getName(), Collectors.toList())
                ));

        Map<Long, String> postersMap = allPrimaryPosters.stream()
                .collect(Collectors.toMap(
                        mm -> mm.getMovie().getId(),
                        MovieMedia::getUrl,
                        (url1, url2) -> url1
                ));

        List<MovieDto> movieDtos = moviePage.getContent().stream().map(movie -> {
            List<String> genres = genresMap.getOrDefault(movie.getId(), List.of());
            String posterUrl = postersMap.get(movie.getId());
            return movieMapper.toDto(movie, genres, posterUrl);
        }).collect(Collectors.toList());

        return new PageResponse<>(
                movieDtos,
                moviePage.getNumber(),
                moviePage.getSize(),
                moviePage.getTotalElements(),
                moviePage.getTotalPages(),
                moviePage.isLast());
    }

    @Override
    public MovieDetailDto getMovieBySlug(String slug) {
        Movie movie = movieRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        if (movie.getStatus() == MovieStatus.DRAFT || movie.getStatus() == MovieStatus.INACTIVE) {
            throw new ResourceNotFoundException("Movie not found");
        }

        return mapToDetailDto(movie);
    }

    private MovieDto mapToDto(Movie movie) {
        List<String> genres = movieGenreRepository.findByMovieId(movie.getId())
                .stream()
                .map(mg -> mg.getGenre().getName())
                .collect(Collectors.toList());

        Optional<MovieMedia> primaryPoster = movieMediaRepository
                .findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        movie.getId(), MovieMediaType.POSTER, ActiveStatus.ACTIVE);

        String posterUrl = primaryPoster.map(MovieMedia::getUrl).orElse(null);

        return movieMapper.toDto(movie, genres, posterUrl);
    }

    private MovieDetailDto mapToDetailDto(Movie movie) {
        MovieDto baseDto = mapToDto(movie);
        MovieDetailDto detailDto = new MovieDetailDto();
        
        detailDto.setPublicId(baseDto.getPublicId());
        detailDto.setSlug(baseDto.getSlug());
        detailDto.setTitle(baseDto.getTitle());
        detailDto.setOriginalTitle(baseDto.getOriginalTitle());
        detailDto.setSynopsis(baseDto.getSynopsis());
        detailDto.setDurationMinutes(baseDto.getDurationMinutes());
        detailDto.setAgeRating(baseDto.getAgeRating());
        detailDto.setReleaseDate(baseDto.getReleaseDate());
        detailDto.setEndDate(baseDto.getEndDate());
        detailDto.setGenres(baseDto.getGenres());
        detailDto.setPrimaryPoster(baseDto.getPrimaryPoster());
        detailDto.setStatus(baseDto.getStatus());

        List<MovieCredit> credits = movieCreditRepository.findByMovieIdAndDeletedAtIsNullOrderByDisplayOrderAsc(movie.getId());
        
        List<MovieDetailDto.PersonDto> directors = credits.stream()
            .filter(c -> c.getRoleType() == com.lorafilm.movie.movie.domain.enums.CreditRoleType.DIRECTOR)
            .map(c -> {
                MovieDetailDto.PersonDto p = new MovieDetailDto.PersonDto();
                p.setPublicId(c.getPerson().getPublicId());
                p.setFullName(c.getPerson().getFullName());
                p.setRoleType(c.getRoleType().name());
                p.setCharacterName(c.getCharacterName());
                return p;
            }).collect(Collectors.toList());

        List<MovieDetailDto.PersonDto> actors = credits.stream()
            .filter(c -> c.getRoleType() == com.lorafilm.movie.movie.domain.enums.CreditRoleType.MAIN_ACTOR ||
                         c.getRoleType() == com.lorafilm.movie.movie.domain.enums.CreditRoleType.SUPPORTING_ACTOR ||
                         c.getRoleType() == com.lorafilm.movie.movie.domain.enums.CreditRoleType.VOICE_ACTOR)
            .map(c -> {
                MovieDetailDto.PersonDto p = new MovieDetailDto.PersonDto();
                p.setPublicId(c.getPerson().getPublicId());
                p.setFullName(c.getPerson().getFullName());
                p.setRoleType(c.getRoleType().name());
                p.setCharacterName(c.getCharacterName());
                return p;
            }).collect(Collectors.toList());

        detailDto.setDirectors(directors);
        detailDto.setActors(actors);

        List<MovieProductionCompany> companies = movieProductionCompanyRepository.findByMovieId(movie.getId());
        List<MovieDetailDto.ProductionCompanyDto> companyDtos = companies.stream().map(c -> {
            MovieDetailDto.ProductionCompanyDto p = new MovieDetailDto.ProductionCompanyDto();
            p.setPublicId(c.getProductionCompany().getPublicId());
            p.setName(c.getProductionCompany().getName());
            p.setRole(c.getRole() != null ? c.getRole().name() : null);
            return p;
        }).collect(Collectors.toList());
        detailDto.setProductionCompanies(companyDtos);

        List<MovieVersion> versions = movieVersionRepository.findByMovieIdAndStatusAndDeletedAtIsNull(movie.getId(), ActiveStatus.ACTIVE);
        List<MovieDetailDto.MovieVersionDto> versionDtos = versions.stream().map(v -> {
            MovieDetailDto.MovieVersionDto d = new MovieDetailDto.MovieVersionDto();
            d.setPublicId(v.getPublicId());
            d.setVersionName(v.getVersionName());
            d.setFormat(v.getFormat() != null ? v.getFormat().name() : null);
            d.setAudioLanguage(v.getAudioLanguage());
            d.setSubtitleLanguage(v.getSubtitleLanguage());
            d.setDubLanguage(v.getDubLanguage());
            return d;
        }).collect(Collectors.toList());
        detailDto.setVersions(versionDtos);

        List<MovieMedia> media = movieMediaRepository.findByMovieIdAndStatusAndDeletedAtIsNull(movie.getId(), ActiveStatus.ACTIVE);
        List<MovieDetailDto.MovieMediaDto> mediaDtos = media.stream().map(m -> {
            MovieDetailDto.MovieMediaDto d = new MovieDetailDto.MovieMediaDto();
            d.setPublicId(m.getPublicId());
            d.setMediaType(m.getMediaType().name());
            d.setUrl(m.getUrl());
            d.setTitle(m.getTitle());
            d.setIsPrimary(m.getIsPrimary());
            return d;
        }).collect(Collectors.toList());
        detailDto.setMedia(mediaDtos);

        return detailDto;
    }
}
