package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbImportOutcome;
import com.lorafilm.movie.integration.tmdb.dto.TmdbImportResult;
import com.lorafilm.movie.integration.tmdb.mapper.TmdbMovieMapper;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lorafilm.movie.movie.repository.GenreRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.PersonRepository;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieTranslationRepository;
import com.lorafilm.movie.movie.repository.ProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.MovieGenreId;
import com.lorafilm.movie.movie.domain.entity.Person;
import com.lorafilm.movie.movie.domain.entity.MovieCredit;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieTranslation;
import com.lorafilm.movie.movie.domain.entity.ProductionCompany;
import com.lorafilm.movie.movie.domain.entity.MovieProductionCompany;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.CreditRoleType;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.integration.tmdb.dto.TmdbGenreDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbPersonDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbTrailerDto;
import com.lorafilm.movie.common.enums.ActiveStatus;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TmdbImportService {

    private static final Logger log = LoggerFactory.getLogger(TmdbImportService.class);
    private static final String SYNC_TYPE_BULK = "TMDB_BULK_EXPORT";

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile Thread bulkSyncThread;

    private final TmdbClient tmdbClient;
    private final TmdbProperties properties;
    private final MovieRepository movieRepository;
    private final TmdbMovieMapper movieMapper;
    private final TmdbSyncStateRepository syncStateRepository;
    private final ObjectMapper objectMapper;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final PersonRepository personRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final MovieTranslationRepository movieTranslationRepository;
    private final ProductionCompanyRepository productionCompanyRepository;
    private final MovieProductionCompanyRepository movieProductionCompanyRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final TmdbProviderMovieService providerMovieService;
    private TmdbImportService self;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    public void setSelf(TmdbImportService self) {
        this.self = self;
    }

    public TmdbImportService(TmdbClient tmdbClient, TmdbProperties properties,
                             MovieRepository movieRepository, TmdbMovieMapper movieMapper,
                             TmdbSyncStateRepository syncStateRepository, ObjectMapper objectMapper,
                             GenreRepository genreRepository, MovieGenreRepository movieGenreRepository,
                             PersonRepository personRepository, MovieCreditRepository movieCreditRepository,
                             MovieMediaRepository movieMediaRepository,
                             MovieTranslationRepository movieTranslationRepository,
                             ProductionCompanyRepository productionCompanyRepository,
                             MovieProductionCompanyRepository movieProductionCompanyRepository,
                             MovieVersionRepository movieVersionRepository,
                             TmdbProviderMovieService providerMovieService) {
        this.tmdbClient = tmdbClient;
        this.properties = properties;
        this.movieRepository = movieRepository;
        this.movieMapper = movieMapper;
        this.syncStateRepository = syncStateRepository;
        this.objectMapper = objectMapper;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.personRepository = personRepository;
        this.movieCreditRepository = movieCreditRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.movieTranslationRepository = movieTranslationRepository;
        this.productionCompanyRepository = productionCompanyRepository;
        this.movieProductionCompanyRepository = movieProductionCompanyRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.providerMovieService = providerMovieService;
    }

    public void stopBulkSync() {
        log.info("Request to stop TMDB Bulk Sync received.");
        stopRequested.set(true);
        if (bulkSyncThread != null && bulkSyncThread.isAlive()) {
            bulkSyncThread.interrupt();
        }
    }

    public TmdbSyncState getBulkSyncStatus() {
        return syncStateRepository.findBySyncType(SYNC_TYPE_BULK)
                .orElseGet(() -> {
                    TmdbSyncState state = new TmdbSyncState();
                    state.setSyncType(SYNC_TYPE_BULK);
                    state.setStatus("IDLE");
                    state.setCursor("0");
                    return state;
                });
    }

    public void resetBulkSyncState() {
        TmdbSyncState syncState = syncStateRepository.findBySyncType(SYNC_TYPE_BULK)
                .orElseGet(() -> {
                    TmdbSyncState state = new TmdbSyncState();
                    state.setSyncType(SYNC_TYPE_BULK);
                    return state;
                });
        syncState.setCursor("0");
        syncState.setStatus("IDLE");
        syncState.setLastSyncTime(null);
        syncStateRepository.save(syncState);
        log.info("TMDB Bulk Sync state reset to cursor 0 and status IDLE.");
    }

    /**
     * Scenario 1: Bulk Export loop
     */
    public void runBulkSync() {
        runBulkSync(false);
    }

    public void runBulkSync(boolean force) {
        if (!properties.isSyncEnabled() && !force) {
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

        if (force) {
            log.info("Force flag enabled. Resetting TMDB sync state status to IDLE and cursor to 0.");
            syncState.setStatus("IDLE");
            syncState.setCursor("0");
            syncStateRepository.save(syncState);
        } else if ("IN_PROGRESS".equals(syncState.getStatus())) {
            // Check if stuck based on threshold
            LocalDateTime thresholdTime = LocalDateTime.now().minusSeconds(properties.getSyncStaleThresholdSeconds());
            if (syncState.getUpdatedAt() != null && syncState.getUpdatedAt().isBefore(thresholdTime)) {
                log.warn("TMDB Bulk Sync was stuck IN_PROGRESS for over {} seconds. Automatically recovering state to IDLE.", properties.getSyncStaleThresholdSeconds());
                syncState.setStatus("IDLE");
                syncStateRepository.save(syncState);
            } else {
                log.warn("TMDB Bulk sync is already in progress. Skipping.");
                return;
            }
        }

        stopRequested.set(false);
        bulkSyncThread = Thread.currentThread();

        try {
            log.info("Triggering TMDB export download on Node.js...");
            tmdbClient.triggerDownloadExport();
            // Give it a moment just in case
            Thread.sleep(2000);
            
            syncState.setStatus("IN_PROGRESS");
            syncStateRepository.save(syncState);
            
            boolean hasMore = true;
            String currentCursor = syncState.getCursor();
            int retryCount = 0;
            int maxRetries = 5;
            
            while (hasMore) {
                if (stopRequested.get() || Thread.currentThread().isInterrupted()) {
                    log.info("TMDB Bulk Sync stopped by user request at cursor {}", currentCursor);
                    syncState.setStatus("IDLE");
                    syncStateRepository.save(syncState);
                    return;
                }

                try {
                    log.info("Fetching TMDB export with cursor {}", currentCursor);
                    String responseBody = tmdbClient.fetchMoviesExport(currentCursor, properties.getBatchSize());
                    TmdbMovieResponse response = objectMapper.readValue(responseBody, TmdbMovieResponse.class);
                    
                    if (response != null && response.getMovies() != null) {
                        try {
                            if (self != null) {
                                self.importMovies(response.getMovies());
                            } else {
                                importMovies(response.getMovies());
                            }
                        } catch (Exception e) {
                            log.error("Failed to batch import movies at cursor {}: {}", currentCursor, e.getMessage(), e);
                        }
                        
                        currentCursor = response.getNextCursor();
                        hasMore = Boolean.TRUE.equals(response.getHasMore());
                        
                        
                        syncState.setCursor(currentCursor);
                        syncStateRepository.save(syncState);
                        
                        retryCount = 0; // Reset retry count on success
                        // Sleep to avoid overloading the Node API and TMDB rate limit
                        Thread.sleep(1000);
                    } else {
                        hasMore = false;
                    }
                } catch (InterruptedException e) {
                    log.info("TMDB Bulk Sync interrupted at cursor {}", currentCursor);
                    syncState.setStatus("IDLE");
                    syncStateRepository.save(syncState);
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    if (stopRequested.get() || Thread.currentThread().isInterrupted()) {
                        log.info("TMDB Bulk Sync stopped during exception handling at cursor {}", currentCursor);
                        syncState.setStatus("IDLE");
                        syncStateRepository.save(syncState);
                        return;
                    }
                    retryCount++;
                    if (retryCount <= maxRetries) {
                        log.warn("Network/Server error fetching export at cursor {} (Attempt {}/{}): {}. Retrying in 5 seconds...", 
                                currentCursor, retryCount, maxRetries, e.getMessage());
                        Thread.sleep(5000);
                    } else {
                        log.error("Max retries ({}) reached for cursor {}. Stopping bulk sync gracefully.", maxRetries, currentCursor);
                        throw e;
                    }
                }
            }

            syncState.setStatus("COMPLETED");
            syncState.setLastSyncTime(LocalDateTime.now());
            syncStateRepository.save(syncState);
            log.info("TMDB Bulk Sync completed successfully.");
        } catch (InterruptedException e) {
            log.info("TMDB Bulk Sync thread interrupted.");
            syncState.setStatus("IDLE");
            syncStateRepository.save(syncState);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("TMDB Bulk sync process stopped with error: {}", e.getMessage());
            syncState.setStatus("FAILED");
            syncStateRepository.save(syncState);
        } finally {
            bulkSyncThread = null;
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
                if (self != null) {
                    self.importMovies(movies);
                } else {
                    importMovies(movies);
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
                if (self != null) {
                    self.importMovies(movies);
                } else {
                    importMovies(movies);
                }
            }
        } catch (Exception e) {
            log.error("Error during TMDB Daily Updated sync", e);
        }
    }

    /**
     * Scenario 3: Sync Single Movie By ID
     */
    public TmdbImportResult importMovieById(Long tmdbId) {
        TmdbMovieWrapperDto dto = providerMovieService.fetchMovie(tmdbId);
        return importMovieSafely(dto);
    }

    public TmdbImportResult importMovieSafely(TmdbMovieWrapperDto wrapper) {
        try {
            return self != null ? self.importMovie(wrapper) : importMovie(wrapper);
        } catch (DataIntegrityViolationException exception) {
            if (wrapper == null || wrapper.getTmdbId() == null) throw exception;
            Movie winner = movieRepository.findByTmdbId(wrapper.getTmdbId()).orElseThrow(() -> exception);
            TmdbImportOutcome outcome = winner.getDeletedAt() == null
                    ? TmdbImportOutcome.ALREADY_IMPORTED
                    : TmdbImportOutcome.DELETED_TOMBSTONE;
            log.info("Concurrent import for TMDB ID {} lost the unique-key race; returning {}",
                    wrapper.getTmdbId(), outcome);
            return new TmdbImportResult(
                    wrapper.getTmdbId(),
                    outcome,
                    winner.getPublicId(),
                    "A concurrent request already imported this movie; existing data was preserved");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TmdbImportResult importMovie(TmdbMovieWrapperDto wrapper) {
        providerMovieService.validateIdentity(null, wrapper);
        
        String statusStr = wrapper.getApprovalStatus() != null ? wrapper.getApprovalStatus() : wrapper.getQualityStatus();
        log.info("Processing TMDB ID {}: Status is {}", wrapper.getTmdbId(), statusStr);

        if ("REJECTED".equalsIgnoreCase(statusStr) || "REJECT".equalsIgnoreCase(statusStr)) {
            log.info("Skipping REJECTED TMDB Movie ID: {}", wrapper.getTmdbId());
            return new TmdbImportResult(
                    wrapper.getTmdbId(),
                    TmdbImportOutcome.REJECTED_BY_PROVIDER,
                    null,
                    "Provider rejected movie; nothing was imported");
        }

        Movie existingMovie = movieRepository.findByTmdbId(wrapper.getTmdbId()).orElse(null);
        if (existingMovie != null) {
            TmdbImportOutcome outcome = existingMovie.getDeletedAt() == null
                    ? TmdbImportOutcome.ALREADY_IMPORTED
                    : TmdbImportOutcome.DELETED_TOMBSTONE;
            log.info("TMDB ID {} already exists with outcome {}; preserving aggregate", wrapper.getTmdbId(), outcome);
            return new TmdbImportResult(
                    wrapper.getTmdbId(),
                    outcome,
                    existingMovie.getPublicId(),
                    outcome == TmdbImportOutcome.DELETED_TOMBSTONE
                            ? "Deleted TMDB movie is preserved as a tombstone"
                            : "Movie is already imported; existing data was preserved");
        }

        Movie newMovie = movieMapper.toEntity(wrapper);
        newMovie.setStatus(MovieStatus.DRAFT);
        String baseSlug = movieMapper.generateSlug(newMovie.getTitle());
        newMovie.setSlug(resolveUniqueMovieSlug(baseSlug, wrapper.getTmdbId(), null));
        newMovie = movieRepository.saveAndFlush(newMovie);
        extractAndSaveRelations(newMovie, wrapper);
        log.info("Inserted TMDB movie {} as DRAFT", wrapper.getTmdbId());
        return new TmdbImportResult(
                wrapper.getTmdbId(),
                TmdbImportOutcome.CREATED,
                newMovie.getPublicId(),
                "Movie imported as DRAFT");
    }

    private String resolveUniqueMovieSlug(String baseSlug, Long tmdbId, Long existingMovieId) {
        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "movie-" + (tmdbId != null ? tmdbId : UUID.randomUUID().toString().substring(0, 8));
        }

        java.util.Optional<Movie> conflict = movieRepository.findBySlugAndDeletedAtIsNull(baseSlug);
        if (conflict.isEmpty()) {
            return baseSlug; // Free slug, no TMDB ID needed
        }

        Movie found = conflict.get();
        if (existingMovieId != null && found.getId().equals(existingMovieId)) {
            return baseSlug; // Belongs to same movie being updated
        }
        if (tmdbId != null && tmdbId.equals(found.getTmdbId())) {
            return baseSlug; // Belongs to same TMDB ID
        }

        // Slug collision with a DIFFERENT movie -> append TMDB ID
        return baseSlug + "-" + tmdbId;
    }

    public void importMovies(List<TmdbMovieWrapperDto> wrappers) {
        if (wrappers == null || wrappers.isEmpty()) return;

        for (TmdbMovieWrapperDto wrapper : wrappers) {
            if (wrapper == null || wrapper.getMovie() == null) continue;
            try {
                importMovieSafely(wrapper);
            } catch (Exception e) {
                log.error("Failed to import single movie TMDB ID {}: {}", wrapper.getTmdbId(), e.getMessage());
            }
        }
    }

    private String safeTruncate(String input, int maxLength) {
        if (input == null) return null;
        return input.length() <= maxLength ? input : input.substring(0, maxLength);
    }

    private void extractAndSaveRelations(Movie movie, TmdbMovieWrapperDto wrapper) {
        if (wrapper == null) return;
        
        // 1. Genres
        if (wrapper.getGenres() != null && !wrapper.getGenres().isEmpty()) {
            movieGenreRepository.deleteByMovieId(movie.getId());
            java.util.Set<Long> processedGenreIds = new java.util.HashSet<>();
            for (TmdbGenreDto gDto : wrapper.getGenres()) {
                if (gDto.getName() == null) continue;
                String slug = movieMapper.generateSlug(gDto.getName());
                Genre genre = genreRepository.findBySlugAndDeletedAtIsNull(slug).orElseGet(() -> {
                    Genre newGenre = new Genre();
                    newGenre.setPublicId(UUID.randomUUID().toString());
                    newGenre.setName(safeTruncate(gDto.getName(), 255));
                    newGenre.setSlug(slug);
                    newGenre.setStatus(ActiveStatus.ACTIVE);
                    return genreRepository.save(newGenre);
                });
                
                if (processedGenreIds.add(genre.getId())) {
                    MovieGenre mg = new MovieGenre();
                    mg.setMovie(movie);
                    mg.setGenre(genre);
                    movieGenreRepository.save(mg);
                }
            }
        }
        
        // 2. Credits (Cast and Crew)
        if (wrapper.getCredits() != null) {
            movieCreditRepository.deleteByMovieId(movie.getId());
            
            class TempCredit {
                TmdbPersonDto dto;
                CreditRoleType role;
                int order;
                TempCredit(TmdbPersonDto dto, CreditRoleType role, int order) {
                    this.dto = dto; this.role = role; this.order = order;
                }
            }
            List<TempCredit> tempCredits = new ArrayList<>();
            
            if (wrapper.getCredits().getDirectors() != null) {
                int order = 1;
                for (TmdbPersonDto pDto : wrapper.getCredits().getDirectors()) {
                    tempCredits.add(new TempCredit(pDto, CreditRoleType.DIRECTOR, order++));
                }
            }
            if (wrapper.getCredits().getMainCast() != null) {
                for (TmdbPersonDto pDto : wrapper.getCredits().getMainCast()) {
                    tempCredits.add(new TempCredit(pDto, CreditRoleType.MAIN_ACTOR, pDto.getOrder() != null ? pDto.getOrder() + 1 : 999));
                }
            }
            if (wrapper.getCredits().getSupportingCast() != null) {
                for (TmdbPersonDto pDto : wrapper.getCredits().getSupportingCast()) {
                    tempCredits.add(new TempCredit(pDto, CreditRoleType.SUPPORTING_ACTOR, pDto.getOrder() != null ? pDto.getOrder() + 1 : 999));
                }
            }
            if (wrapper.getCredits().getWriters() != null) {
                int order = 1;
                for (TmdbPersonDto pDto : wrapper.getCredits().getWriters()) {
                    tempCredits.add(new TempCredit(pDto, CreditRoleType.WRITER, order++));
                }
            }
            if (wrapper.getCredits().getProducers() != null) {
                int order = 1;
                for (TmdbPersonDto pDto : wrapper.getCredits().getProducers()) {
                    tempCredits.add(new TempCredit(pDto, CreditRoleType.PRODUCER, order++));
                }
            }

            if (!tempCredits.isEmpty()) {
                Map<Long, TmdbPersonDto> uniquePersons = new java.util.HashMap<>();
                for (TempCredit tc : tempCredits) {
                    if (tc.dto.getTmdbPersonId() != null) {
                        uniquePersons.put(tc.dto.getTmdbPersonId(), tc.dto);
                    }
                }
                
                List<Long> personIds = new ArrayList<>(uniquePersons.keySet());
                Map<Long, Person> personMap = new java.util.HashMap<>();
                if (!personIds.isEmpty()) {
                    List<Person> existingPersons = personRepository.findByTmdbPersonIdIn(personIds);
                    for (Person p : existingPersons) {
                        personMap.put(p.getTmdbPersonId(), p);
                    }
                    
                    for (TmdbPersonDto pDto : uniquePersons.values()) {
                        if (!personMap.containsKey(pDto.getTmdbPersonId())) {
                            try {
                                Person newPerson = new Person();
                                newPerson.setPublicId(UUID.randomUUID().toString());
                                newPerson.setTmdbPersonId(pDto.getTmdbPersonId());
                                String nameToUse = pDto.getOriginalName() != null ? pDto.getOriginalName() : (pDto.getName() != null ? pDto.getName() : "Unknown");
                                newPerson.setFullName(safeTruncate(nameToUse, 255));
                                newPerson.setStageName(safeTruncate(pDto.getName(), 255));
                                newPerson.setProfileImageUrl(safeTruncate(pDto.getProfileUrl(), 255));
                                newPerson.setStatus(ActiveStatus.ACTIVE);
                                Person saved = personRepository.save(newPerson);
                                personMap.put(saved.getTmdbPersonId(), saved);
                            } catch (Exception ex) {
                                personRepository.findByTmdbPersonId(pDto.getTmdbPersonId())
                                        .ifPresent(p -> personMap.put(p.getTmdbPersonId(), p));
                            }
                        }
                    }
                }
                
                List<MovieCredit> creditsToSave = new ArrayList<>();
                for (TempCredit tc : tempCredits) {
                    if (tc.dto.getTmdbPersonId() == null) continue;
                    Person p = personMap.get(tc.dto.getTmdbPersonId());
                    if (p != null) {
                        MovieCredit credit = new MovieCredit();
                        credit.setMovie(movie);
                        credit.setPerson(p);
                        credit.setRoleType(tc.role);
                        credit.setCharacterName(safeTruncate(tc.dto.getCharacter(), 255));
                        credit.setDisplayOrder(tc.order);
                        creditsToSave.add(credit);
                    }
                }
                if (!creditsToSave.isEmpty()) {
                    movieCreditRepository.saveAll(creditsToSave);
                }
            }
        }
        
        // 3. Videos / Media — xóa cũ trước, insert mới
        movieMediaRepository.deleteByMovieId(movie.getId());

        if (wrapper.getVideos() != null && wrapper.getVideos().getPrimaryTrailer() != null) {
            TmdbTrailerDto trailer = wrapper.getVideos().getPrimaryTrailer();
            if (trailer.getUrl() != null) {
                MovieMedia media = new MovieMedia();
                media.setPublicId(UUID.randomUUID().toString());
                media.setMovie(movie);
                media.setMediaType(MovieMediaType.TRAILER);
                media.setUrl(safeTruncate(trailer.getUrl(), 255));
                media.setTitle(safeTruncate(trailer.getName() != null ? trailer.getName() : "Official Trailer", 255));
                media.setIsPrimary(true);
                media.setStatus(ActiveStatus.ACTIVE);
                media.setDisplayOrder(1);
                movieMediaRepository.save(media);
            }
        }
        
        if (wrapper.getMedia() != null) {
            // Posters
            if (wrapper.getMedia().getPosters() != null) {
                int displayOrder = 1;
                for (com.lorafilm.movie.integration.tmdb.dto.TmdbImageDto img : wrapper.getMedia().getPosters()) {
                    if (img.getUrl() == null) continue;
                    boolean isPrimary = wrapper.getMedia().getPrimaryPoster() != null 
                        && img.getUrl().equals(wrapper.getMedia().getPrimaryPoster().getUrl());
                        
                    MovieMedia media = new MovieMedia();
                    media.setPublicId(UUID.randomUUID().toString());
                    media.setMovie(movie);
                    media.setMediaType(MovieMediaType.POSTER);
                    media.setUrl(safeTruncate(img.getUrl(), 255));
                    media.setIsPrimary(isPrimary);
                    media.setStatus(ActiveStatus.ACTIVE);
                    media.setDisplayOrder(displayOrder++);
                    movieMediaRepository.save(media);
                }
            }

            // Backdrops (Banners)
            if (wrapper.getMedia().getBackdrops() != null) {
                int displayOrder = 1;
                for (com.lorafilm.movie.integration.tmdb.dto.TmdbImageDto img : wrapper.getMedia().getBackdrops()) {
                    if (img.getUrl() == null) continue;
                    boolean isPrimary = wrapper.getMedia().getPrimaryBackdrop() != null 
                        && img.getUrl().equals(wrapper.getMedia().getPrimaryBackdrop().getUrl());
                        
                    MovieMedia media = new MovieMedia();
                    media.setPublicId(UUID.randomUUID().toString());
                    media.setMovie(movie);
                    media.setMediaType(MovieMediaType.BANNER);
                    media.setUrl(safeTruncate(img.getUrl(), 255));
                    media.setIsPrimary(isPrimary);
                    media.setStatus(ActiveStatus.ACTIVE);
                    media.setDisplayOrder(displayOrder++);
                    movieMediaRepository.save(media);
                }
            }
        }
        
        // 4. Production Companies
        if (wrapper.getProductionCompanies() != null && !wrapper.getProductionCompanies().isEmpty()) {
            movieProductionCompanyRepository.deleteByMovieId(movie.getId());
            for (com.lorafilm.movie.integration.tmdb.dto.TmdbProductionCompanyDto companyDto : wrapper.getProductionCompanies()) {
                if (companyDto.getName() == null) continue;
                
                ProductionCompany company = productionCompanyRepository.findByNameIgnoreCase(companyDto.getName()).orElseGet(() -> {
                    ProductionCompany newCompany = new ProductionCompany();
                    newCompany.setPublicId(UUID.randomUUID().toString());
                    newCompany.setName(safeTruncate(companyDto.getName(), 255));
                    newCompany.setLogoUrl(safeTruncate(companyDto.getLogoUrl(), 255));
                    newCompany.setCountry(safeTruncate(companyDto.getOriginCountry(), 255));
                    newCompany.setStatus(ActiveStatus.ACTIVE);
                    return productionCompanyRepository.save(newCompany);
                });
                
                MovieProductionCompany mpc = new MovieProductionCompany();
                mpc.setMovie(movie);
                mpc.setProductionCompany(company);
                mpc.setRole(com.lorafilm.movie.movie.domain.enums.CompanyRoleType.PRODUCTION);
                movieProductionCompanyRepository.save(mpc);
            }
        }
        
        // 5. Translations
        movieTranslationRepository.deleteByMovieId(movie.getId());
        boolean hasVietnameseTranslation = false;
        java.util.Set<String> processedLocales = new java.util.HashSet<>();
        
        if (wrapper.getTranslations() != null && !wrapper.getTranslations().isEmpty()) {
            for (com.lorafilm.movie.integration.tmdb.dto.TmdbTranslationDto tDto : wrapper.getTranslations()) {
                String localeStr = (tDto.getLocale() != null && !tDto.getLocale().isBlank()) 
                        ? tDto.getLocale() 
                        : tDto.getLanguageCode();
                if (localeStr == null || localeStr.isBlank()) continue;

                String lang = tDto.getLanguageCode() != null ? tDto.getLanguageCode().toLowerCase() : "";
                String loc = tDto.getLocale() != null ? tDto.getLocale().toLowerCase() : "";
                if (lang.contains("vi") || loc.contains("vi")) {
                    hasVietnameseTranslation = true;
                }

                if (processedLocales.add(localeStr.toLowerCase())) {
                    MovieTranslation mt = new MovieTranslation();
                    mt.setMovie(movie);
                    mt.setLocale(safeTruncate(localeStr, 255));
                    mt.setTitle(safeTruncate(tDto.getTitle() != null && !tDto.getTitle().isBlank() ? tDto.getTitle() : movie.getTitle(), 255));
                    mt.setSynopsis(tDto.getOverview() != null && !tDto.getOverview().isBlank() ? tDto.getOverview() : movie.getSynopsis());
                    movieTranslationRepository.save(mt);
                }
            }
        }
        
        // Fallback: If no Vietnamese translation is available in TMDB translations, use original/base movie data as fallback
        if (!hasVietnameseTranslation && processedLocales.add("vi-vn")) {
            MovieTranslation defaultTranslation = new MovieTranslation();
            defaultTranslation.setMovie(movie);
            defaultTranslation.setLocale("vi-VN");
            defaultTranslation.setTitle(safeTruncate(movie.getTitle(), 255));
            defaultTranslation.setSynopsis(movie.getSynopsis());
            movieTranslationRepository.save(defaultTranslation);
        }
        
        // 6. Default Movie Version (since TMDB doesn't provide 2D/3D info)
        if (movieVersionRepository.findByMovieIdAndDeletedAtIsNull(movie.getId()).isEmpty()) {
            MovieVersion version = new MovieVersion();
            version.setPublicId(UUID.randomUUID().toString());
            version.setMovie(movie);
            version.setVersionName("2D - Phụ đề");
            version.setFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat.TWO_D);
            version.setAudioLanguage("EN");
            version.setSubtitleLanguage("VI");
            version.setStatus(ActiveStatus.ACTIVE);
            movieVersionRepository.save(version);
        }
    }

}
