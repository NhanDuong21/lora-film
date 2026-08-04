package com.lorafilm.movie.movie.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieCredit;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieProductionCompany;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.AdminMovieListQuery;
import com.lorafilm.movie.movie.dto.MovieBulkApprovalResponse;
import com.lorafilm.movie.movie.dto.MovieBulkApprovalResult;
import com.lorafilm.movie.movie.dto.TmdbQueueBreakdownResponse;
import com.lorafilm.movie.movie.dto.MovieDetailDto;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieHealthSpecifications;
import com.lorafilm.movie.movie.repository.MovieSpecification;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;

@Service
public class MovieServiceImpl implements MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieServiceImpl.class);

    private static final java.util.Set<String> SORT_FIELDS = java.util.Set.of(
            "updatedAt", "releaseDate", "title", "tmdbLastUpdated", "createdAt");

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final MovieProductionCompanyRepository movieProductionCompanyRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final MovieMapper movieMapper;
    private final MovieReadinessEvaluator readinessEvaluator;
    private final AdminMovieProjectionService projectionService;
    private final MovieLifecyclePolicy lifecyclePolicy;
    private final MovieApprovalPolicy approvalPolicy;
    private final MovieStatusHistoryService statusHistoryService;
    private final CurrentUserProvider currentUserProvider;

    public MovieServiceImpl(MovieRepository movieRepository,
            MovieGenreRepository movieGenreRepository,
            MovieMediaRepository movieMediaRepository,
            MovieCreditRepository movieCreditRepository,
            MovieProductionCompanyRepository movieProductionCompanyRepository,
            MovieVersionRepository movieVersionRepository,
            MovieMapper movieMapper,
            MovieReadinessEvaluator readinessEvaluator,
            AdminMovieProjectionService projectionService,
            MovieLifecyclePolicy lifecyclePolicy,
            MovieApprovalPolicy approvalPolicy,
            MovieStatusHistoryService statusHistoryService,
            CurrentUserProvider currentUserProvider) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.movieCreditRepository = movieCreditRepository;
        this.movieProductionCompanyRepository = movieProductionCompanyRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.movieMapper = movieMapper;
        this.readinessEvaluator = readinessEvaluator;
        this.projectionService = projectionService;
        this.lifecyclePolicy = lifecyclePolicy;
        this.approvalPolicy = approvalPolicy;
        this.statusHistoryService = statusHistoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieDto> getMovies(AdminMovieListQuery query) {
        Specification<Movie> spec = buildAdminMovieSpecification(query);

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), parseSort(query.getSort()));
        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        return projectionService.enrichMovies(moviePage);
    }

    @Override
    @Transactional
    public MovieBulkApprovalResponse bulkApproveTmdbMovies(AdminMovieListQuery filter, int limit) {
        validateBulkApprovalFilter(filter, limit);

        java.time.LocalDate today = lifecyclePolicy.currentDate();
        Specification<Movie> approvalWindow = MovieSpecification.releaseDateFrom(today.plusDays(1))
                .or(MovieSpecification.releaseDateTo(today).and(
                        MovieSpecification.hasOperationalShowtime(
                                lifecyclePolicy.currentInstant(),
                                MovieApprovalPolicy.OPERATIONAL_SHOWTIME_STATUSES,
                                true)));
        Specification<Movie> specification = buildAdminMovieSpecification(filter)
                .and(MovieSpecification.hasStatus(MovieStatus.DRAFT))
                .and(MovieSpecification.hasTmdbSource(true))
                .and(approvalWindow);
        Pageable pageable = PageRequest.of(0, limit, parseSort(filter.getSort()));
        List<Movie> candidates = movieRepository.findAll(specification, pageable).getContent();
        List<MovieBulkApprovalResult> results = new java.util.ArrayList<>();

        for (Movie candidate : candidates) {
            String publicId = candidate.getPublicId();
            String title = candidate.getTitle();
            try {
                Movie freshMovie = movieRepository.findByPublicIdForUpdate(publicId).orElse(null);
                if (freshMovie == null) {
                    results.add(MovieBulkApprovalResult.skipped(
                            publicId,
                            title,
                            ErrorCode.MOVIE_NOT_FOUND.name(),
                            "Movie no longer exists or was deleted."));
                    continue;
                }
                if (freshMovie.getStatus() != MovieStatus.DRAFT) {
                    results.add(MovieBulkApprovalResult.skipped(
                            publicId,
                            freshMovie.getTitle(),
                            "STATUS_CHANGED",
                            "Movie is no longer waiting for approval."));
                    continue;
                }
                if (freshMovie.getTmdbId() == null) {
                    results.add(MovieBulkApprovalResult.skipped(
                            publicId,
                            freshMovie.getTitle(),
                            "SOURCE_CHANGED",
                            "Movie is no longer a TMDB import."));
                    continue;
                }

                MovieApprovalPolicy.ApprovalDecision decision = approvalPolicy.evaluate(freshMovie);
                if (decision.targetStatus() == null) {
                    results.add(MovieBulkApprovalResult.skipped(
                            publicId,
                            freshMovie.getTitle(),
                            "RELEASE_DATE_REQUIRED",
                            String.join(" ", decision.blockers())));
                    continue;
                }

                MovieDto approved = transitionMovieStatus(
                        freshMovie, decision.targetStatus(), "Bulk TMDB approval");
                results.add(MovieBulkApprovalResult.approved(
                        publicId,
                        freshMovie.getTitle(),
                        approved.getStatus()));
            } catch (BusinessException exception) {
                ErrorCode errorCode = exception.getErrorCode();
                results.add(MovieBulkApprovalResult.skipped(
                        publicId,
                        title,
                        errorCode == null ? "VALIDATION_FAILED" : errorCode.name(),
                        exception.getMessage()));
            } catch (RuntimeException exception) {
                log.error("Bulk TMDB approval failed for movie {}", publicId, exception);
                results.add(MovieBulkApprovalResult.error(
                        publicId,
                        title,
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        "Unexpected error while approving this movie."));
            }
        }

        int approved = (int) results.stream().filter(item -> "APPROVED".equals(item.outcome())).count();
        int errors = (int) results.stream().filter(item -> "ERROR".equals(item.outcome())).count();
        int skipped = results.size() - approved - errors;
        return new MovieBulkApprovalResponse(
                candidates.size(),
                approved,
                skipped,
                errors,
                limit,
                List.copyOf(results));
    }

    @Override
    @Transactional(readOnly = true)
    public TmdbQueueBreakdownResponse getTmdbQueueBreakdown(AdminMovieListQuery filter) {
        validateTmdbQueueFilter(filter);

        Specification<Movie> base = buildAdminMovieSpecification(filter)
                .and(MovieSpecification.hasStatus(MovieStatus.DRAFT))
                .and(MovieSpecification.hasTmdbSource(true));
        java.time.LocalDate today = lifecyclePolicy.currentDate();
        long total = movieRepository.count(base);
        long future = movieRepository.count(base.and(MovieSpecification.releaseDateFrom(today.plusDays(1))));
        Specification<Movie> released = MovieSpecification.releaseDateTo(today);
        long readyToShow = movieRepository.count(base
                .and(released)
                .and(MovieSpecification.hasOperationalShowtime(
                        lifecyclePolicy.currentInstant(),
                        MovieApprovalPolicy.OPERATIONAL_SHOWTIME_STATUSES,
                        true)));
        long needsSchedule = movieRepository.count(base
                .and(released)
                .and(MovieSpecification.hasOperationalShowtime(
                        lifecyclePolicy.currentInstant(),
                        MovieApprovalPolicy.OPERATIONAL_SHOWTIME_STATUSES,
                        false)));
        long undated = movieRepository.count(base.and(MovieSpecification.releaseDateIsNull()));
        return new TmdbQueueBreakdownResponse(total, future, readyToShow, needsSchedule, undated);
    }

    private Specification<Movie> buildAdminMovieSpecification(AdminMovieListQuery query) {
        if (query == null) {
            throw validationError("Movie filter is required");
        }

        Specification<Movie> spec = Specification.where(MovieSpecification.isNotDeleted());

        String status = normalize(query.getStatus());
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            try {
                MovieStatus parsedStatus = MovieStatus.valueOf(status.toUpperCase(Locale.ROOT));
                spec = spec.and(MovieSpecification.hasStatus(parsedStatus));
            } catch (IllegalArgumentException e) {
                throw validationError("Invalid status: " + status);
            }
        }

        if (query.getGenreId() != null) {
            spec = spec.and(MovieSpecification.hasGenreId(query.getGenreId()));
        }

        String genrePublicId = normalize(query.getGenrePublicId());
        if (genrePublicId != null) {
            spec = spec.and(MovieSpecification.hasGenrePublicId(genrePublicId));
        }

        String keyword = normalize(query.getKeyword());
        if (keyword != null) {
            spec = spec.and(MovieSpecification.hasKeyword(keyword));
        }

        String city = normalize(query.getCity());
        if (city != null) {
            spec = spec.and(MovieSpecification.hasShowtimeInCity(city));
        }

        if (query.getCinemaId() != null) {
            spec = spec.and(MovieSpecification.hasShowtimeInCinema(query.getCinemaId()));
        }

        if (query.getDate() != null) {
            spec = spec.and(MovieSpecification.hasShowtimeOnDate(query.getDate()));
        }

        String source = normalize(query.getSource());
        if (source != null) {
            if (source.equalsIgnoreCase("TMDB")) {
                spec = spec.and(MovieSpecification.hasTmdbSource(true));
            } else if (source.equalsIgnoreCase("MANUAL")) {
                spec = spec.and(MovieSpecification.hasTmdbSource(false));
            } else {
                throw validationError("Invalid source: " + source + ". Allowed values: TMDB, MANUAL");
            }
        }

        String healthStatus = normalize(query.getHealthStatus());
        if (healthStatus != null) {
            try {
                MovieHealthStatus parsed = MovieHealthStatus.valueOf(healthStatus.toUpperCase(Locale.ROOT));
                spec = spec.and(MovieHealthSpecifications.healthStatusEquals(parsed));
            } catch (IllegalArgumentException e) {
                throw validationError("Invalid healthStatus: " + healthStatus + ". Allowed values: READY, WARNING, BLOCKED");
            }
        }

        Boolean hasPrimaryPoster = parseBoolean(query.getHasPrimaryPoster(), "hasPrimaryPoster");
        if (hasPrimaryPoster != null) {
            Specification<Movie> posterSpec = MovieHealthSpecifications.hasActivePrimaryPoster();
            spec = spec.and(hasPrimaryPoster ? posterSpec : Specification.not(posterSpec));
        }

        Boolean hasActiveVersion = parseBoolean(query.getHasActiveVersion(), "hasActiveVersion");
        if (hasActiveVersion != null) {
            Specification<Movie> versionSpec = MovieHealthSpecifications.hasActiveVersion();
            spec = spec.and(hasActiveVersion ? versionSpec : Specification.not(versionSpec));
        }

        Boolean hasShowtime = parseBoolean(query.getHasShowtime(), "hasShowtime");
        if (hasShowtime != null) {
            spec = spec.and(MovieSpecification.hasShowtime(hasShowtime));
        }

        String country = normalize(query.getCountry());
        if (country != null) {
            spec = spec.and(MovieSpecification.hasCountry(country));
        }

        validateRange(query.getReleaseDateFrom(), query.getReleaseDateTo(), "releaseDate");
        if (query.getReleaseDateFrom() != null) {
            spec = spec.and(MovieSpecification.releaseDateFrom(query.getReleaseDateFrom()));
        }
        if (query.getReleaseDateTo() != null) {
            spec = spec.and(MovieSpecification.releaseDateTo(query.getReleaseDateTo()));
        }

        validateRange(query.getTmdbUpdatedFrom(), query.getTmdbUpdatedTo(), "tmdbUpdated");
        if (query.getTmdbUpdatedFrom() != null) {
            spec = spec.and(MovieSpecification.tmdbUpdatedFrom(query.getTmdbUpdatedFrom().atStartOfDay()));
        }
        if (query.getTmdbUpdatedTo() != null) {
            try {
                spec = spec.and(MovieSpecification.tmdbUpdatedBefore(
                        query.getTmdbUpdatedTo().plusDays(1).atStartOfDay()));
            } catch (java.time.DateTimeException exception) {
                throw validationError("tmdbUpdatedTo is outside the supported date range");
            }
        }
        return spec;
    }

    private void validateBulkApprovalFilter(AdminMovieListQuery filter, int limit) {
        if (filter == null) {
            throw validationError("Movie filter is required");
        }
        if (limit < 1 || limit > 100) {
            throw validationError("Bulk approval limit must be between 1 and 100");
        }
        if (!"DRAFT".equalsIgnoreCase(normalize(filter.getStatus()))) {
            throw validationError("Bulk approval requires status=DRAFT");
        }
        if (!"TMDB".equalsIgnoreCase(normalize(filter.getSource()))) {
            throw validationError("Bulk approval requires source=TMDB");
        }
    }

    private void validateTmdbQueueFilter(AdminMovieListQuery filter) {
        if (filter == null) {
            throw validationError("Movie filter is required");
        }
        if (!"DRAFT".equalsIgnoreCase(normalize(filter.getStatus()))) {
            throw validationError("TMDB queue breakdown requires status=DRAFT");
        }
        if (!"TMDB".equalsIgnoreCase(normalize(filter.getSource()))) {
            throw validationError("TMDB queue breakdown requires source=TMDB");
        }
    }

    private Sort parseSort(String rawSort) {
        String sort = rawSort == null ? "releaseDate,desc" : rawSort.trim();
        if (sort.isEmpty()) {
            throw validationError("Invalid sort format. Expected field,direction");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2) {
            throw validationError("Invalid sort format. Expected field,direction");
        }
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase(Locale.ROOT);
        if (!SORT_FIELDS.contains(field)) {
            throw validationError("Unsupported sort field: " + field);
        }
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw validationError("Unsupported sort direction: " + parts[1].trim());
        }
        Sort primary = direction.equals("desc") ? Sort.by(field).descending() : Sort.by(field).ascending();
        return primary.and(Sort.by("id").descending());
    }

    private Boolean parseBoolean(String rawValue, String fieldName) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue.equals("true")) {
            return true;
        }
        if (rawValue.equals("false")) {
            return false;
        }
        throw validationError("Invalid " + fieldName + ": " + rawValue + ". Allowed values: true, false");
    }

    private void validateRange(java.time.LocalDate from, java.time.LocalDate to, String fieldName) {
        if (from != null && to != null && from.isAfter(to)) {
            throw validationError(fieldName + "From must be on or before " + fieldName + "To");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private BusinessException validationError(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieDetailDto getMovieByIdentifier(String identifier) {
        Optional<Movie> movieOpt = movieRepository.findByPublicIdAndDeletedAtIsNull(identifier);
        if (movieOpt.isEmpty()) {
            movieOpt = movieRepository.findBySlugAndDeletedAtIsNull(identifier);
        }
        Movie movie = movieOpt.orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        MovieDetailDto detailDto = mapToDetailDto(movie);
        return projectionService.enrichMovieDetail(movie, detailDto);
    }

    @Override
    @Transactional
    public MovieDto updateMovieStatus(String moviePublicId, MovieStatus targetStatus) {
        return updateMovieStatus(moviePublicId, targetStatus, null);
    }

    @Override
    @Transactional
    public MovieDto updateMovieStatus(String moviePublicId, MovieStatus targetStatus, String reason) {
        Movie movie = movieRepository.findByPublicIdForUpdate(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));

        return transitionMovieStatus(movie, targetStatus, reason);
    }

    private MovieDto transitionMovieStatus(Movie movie, MovieStatus targetStatus) {
        return transitionMovieStatus(movie, targetStatus, null);
    }

    private MovieDto transitionMovieStatus(Movie movie, MovieStatus targetStatus, String reason) {
        MovieStatus previousStatus = movie.getStatus();
        lifecyclePolicy.validateTransition(movie, targetStatus);
        if (previousStatus == MovieStatus.DRAFT
                && (targetStatus == MovieStatus.UPCOMING || targetStatus == MovieStatus.NOW_SHOWING)) {
            approvalPolicy.validateApprovalTarget(movie, targetStatus);
        } else if (targetStatus == MovieStatus.NOW_SHOWING) {
            approvalPolicy.validateNowShowingSchedule(movie);
        }
        if (targetStatus == MovieStatus.UPCOMING || targetStatus == MovieStatus.NOW_SHOWING) {
            validatePublishConditions(movie);
        }

        movie.setStatus(targetStatus);
        Movie savedMovie = movieRepository.save(movie);
        statusHistoryService.record(
                savedMovie,
                previousStatus,
                targetStatus,
                reason,
                currentUserProvider.getCurrentUserId());
        log.info("Movie lifecycle transition applied: publicId={}, from={}, to={}",
                movie.getPublicId(), previousStatus, targetStatus);

        List<String> genres = movieGenreRepository.findByMovieId(savedMovie.getId())
                .stream().map(mg -> mg.getGenre().getName()).collect(Collectors.toList());
        Optional<MovieMedia> primaryPoster = movieMediaRepository
                .findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        savedMovie.getId(), MovieMediaType.POSTER, ActiveStatus.ACTIVE);
        String posterUrl = primaryPoster.map(MovieMedia::getUrl).orElse(null);

        return movieMapper.toDto(savedMovie, genres, posterUrl);
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public void validatePublishConditions(Long movieId) {
        Optional<Movie> movie = movieRepository.findById(movieId);
        validatePublishConditions(movie.orElse(null), movieId);
    }

    private void validatePublishConditions(Movie movie) {
        validatePublishConditions(movie, movie.getId());
    }

    private void validatePublishConditions(Movie movie, Long movieId) {
        boolean hasActiveVersion = movieVersionRepository.existsActiveVersion(movieId);
        boolean hasPrimaryPoster = movieMediaRepository.existsPrimaryPoster(movieId);
        boolean hasGenres = !movieGenreRepository.findByMovieId(movieId).isEmpty();

        MovieHealthFacts healthFacts = MovieHealthFacts.from(
                movie,
                hasGenres,
                hasActiveVersion,
                hasPrimaryPoster);
        readinessEvaluator.validatePublishConditions(healthFacts);
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
                p.setDisplayOrder(c.getDisplayOrder());
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
                p.setDisplayOrder(c.getDisplayOrder());
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
                p.setDisplayOrder(c.getDisplayOrder());
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
                p.setDisplayOrder(c.getDisplayOrder());
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

        List<MovieVersion> versions = movieVersionRepository.findByMovieIdAndDeletedAtIsNull(movie.getId());
        List<MovieDetailDto.MovieVersionDto> versionDtos = versions.stream().map(v -> {
            MovieDetailDto.MovieVersionDto d = new MovieDetailDto.MovieVersionDto();
            d.setPublicId(v.getPublicId());
            d.setVersionName(v.getVersionName());
            d.setFormat(v.getFormat() != null ? v.getFormat().getValue() : null);
            d.setAudioLanguage(v.getAudioLanguage());
            d.setSubtitleLanguage(v.getSubtitleLanguage());
            d.setDubLanguage(v.getDubLanguage());
            d.setStatus(v.getStatus() != null ? v.getStatus().name() : null);
            return d;
        }).collect(Collectors.toList());
        detailDto.setVersions(versionDtos);

        List<MovieMedia> media = movieMediaRepository.findByMovieIdAndDeletedAtIsNull(movie.getId());
        List<MovieDetailDto.MovieMediaDto> mediaDtos = media.stream().map(m -> {
            MovieDetailDto.MovieMediaDto d = new MovieDetailDto.MovieMediaDto();
            d.setPublicId(m.getPublicId());
            d.setMediaType(m.getMediaType().name());
            d.setUrl(m.getUrl());
            d.setTitle(m.getTitle());
            d.setIsPrimary(m.getIsPrimary());
            d.setDisplayOrder(m.getDisplayOrder());
            d.setStatus(m.getStatus() != null ? m.getStatus().name() : null);
            return d;
        }).collect(Collectors.toList());
        detailDto.setMedia(mediaDtos);

        return detailDto;
    }
}
