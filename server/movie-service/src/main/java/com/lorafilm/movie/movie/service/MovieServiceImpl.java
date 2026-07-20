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
import org.springframework.transaction.annotation.Transactional;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieCredit;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieProductionCompany;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDetailDto;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieSpecification;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;

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
            if (!status.equalsIgnoreCase("ALL")) {
                try {
                    MovieStatus parsedStatus = MovieStatus.valueOf(status.toUpperCase());
                    spec = spec.and(MovieSpecification.hasStatus(parsedStatus));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid status: " + status);
                }
            }
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

        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length >= 2) {
                sorting = sortParams[1].equalsIgnoreCase("desc") ? 
                        Sort.by(sortParams[0]).descending() : 
                        Sort.by(sortParams[0]).ascending();
            } else {
                sorting = Sort.by(sortParams[0]).ascending();
            }
        } else {
            sorting = Sort.by("createdAt").descending();
        }
        Pageable pageable = PageRequest.of(page, size, sorting);
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
    @Transactional(readOnly = true)
    public MovieDetailDto getMovieByIdentifier(String identifier) {
        Optional<Movie> movieOpt = movieRepository.findByPublicIdAndDeletedAtIsNull(identifier);
        if (movieOpt.isEmpty()) {
            movieOpt = movieRepository.findBySlugAndDeletedAtIsNull(identifier);
        }
        Movie movie = movieOpt.orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        return mapToDetailDto(movie);
    }

    @Override
    @Transactional
    public MovieDto updateMovieStatus(String moviePublicId, MovieStatus targetStatus) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));

        if (targetStatus == MovieStatus.UPCOMING || targetStatus == MovieStatus.NOW_SHOWING) {
            validatePublishConditions(movie.getId());
        }

        movie.setStatus(targetStatus);
        Movie savedMovie = movieRepository.save(movie);

        List<String> genres = movieGenreRepository.findByMovieId(savedMovie.getId())
                .stream().map(mg -> mg.getGenre().getName()).collect(Collectors.toList());
        Optional<MovieMedia> primaryPoster = movieMediaRepository
                .findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        savedMovie.getId(), MovieMediaType.POSTER, ActiveStatus.ACTIVE);
        String posterUrl = primaryPoster.map(MovieMedia::getUrl).orElse(null);

        return movieMapper.toDto(savedMovie, genres, posterUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePublishConditions(Long movieId) {
        boolean hasActiveVersion = movieVersionRepository.existsActiveVersion(movieId);
        boolean hasPrimaryPoster = movieMediaRepository.existsPrimaryPoster(movieId);
        boolean hasGenre = !movieGenreRepository.findByMovieId(movieId).isEmpty();
        
        if (!hasGenre) {
            throw new BusinessException(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie must have at least 1 genre to be published");
        }

        if (!hasActiveVersion && !hasPrimaryPoster) {
            throw new BusinessException(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie must have at least one active version and one primary poster to publish");
        }
        if (!hasActiveVersion) {
            throw new BusinessException(ErrorCode.MOVIE_ACTIVE_VERSION_REQUIRED, "Movie must have at least one active version to publish");
        }
        if (!hasPrimaryPoster) {
            throw new BusinessException(ErrorCode.MOVIE_PRIMARY_POSTER_REQUIRED, "Movie must have at least one active primary poster to publish");
        }
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
        detailDto.setActiveSlug(baseDto.getActiveSlug());
        detailDto.setCountry(baseDto.getCountry());

        List<MovieCredit> credits = movieCreditRepository.findByMovieIdAndDeletedAtIsNullOrderByDisplayOrderAsc(movie.getId());
        
        List<MovieDetailDto.PersonDto> directors = credits.stream()
            .filter(c -> c.getRoleType() == com.lorafilm.movie.movie.domain.enums.CreditRoleType.DIRECTOR)
            .map(c -> {
                MovieDetailDto.PersonDto p = new MovieDetailDto.PersonDto();
                p.setPublicId(c.getPerson().getPublicId());
                p.setFullName(c.getPerson().getFullName());
                p.setRoleType(c.getRoleType().name());
                p.setCharacterName(c.getCharacterName());
                p.setProfileImageUrl(c.getPerson().getProfileImageUrl());
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
                p.setProfileImageUrl(c.getPerson().getProfileImageUrl());
                return p;
            }).collect(Collectors.toList());
 
        List<MovieDetailDto.PersonDto> writers = credits.stream()
            .filter(c -> c.getRoleType() == com.lorafilm.movie.movie.domain.enums.CreditRoleType.WRITER)
            .map(c -> {
                MovieDetailDto.PersonDto p = new MovieDetailDto.PersonDto();
                p.setPublicId(c.getPerson().getPublicId());
                p.setFullName(c.getPerson().getFullName());
                p.setRoleType(c.getRoleType().name());
                p.setCharacterName(c.getCharacterName());
                p.setProfileImageUrl(c.getPerson().getProfileImageUrl());
                return p;
            }).collect(Collectors.toList());
 
        List<MovieDetailDto.PersonDto> producers = credits.stream()
            .filter(c -> c.getRoleType() == com.lorafilm.movie.movie.domain.enums.CreditRoleType.PRODUCER)
            .map(c -> {
                MovieDetailDto.PersonDto p = new MovieDetailDto.PersonDto();
                p.setPublicId(c.getPerson().getPublicId());
                p.setFullName(c.getPerson().getFullName());
                p.setRoleType(c.getRoleType().name());
                p.setCharacterName(c.getCharacterName());
                p.setProfileImageUrl(c.getPerson().getProfileImageUrl());
                return p;
            }).collect(Collectors.toList());
 
        detailDto.setDirectors(directors);
        detailDto.setActors(actors);
        detailDto.setWriters(writers);
        detailDto.setProducers(producers);
 
        List<MovieProductionCompany> companies = movieProductionCompanyRepository.findByMovieId(movie.getId());
         
        List<MovieDetailDto.ProductionCompanyDto> productionCompanies = companies.stream()
            .filter(c -> c.getRole() == com.lorafilm.movie.movie.domain.enums.CompanyRoleType.PRODUCTION)
            .map(c -> {
                MovieDetailDto.ProductionCompanyDto p = new MovieDetailDto.ProductionCompanyDto();
                p.setPublicId(c.getProductionCompany().getPublicId());
                p.setName(c.getProductionCompany().getName());
                p.setRole(c.getRole().name());
                p.setLogoUrl(c.getProductionCompany().getLogoUrl());
                return p;
            }).collect(Collectors.toList());
 
        List<MovieDetailDto.ProductionCompanyDto> distributors = companies.stream()
            .filter(c -> c.getRole() == com.lorafilm.movie.movie.domain.enums.CompanyRoleType.DISTRIBUTOR)
            .map(c -> {
                MovieDetailDto.ProductionCompanyDto p = new MovieDetailDto.ProductionCompanyDto();
                p.setPublicId(c.getProductionCompany().getPublicId());
                p.setName(c.getProductionCompany().getName());
                p.setRole(c.getRole().name());
                p.setLogoUrl(c.getProductionCompany().getLogoUrl());
                return p;
            }).collect(Collectors.toList());
 
        List<MovieDetailDto.ProductionCompanyDto> studios = companies.stream()
            .filter(c -> c.getRole() == com.lorafilm.movie.movie.domain.enums.CompanyRoleType.STUDIO)
            .map(c -> {
                MovieDetailDto.ProductionCompanyDto p = new MovieDetailDto.ProductionCompanyDto();
                p.setPublicId(c.getProductionCompany().getPublicId());
                p.setName(c.getProductionCompany().getName());
                p.setRole(c.getRole().name());
                p.setLogoUrl(c.getProductionCompany().getLogoUrl());
                return p;
            }).collect(Collectors.toList());

        detailDto.setProductionCompanies(productionCompanies);
        detailDto.setDistributors(distributors);
        detailDto.setStudios(studios);

        List<MovieVersion> versions = movieVersionRepository.findByMovieIdAndStatusAndDeletedAtIsNull(movie.getId(), ActiveStatus.ACTIVE);
        List<MovieDetailDto.MovieVersionDto> versionDtos = versions.stream().map(v -> {
            MovieDetailDto.MovieVersionDto d = new MovieDetailDto.MovieVersionDto();
            d.setPublicId(v.getPublicId());
            d.setVersionName(v.getVersionName());
            d.setFormat(v.getFormat() != null ? v.getFormat().getValue() : null);
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
