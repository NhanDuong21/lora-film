package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.integration.tmdb.mapper.TmdbMovieMapper;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
import com.lorafilm.movie.integration.tmdb.dto.TmdbGenreDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbPersonDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbTrailerDto;
import com.lorafilm.movie.common.enums.ActiveStatus;
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

    public TmdbImportService(TmdbClient tmdbClient, TmdbProperties properties,
                             MovieRepository movieRepository, TmdbMovieMapper movieMapper,
                             TmdbSyncStateRepository syncStateRepository, ObjectMapper objectMapper,
                             GenreRepository genreRepository, MovieGenreRepository movieGenreRepository,
                             PersonRepository personRepository, MovieCreditRepository movieCreditRepository,
                             MovieMediaRepository movieMediaRepository,
                             MovieTranslationRepository movieTranslationRepository,
                             ProductionCompanyRepository productionCompanyRepository,
                             MovieProductionCompanyRepository movieProductionCompanyRepository,
                             MovieVersionRepository movieVersionRepository) {
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
    }

    /**
     * Scenario 1: Bulk Export loop
     */
    public void runBulkSync() {
        if (!properties.isSyncEnabled()) {
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

        if ("IN_PROGRESS".equals(syncState.getStatus())) {
            log.warn("TMDB Bulk sync is already in progress. Skipping.");
            return;
        }

        try {
            log.info("Triggering TMDB export download on Node.js...");
            tmdbClient.triggerDownloadExport();
            // Give it a moment just in case
            Thread.sleep(2000);
            
            syncState.setStatus("IN_PROGRESS");
            syncStateRepository.save(syncState);
            
            boolean hasMore = true;
            String currentCursor = syncState.getCursor();
            
            while (hasMore) {
                log.info("Fetching TMDB export with cursor {}", currentCursor);
                String responseBody = tmdbClient.fetchMoviesExport(currentCursor, properties.getBatchSize());
                TmdbMovieResponse response = objectMapper.readValue(responseBody, TmdbMovieResponse.class);
                
                if (response != null && response.getMovies() != null) {
                    try {
                        importMovies(response.getMovies());
                    } catch (Exception e) {
                        log.error("Failed to batch import movies: {}", e.getMessage());
                    }
                    
                    currentCursor = response.getNextCursor();
                    hasMore = Boolean.TRUE.equals(response.getHasMore());
                    
                    syncState.setCursor(currentCursor);
                    syncState.setLastSyncTime(LocalDateTime.now());
                    syncStateRepository.save(syncState);
                    
                    // Sleep to avoid overloading the Node API and TMDB rate limit
                    Thread.sleep(1000);
                } else {
                    hasMore = false;
                }
            }

            syncState.setStatus("COMPLETED");
            syncStateRepository.save(syncState);
            log.info("TMDB Bulk Sync completed successfully.");
        } catch (Exception e) {
            log.error("Error during TMDB Bulk sync process", e);
            syncState.setStatus("FAILED");
            syncStateRepository.save(syncState);
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
                importMovies(movies);
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
                importMovies(movies);
            }
        } catch (Exception e) {
            log.error("Error during TMDB Daily Updated sync", e);
        }
    }

    /**
     * Scenario 3: Sync Single Movie By ID
     */
    public void importMovieById(Long tmdbId) {
        try {
            String responseBody = tmdbClient.fetchMovieDetails(tmdbId);
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("success") && !root.get("success").asBoolean()) {
                throw new RuntimeException("TMDB Error: " + root.path("message").asText());
            }
            if (root.has("data")) {
                TmdbMovieWrapperDto dto = objectMapper.readValue(
                    root.get("data").toString(), 
                    TmdbMovieWrapperDto.class
                );
                importMovie(dto);
            }
        } catch (Exception e) {
            log.error("Error importing single movie {}: {}", tmdbId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importMovie(TmdbMovieWrapperDto wrapper) {
        if (wrapper == null) {
            log.warn("Wrapper is null");
            return;
        }
        if (wrapper.getMovie() == null) {
            log.warn("Wrapper movie is null for TMDB ID: {}", wrapper.getTmdbId());
            return;
        }
        
        // Quality check based on guide
        if (!"ACCEPT".equalsIgnoreCase(wrapper.getQualityStatus())) {
            log.info("SKIP TMDB ID {}: Quality Status is {}", wrapper.getTmdbId(), wrapper.getQualityStatus());
            return;
        }

        Movie existingMovie = movieRepository.findByTmdbId(wrapper.getTmdbId()).orElse(null);

        if (existingMovie == null) {
            // INSERT flow
            log.info("Attempting to insert TMDB ID: {}", wrapper.getTmdbId());
            Movie newMovie = movieMapper.toEntity(wrapper);
            newMovie = movieRepository.save(newMovie);
            log.info("INSERTED TMDB Movie ID {}", wrapper.getTmdbId());
            extractAndSaveRelations(newMovie, wrapper);
        } else {
            // UPDATE flow with timestamp check
            if (existingMovie.getTmdbLastUpdated() == null || 
               (wrapper.getLastUpdated() != null && wrapper.getLastUpdated().isAfter(existingMovie.getTmdbLastUpdated()))) {
                
                log.info("Attempting to update TMDB ID: {}", wrapper.getTmdbId());
                movieMapper.updateEntityFromDto(wrapper, existingMovie);
                existingMovie = movieRepository.save(existingMovie);
                log.info("UPDATED TMDB Movie ID {}", wrapper.getTmdbId());
                extractAndSaveRelations(existingMovie, wrapper);
            } else {
                log.debug("SKIP TMDB ID {}: Movie data is up to date.", wrapper.getTmdbId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importMovies(List<TmdbMovieWrapperDto> wrappers) {
        if (wrappers == null || wrappers.isEmpty()) return;

        List<TmdbMovieWrapperDto> validWrappers = wrappers.stream()
                .filter(w -> w != null && w.getMovie() != null && "ACCEPT".equalsIgnoreCase(w.getQualityStatus()))
                .collect(Collectors.toList());

        if (validWrappers.isEmpty()) return;

        List<Long> tmdbIds = validWrappers.stream()
                .map(TmdbMovieWrapperDto::getTmdbId)
                .collect(Collectors.toList());

        List<Movie> existingMoviesList = movieRepository.findByTmdbIdIn(tmdbIds);
        Map<Long, Movie> existingMoviesMap = existingMoviesList.stream()
                .collect(Collectors.toMap(Movie::getTmdbId, Function.identity()));

        List<Movie> toSave = new ArrayList<>();

        for (TmdbMovieWrapperDto wrapper : validWrappers) {
            Movie existingMovie = existingMoviesMap.get(wrapper.getTmdbId());

            if (existingMovie == null) {
                // INSERT flow
                log.info("Batch INSERT TMDB ID: {}", wrapper.getTmdbId());
                Movie newMovie = movieMapper.toEntity(wrapper);
                toSave.add(newMovie);
            } else {
                // UPDATE flow
                if (existingMovie.getTmdbLastUpdated() == null ||
                   (wrapper.getLastUpdated() != null && wrapper.getLastUpdated().isAfter(existingMovie.getTmdbLastUpdated()))) {
                    
                    log.info("Batch UPDATE TMDB ID: {}", wrapper.getTmdbId());
                    movieMapper.updateEntityFromDto(wrapper, existingMovie);
                    toSave.add(existingMovie);
                }
            }
        }

        if (!toSave.isEmpty()) {
            movieRepository.saveAll(toSave);
            log.info("Batch saved {} movies.", toSave.size());
            
            // Save relations
            Map<Long, Movie> savedMoviesMap = movieRepository.findByTmdbIdIn(tmdbIds).stream()
                    .collect(Collectors.toMap(Movie::getTmdbId, Function.identity()));
            for (TmdbMovieWrapperDto wrapper : validWrappers) {
                Movie movie = savedMoviesMap.get(wrapper.getTmdbId());
                if (movie != null) {
                    extractAndSaveRelations(movie, wrapper);
                }
            }
        }
    }

    private void extractAndSaveRelations(Movie movie, TmdbMovieWrapperDto wrapper) {
        if (wrapper == null) return;
        
        // 1. Genres
        if (wrapper.getGenres() != null && !wrapper.getGenres().isEmpty()) {
            movieGenreRepository.deleteByMovieId(movie.getId());
            for (TmdbGenreDto gDto : wrapper.getGenres()) {
                if (gDto.getName() == null) continue;
                String slug = movieMapper.generateSlug(gDto.getName());
                Genre genre = genreRepository.findBySlugAndDeletedAtIsNull(slug).orElseGet(() -> {
                    Genre newGenre = new Genre();
                    newGenre.setPublicId(UUID.randomUUID().toString());
                    newGenre.setName(gDto.getName());
                    newGenre.setSlug(slug);
                    newGenre.setStatus(ActiveStatus.ACTIVE);
                    return genreRepository.save(newGenre);
                });
                
                MovieGenre mg = new MovieGenre();
                mg.setMovie(movie);
                mg.setGenre(genre);
                movieGenreRepository.save(mg);
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
                    List<Person> existingPersons = personRepository.findByTmdbPersonIdInAndDeletedAtIsNull(personIds);
                    for (Person p : existingPersons) {
                        personMap.put(p.getTmdbPersonId(), p);
                    }
                    
                    List<Person> newPersons = new ArrayList<>();
                    for (TmdbPersonDto pDto : uniquePersons.values()) {
                        if (!personMap.containsKey(pDto.getTmdbPersonId())) {
                            Person newPerson = new Person();
                            newPerson.setPublicId(UUID.randomUUID().toString());
                            newPerson.setTmdbPersonId(pDto.getTmdbPersonId());
                            newPerson.setFullName(pDto.getOriginalName() != null ? pDto.getOriginalName() : (pDto.getName() != null ? pDto.getName() : "Unknown"));
                            newPerson.setStageName(pDto.getName());
                            newPerson.setProfileImageUrl(pDto.getProfileUrl());
                            newPerson.setStatus(ActiveStatus.ACTIVE);
                            newPersons.add(newPerson);
                        }
                    }
                    if (!newPersons.isEmpty()) {
                        newPersons = personRepository.saveAll(newPersons);
                        for (Person p : newPersons) {
                            personMap.put(p.getTmdbPersonId(), p);
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
                        credit.setCharacterName(tc.dto.getCharacter());
                        credit.setDisplayOrder(tc.order);
                        creditsToSave.add(credit);
                    }
                }
                if (!creditsToSave.isEmpty()) {
                    movieCreditRepository.saveAll(creditsToSave);
                }
            }
        }
        
        // 3. Videos / Media
        if (wrapper.getVideos() != null && wrapper.getVideos().getPrimaryTrailer() != null) {
            TmdbTrailerDto trailer = wrapper.getVideos().getPrimaryTrailer();
            if (trailer.getUrl() != null) {
                // Check if primary trailer already exists
                boolean exists = movieMediaRepository.existsPrimaryMedia(movie.getId(), MovieMediaType.TRAILER);
                if (!exists) {
                    MovieMedia media = new MovieMedia();
                    media.setPublicId(UUID.randomUUID().toString());
                    media.setMovie(movie);
                    media.setMediaType(MovieMediaType.TRAILER);
                    media.setUrl(trailer.getUrl());
                    media.setTitle(trailer.getName() != null ? trailer.getName() : "Official Trailer");
                    media.setIsPrimary(true);
                    media.setStatus(ActiveStatus.ACTIVE);
                    media.setDisplayOrder(1);
                    movieMediaRepository.save(media);
                }
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
                    media.setUrl(img.getUrl());
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
                    media.setUrl(img.getUrl());
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
                    newCompany.setName(companyDto.getName());
                    newCompany.setLogoUrl(companyDto.getLogoUrl());
                    newCompany.setCountry(companyDto.getOriginCountry());
                    newCompany.setStatus(ActiveStatus.ACTIVE);
                    return productionCompanyRepository.save(newCompany);
                });
                
                MovieProductionCompany mpc = new MovieProductionCompany();
                mpc.setMovie(movie);
                mpc.setProductionCompany(company);
                // mpc.setRole(...) // Adjust according to enum if needed, usually defaults to PRODUCTION in SQL
                movieProductionCompanyRepository.save(mpc);
            }
        }
        
        // 5. Translations
        if (wrapper.getTranslations() != null && !wrapper.getTranslations().isEmpty()) {
            movieTranslationRepository.deleteByMovieId(movie.getId());
            for (com.lorafilm.movie.integration.tmdb.dto.TmdbTranslationDto tDto : wrapper.getTranslations()) {
                if (tDto.getLanguageCode() == null) continue;
                MovieTranslation mt = new MovieTranslation();
                mt.setMovie(movie);
                mt.setLocale(tDto.getLanguageCode());
                mt.setTitle(tDto.getTitle() != null ? tDto.getTitle() : movie.getTitle());
                mt.setSynopsis(tDto.getOverview());
                movieTranslationRepository.save(mt);
            }
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
