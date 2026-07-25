package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.config.AuditConfig;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieSummaryResponse;
import com.lorafilm.movie.movie.service.MovieHealthFacts;
import com.lorafilm.movie.movie.service.AdminMovieProjectionService;
import com.lorafilm.movie.movie.service.MovieReadinessEvaluator;
import com.lorafilm.movie.movie.service.MovieSummaryQueryService;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({
        AuditConfig.class,
        MovieReadinessEvaluator.class,
        MovieSummaryQueryService.class,
        AdminMovieProjectionService.class,
        MovieMapper.class
})
@DataJpaTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@ActiveProfiles("test")
class MovieHealthSpecificationsIntegrationTest {

    @Autowired private MovieRepository movieRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private MovieGenreRepository movieGenreRepository;
    @Autowired private MovieVersionRepository movieVersionRepository;
    @Autowired private MovieMediaRepository movieMediaRepository;
    @Autowired private CinemaRepository cinemaRepository;
    @Autowired private AuditoriumRepository auditoriumRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private MovieReadinessEvaluator evaluator;
    @Autowired private MovieSummaryQueryService summaryQueryService;
    @Autowired private AdminMovieProjectionService projectionService;
    @Autowired private jakarta.persistence.EntityManager entityManager;
    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    private Genre genre;
    private Movie ready;
    private Movie warningDuration;
    private Movie warningBlankTitle;
    private Movie warningInvalidDuration;
    private Movie blockedGenre;
    private Movie blockedVersion;
    private Movie blockedPoster;

    @BeforeEach
    void setUp() {
        genre = new Genre();
        genre.setPublicId(UUID.randomUUID().toString());
        genre.setName("Health Test Genre");
        genre.setSlug("health-test-genre-" + UUID.randomUUID());
        genre.setStatus(ActiveStatus.ACTIVE);
        genre = genreRepository.save(genre);

        ready = createHealthyMovie("ready", MovieStatus.UPCOMING, 120, true, true, true);
        ready.setTmdbId(1001L);
        ready.setTmdbLastUpdated(java.time.LocalDateTime.of(2026, 7, 20, 14, 30));
        ready.setReleaseDate(LocalDate.of(2026, 7, 20));
        ready.setCountry("United States");
        movieRepository.save(ready);
        warningDuration = createHealthyMovie("warning-duration", MovieStatus.NOW_SHOWING, 15, true, true, true);
        warningBlankTitle = createHealthyMovie("warning-title", MovieStatus.ENDED, 120, true, true, true);
        warningBlankTitle.setTitle("   ");
        movieRepository.save(warningBlankTitle);
        warningInvalidDuration = createHealthyMovie("warning-invalid-duration", MovieStatus.INACTIVE, 0, true, true, true);
        blockedGenre = createHealthyMovie("blocked-genre", MovieStatus.DRAFT, 120, false, true, true);
        blockedVersion = createHealthyMovie("blocked-version", MovieStatus.DRAFT, 120, true, false, true);
        blockedPoster = createHealthyMovie("blocked-poster", MovieStatus.DRAFT, 120, true, true, false);

        MovieVersion deletedVersion = createVersion(blockedVersion);
        deletedVersion.setDeletedAt(Instant.now());
        movieVersionRepository.save(deletedVersion);
        MovieMedia deletedPoster = createPoster(blockedPoster);
        deletedPoster.setDeletedAt(Instant.now());
        movieMediaRepository.save(deletedPoster);

        Movie deletedMovie = createHealthyMovie("deleted-ready", MovieStatus.DRAFT, 120, true, true, true);
        deletedMovie.setDeletedAt(Instant.now());
        movieRepository.save(deletedMovie);

        createShowtimes();
    }

    @Test
    void databaseHealthBucketsMatchCanonicalEvaluator() {
        assertParity(ready, true, true, true, MovieHealthStatus.READY);
        assertParity(warningDuration, true, true, true, MovieHealthStatus.WARNING);
        assertParity(warningBlankTitle, true, true, true, MovieHealthStatus.WARNING);
        assertParity(warningInvalidDuration, true, true, true, MovieHealthStatus.WARNING);
        assertParity(blockedGenre, false, true, true, MovieHealthStatus.BLOCKED);
        assertParity(blockedVersion, true, false, true, MovieHealthStatus.BLOCKED);
        assertParity(blockedPoster, true, true, false, MovieHealthStatus.BLOCKED);

        assertEquals(Set.of(ready.getPublicId()), selected(MovieHealthStatus.READY));
        assertEquals(Set.of(
                        warningDuration.getPublicId(),
                        warningBlankTitle.getPublicId(),
                        warningInvalidDuration.getPublicId()),
                selected(MovieHealthStatus.WARNING));
        assertEquals(Set.of(
                        blockedGenre.getPublicId(),
                        blockedVersion.getPublicId(),
                        blockedPoster.getPublicId()),
                selected(MovieHealthStatus.BLOCKED));
    }

    @Test
    void summaryPartitionsGlobalNonDeletedPopulation() {
        entityManager.flush();
        entityManager.clear();
        org.hibernate.stat.Statistics statistics = entityManagerFactory
                .unwrap(org.hibernate.SessionFactory.class)
                .getStatistics();
        statistics.clear();

        MovieSummaryResponse summary = summaryQueryService.getSummary();

        assertEquals(7, summary.total());
        assertEquals(3, summary.draft());
        assertEquals(1, summary.upcoming());
        assertEquals(1, summary.nowShowing());
        assertEquals(1, summary.ended());
        assertEquals(1, summary.inactive());
        assertEquals(1, summary.ready());
        assertEquals(3, summary.warning());
        assertEquals(3, summary.blocked());
        assertEquals(summary.total(), summary.ready() + summary.warning() + summary.blocked());
        assertEquals(1, summary.missingPrimaryPoster());
        assertEquals(1, summary.missingActiveVersion());
        assertEquals(6, summary.withoutShowtime());
        assertEquals(6, statistics.getPrepareStatementCount(), "Summary must execute exactly six SQL statements");
    }

    @Test
    void summaryReturnsTwelveZeroMetricsForAnEmptyDatabase() {
        showtimeRepository.deleteAllInBatch();
        movieGenreRepository.deleteAllInBatch();
        movieMediaRepository.deleteAllInBatch();
        movieVersionRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();

        org.hibernate.stat.Statistics statistics = entityManagerFactory
                .unwrap(org.hibernate.SessionFactory.class)
                .getStatistics();
        statistics.clear();

        MovieSummaryResponse summary = summaryQueryService.getSummary();

        assertEquals(new MovieSummaryResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), summary);
        assertEquals(6, statistics.getPrepareStatementCount(), "Empty summary must retain the six-query contract");
    }

    @Test
    void enrichedNonEmptyPageUsesEightQueriesWithoutPerRowGrowth() {
        entityManager.flush();
        entityManager.clear();
        org.hibernate.stat.Statistics statistics = entityManagerFactory
                .unwrap(org.hibernate.SessionFactory.class)
                .getStatistics();
        statistics.clear();

        Page<Movie> page = movieRepository.findAll(
                MovieSpecification.isNotDeleted(),
                PageRequest.of(0, 5, Sort.by("releaseDate").descending().and(Sort.by("id").descending())));
        projectionService.enrichMovies(page);

        assertEquals(8, statistics.getPrepareStatementCount(),
                "List page must use content + count + six batch enrichment queries");
    }

    @Test
    void showtimePredicateUsesAnyNonDeletedShowtimeRegardlessOfStatus() {
        Set<String> withShowtime = movieRepository.findAll(
                        MovieSpecification.isNotDeleted().and(MovieSpecification.hasShowtime(true)))
                .stream().map(Movie::getPublicId).collect(Collectors.toSet());
        Set<String> withoutShowtime = movieRepository.findAll(
                        MovieSpecification.isNotDeleted().and(MovieSpecification.hasShowtime(false)))
                .stream().map(Movie::getPublicId).collect(Collectors.toSet());

        assertEquals(Set.of(ready.getPublicId()), withShowtime);
        assertFalse(withoutShowtime.contains(ready.getPublicId()));
        assertTrue(withoutShowtime.contains(warningDuration.getPublicId()),
                "A soft-deleted showtime must not satisfy hasShowtime");
    }

    @Test
    void operationalFiltersUseDatabaseFactsAndComposeBeforePagination() {
        assertEquals(Set.of(ready.getPublicId()), ids(MovieSpecification.hasTmdbSource(true)));
        assertEquals(6, ids(MovieSpecification.hasTmdbSource(false)).size());
        assertEquals(Set.of(ready.getPublicId()), ids(MovieSpecification.hasCountry("united states")));
        assertEquals(Set.of(ready.getPublicId()), ids(MovieSpecification.releaseDateFrom(LocalDate.of(2026, 7, 15))));
        assertEquals(Set.of(), ids(MovieSpecification.releaseDateTo(LocalDate.of(2026, 6, 30))));
        assertEquals(Set.of(ready.getPublicId()), ids(
                MovieSpecification.releaseDateFrom(LocalDate.of(2026, 7, 20))
                        .and(MovieSpecification.releaseDateTo(LocalDate.of(2026, 7, 20)))));
        assertEquals(Set.of(ready.getPublicId()), ids(MovieSpecification.tmdbUpdatedFrom(
                java.time.LocalDateTime.of(2026, 7, 20, 0, 0))));
        assertEquals(Set.of(ready.getPublicId()), ids(MovieSpecification.tmdbUpdatedBefore(
                java.time.LocalDateTime.of(2026, 7, 21, 0, 0))));

        Set<String> genreMatches = ids(MovieSpecification.hasGenrePublicId(genre.getPublicId()));
        assertEquals(6, genreMatches.size());
        assertFalse(genreMatches.contains(blockedGenre.getPublicId()));
        assertEquals(Set.of(blockedPoster.getPublicId()), ids(
                Specification.not(MovieHealthSpecifications.hasActivePrimaryPoster())));
        assertEquals(Set.of(blockedVersion.getPublicId()), ids(
                Specification.not(MovieHealthSpecifications.hasActiveVersion())));

        Specification<Movie> warnings = MovieSpecification.isNotDeleted()
                .and(MovieHealthSpecifications.healthStatusEquals(MovieHealthStatus.WARNING));
        Page<Movie> warningPage = movieRepository.findAll(warnings, PageRequest.of(0, 1));
        assertEquals(3, warningPage.getTotalElements());
        assertEquals(1, warningPage.getContent().size());

        Specification<Movie> combined = MovieSpecification.isNotDeleted()
                .and(MovieSpecification.hasTmdbSource(true))
                .and(MovieHealthSpecifications.healthStatusEquals(MovieHealthStatus.READY))
                .and(MovieSpecification.hasShowtime(true));
        assertEquals(Set.of(ready.getPublicId()), movieRepository.findAll(combined).stream()
                .map(Movie::getPublicId).collect(Collectors.toSet()));
    }

    private void assertParity(
            Movie movie,
            boolean hasGenre,
            boolean hasVersion,
            boolean hasPoster,
            MovieHealthStatus expected) {
        assertEquals(expected, evaluator.evaluate(MovieHealthFacts.from(
                movie, hasGenre, hasVersion, hasPoster)).getHealthStatus());
        assertTrue(selected(expected).contains(movie.getPublicId()));
    }

    private Set<String> selected(MovieHealthStatus status) {
        Specification<Movie> spec = MovieSpecification.isNotDeleted()
                .and(MovieHealthSpecifications.healthStatusEquals(status));
        return movieRepository.findAll(spec).stream()
                .map(Movie::getPublicId)
                .collect(Collectors.toSet());
    }

    private Set<String> ids(Specification<Movie> specification) {
        return movieRepository.findAll(MovieSpecification.isNotDeleted().and(specification)).stream()
                .map(Movie::getPublicId)
                .collect(Collectors.toSet());
    }

    private Movie createHealthyMovie(
            String marker,
            MovieStatus status,
            int duration,
            boolean withGenre,
            boolean withVersion,
            boolean withPoster) {
        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle("Movie " + marker);
        movie.setOriginalTitle("Original " + marker);
        movie.setSlug(marker + "-" + UUID.randomUUID());
        movie.setDurationMinutes(duration);
        movie.setAgeRating(AgeRating.T13);
        movie.setReleaseDate(LocalDate.of(2026, 7, 1));
        movie.setCountry("VN");
        movie.setStatus(status);
        movie = movieRepository.save(movie);

        if (withGenre) {
            MovieGenre movieGenre = new MovieGenre();
            movieGenre.setMovie(movie);
            movieGenre.setGenre(genre);
            movieGenreRepository.save(movieGenre);
        }
        if (withVersion) {
            createVersion(movie);
        }
        if (withPoster) {
            createPoster(movie);
        }
        return movie;
    }

    private MovieVersion createVersion(Movie movie) {
        MovieVersion version = new MovieVersion();
        version.setPublicId(UUID.randomUUID().toString());
        version.setMovie(movie);
        version.setVersionName("2D " + UUID.randomUUID());
        version.setFormat(MovieFormat.TWO_D);
        version.setAudioLanguage("VI");
        version.setSubtitleLanguage(UUID.randomUUID().toString());
        version.setStatus(ActiveStatus.ACTIVE);
        return movieVersionRepository.save(version);
    }

    private MovieMedia createPoster(Movie movie) {
        MovieMedia poster = new MovieMedia();
        poster.setPublicId(UUID.randomUUID().toString());
        poster.setMovie(movie);
        poster.setMediaType(MovieMediaType.POSTER);
        poster.setUrl("https://example.test/" + UUID.randomUUID() + ".jpg");
        poster.setDisplayOrder(0);
        poster.setIsPrimary(true);
        poster.setStatus(ActiveStatus.ACTIVE);
        return movieMediaRepository.save(poster);
    }

    private void createShowtimes() {
        Cinema cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setName("Health Cinema");
        cinema.setSlug("health-cinema-" + UUID.randomUUID());
        cinema.setAddress("1 Test Street");
        cinema.setCity("HCM");
        cinema.setDistrict("Q1");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema = cinemaRepository.save(cinema);

        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinema);
        auditorium.setName("Health Screen");
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCapacity(20);
        auditorium.setScreenType(ScreenType.STANDARD);
        auditorium.setSoundType(SoundType.DOLBY_ATMOS);
        auditorium.setCleaningBufferMinutes(15);
        auditorium = auditoriumRepository.save(auditorium);

        saveShowtime(ready, activeVersion(ready), cinema, auditorium, ShowtimeStatus.CANCELLED, false);
        saveShowtime(warningDuration, activeVersion(warningDuration), cinema, auditorium, ShowtimeStatus.DRAFT, true);
    }

    private MovieVersion activeVersion(Movie movie) {
        return movieVersionRepository.findByMovieIdAndStatusAndDeletedAtIsNull(movie.getId(), ActiveStatus.ACTIVE)
                .stream().findFirst().orElseThrow();
    }

    private void saveShowtime(
            Movie movie,
            MovieVersion version,
            Cinema cinema,
            Auditorium auditorium,
            ShowtimeStatus status,
            boolean deleted) {
        Showtime showtime = new Showtime();
        showtime.setPublicId(UUID.randomUUID().toString());
        showtime.setMovie(movie);
        showtime.setMovieVersion(version);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(Instant.parse("2026-07-15T10:00:00Z"));
        showtime.setEndTime(Instant.parse("2026-07-15T12:00:00Z"));
        showtime.setServiceDate(java.time.LocalDate.of(2026, 7, 15));
        showtime.setStatus(status);
        if (deleted) {
            showtime.setDeletedAt(Instant.now());
        }
        showtimeRepository.save(showtime);
    }
}
