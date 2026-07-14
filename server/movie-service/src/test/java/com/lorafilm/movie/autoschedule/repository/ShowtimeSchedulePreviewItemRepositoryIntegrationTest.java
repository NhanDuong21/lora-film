package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.*;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewTestFactory;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@org.springframework.context.annotation.Import(com.lorafilm.movie.common.config.AuditConfig.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://127.0.0.1:3307/movie_db_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
    "spring.datasource.username=root",
    "spring.datasource.password=12345678",
    "spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.jpa.hibernate.ddl-auto=update"
})
public class ShowtimeSchedulePreviewItemRepositoryIntegrationTest {

    @Autowired
    private ShowtimeSchedulePreviewItemRepository itemRepository;

    @Autowired
    private ShowtimeSchedulePreviewRepository previewRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieVersionRepository movieVersionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Cinema cinema;
    private Auditorium auditorium;
    private Movie movie;
    private MovieVersion movieVersion;
    private ShowtimeSchedulePreview preview;

    @BeforeEach
    void setUp() {
        cinema = new Cinema();
        cinema.setName("Cinema " + UUID.randomUUID());
        cinema.setSlug("cinema-" + UUID.randomUUID());
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema.setAddress("123 Street");
        cinema.setCity("HCM");
        cinema.setDistrict("Q1");
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema = cinemaRepository.saveAndFlush(cinema);

        auditorium = new Auditorium();
        auditorium.setCinema(cinema);
        auditorium.setName("Auditorium " + UUID.randomUUID());
        auditorium.setCapacity(100);
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium = auditoriumRepository.saveAndFlush(auditorium);

        movie = new Movie();
        movie.setTitle("Test Movie " + UUID.randomUUID());
        movie.setSlug("test-movie-" + UUID.randomUUID());
        movie.setDurationMinutes(120);
        movie.setAgeRating(AgeRating.T18);
        movie.setReleaseDate(LocalDate.now().minusDays(10));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setPublicId(UUID.randomUUID().toString());
        movie = movieRepository.saveAndFlush(movie);

        movieVersion = new MovieVersion();
        movieVersion.setMovie(movie);
        movieVersion.setVersionName("2D SUB");
        movieVersion.setFormat(MovieFormat.TWO_D);
        movieVersion.setAudioLanguage("EN");
        movieVersion.setSubtitleLanguage("VI");
        movieVersion.setStatus(ActiveStatus.ACTIVE);
        movieVersion.setPublicId(UUID.randomUUID().toString());
        movieVersion = movieVersionRepository.saveAndFlush(movieVersion);

        preview = ShowtimeSchedulePreviewTestFactory.createPreview();
        preview.setPublicId(UUID.randomUUID().toString());
        preview.setCinema(cinema);
        preview.setScheduleFrom(LocalDate.now());
        preview.setScheduleTo(LocalDate.now().plusDays(7));
        preview.setTimezoneSnapshot("Asia/Ho_Chi_Minh");
        preview.setStrategy(AutoScheduleStrategy.BALANCED);
        preview.setStrategyVersion("BALANCED_V1");
        preview.setApplyMode(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        preview.setStatus(SchedulePreviewStatus.GENERATING);
        preview.setSlotGranularityMinutes(15);
        preview.setTotalCandidateCount(0);
        preview.setValidCandidateCount(0);
        preview.setRejectedCandidateCount(0);
        preview.setSelectedCandidateCount(0);
        preview.setGeneratedAt(Instant.now());
        preview.setExpiresAt(Instant.now().plusSeconds(3600));
        preview.setGeneratedBy(1001L);
        preview.setGenerateIdempotencyKey(UUID.randomUUID().toString());
        preview.setRequestFingerprint("0".repeat(64));
        preview = previewRepository.saveAndFlush(preview);
    }

    private ShowtimeSchedulePreviewItem createValidItem() {
        ShowtimeSchedulePreviewItem item = ShowtimeSchedulePreviewTestFactory.createItem();
        item.setPublicId(UUID.randomUUID().toString());
        item.setPreview(preview);
        item.setMovie(movie);
        item.setMovieVersion(movieVersion);
        item.setCinema(cinema);
        item.setAuditorium(auditorium);
        item.setStartTime(Instant.now());
        item.setEndTime(Instant.now().plusSeconds(7200));
        item.setOccupancyEndTime(Instant.now().plusSeconds(8100));
        item.setScore(new java.math.BigDecimal("85.500"));
        item.setRankingPosition(1);
        item.setValidationStatus(PreviewItemValidationStatus.VALID);
        item.setSelected(false);
        item.setApplyStatus(PreviewItemApplyStatus.PENDING);
        return item;
    }

    @Test
    void ITEM_REPO_001_persistValidItem() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        item.setSelected(true);
        itemRepository.saveAndFlush(item);
        entityManager.clear();
        ShowtimeSchedulePreviewItem saved = itemRepository.findById(item.getId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void ITEM_REPO_002_persistRejectedItem() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        item.setValidationStatus(PreviewItemValidationStatus.REJECTED);
        item.setSelected(false);
        item.setRejectionCode("TIME_CONFLICT");
        item.setRejectionReason("Overlap with existing");
        ShowtimeSchedulePreviewItem saved = itemRepository.saveAndFlush(item);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void ITEM_REPO_003_rejectedSelectedItemRejectedByDB() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        item.setValidationStatus(PreviewItemValidationStatus.REJECTED);
        item.setSelected(true); // invalid combo
        item.setRejectionCode("ERR");
        
        assertThrows(Exception.class, () -> {
            itemRepository.saveAndFlush(item);
        });
    }

    @Test
    void ITEM_REPO_004_findByPublicId() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        itemRepository.saveAndFlush(item);
        entityManager.clear();

        Optional<ShowtimeSchedulePreviewItem> found = itemRepository.findByPublicId(item.getPublicId());
        assertThat(found).isPresent();
    }

    @Test
    void ITEM_REPO_005_rankingOrder() {
        ShowtimeSchedulePreviewItem item1 = createValidItem();
        item1.setRankingPosition(3);
        item1.setStartTime(Instant.now().plusSeconds(1000));
        itemRepository.save(item1);

        ShowtimeSchedulePreviewItem item2 = createValidItem();
        item2.setRankingPosition(1);
        item2.setStartTime(Instant.now().plusSeconds(2000));
        itemRepository.save(item2);

        ShowtimeSchedulePreviewItem item3 = createValidItem();
        item3.setRankingPosition(2);
        item3.setStartTime(Instant.now().plusSeconds(3000));
        itemRepository.save(item3);
        
        itemRepository.flush();

        List<ShowtimeSchedulePreviewItem> items = itemRepository.findAllByPreviewIdOrderByRankingPositionAscIdAsc(preview.getId());
        
        assertThat(items).hasSize(3);
        assertThat(items.get(0).getRankingPosition()).isEqualTo(1);
        assertThat(items.get(1).getRankingPosition()).isEqualTo(2);
        assertThat(items.get(2).getRankingPosition()).isEqualTo(3);
    }

    @Test
    void ITEM_REPO_006_selectedValidPendingQuery() {
        ShowtimeSchedulePreviewItem i1 = createValidItem();
        i1.setValidationStatus(PreviewItemValidationStatus.VALID);
        i1.setSelected(true);
        i1.setApplyStatus(PreviewItemApplyStatus.PENDING);
        i1.setStartTime(Instant.now().plusSeconds(1000));
        itemRepository.save(i1);

        ShowtimeSchedulePreviewItem i2 = createValidItem();
        i2.setValidationStatus(PreviewItemValidationStatus.VALID);
        i2.setSelected(false);
        i2.setApplyStatus(PreviewItemApplyStatus.PENDING);
        i2.setStartTime(Instant.now().plusSeconds(2000));
        itemRepository.save(i2);

        ShowtimeSchedulePreviewItem i3 = createValidItem();
        i3.setValidationStatus(PreviewItemValidationStatus.REJECTED);
        i3.setSelected(false);
        i3.setApplyStatus(PreviewItemApplyStatus.PENDING);
        i3.setStartTime(Instant.now().plusSeconds(3000));
        itemRepository.save(i3);

        ShowtimeSchedulePreviewItem i4 = createValidItem();
        i4.setValidationStatus(PreviewItemValidationStatus.VALID);
        i4.setSelected(true);
        i4.setApplyStatus(PreviewItemApplyStatus.CREATED);
        i4.setStartTime(Instant.now().plusSeconds(4000));
        itemRepository.save(i4);

        itemRepository.flush();

        List<ShowtimeSchedulePreviewItem> found = itemRepository.findAllByPreviewIdAndSelectedTrueAndValidationStatusAndApplyStatus(
                preview.getId(), PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(i1.getId());
    }

    @Test
    void ITEM_REPO_007_uniquePreviewAuditoriumStartTime() {
        ShowtimeSchedulePreviewItem i1 = createValidItem();
        itemRepository.saveAndFlush(i1);

        ShowtimeSchedulePreviewItem i2 = createValidItem();
        i2.setStartTime(i1.getStartTime());
        
        MovieVersion mv2 = new MovieVersion();
        mv2.setMovie(movie);
        mv2.setVersionName("2D DUB");
        mv2.setFormat(MovieFormat.TWO_D);
        mv2.setAudioLanguage("VI");
        mv2.setStatus(ActiveStatus.ACTIVE);
        mv2.setPublicId(UUID.randomUUID().toString());
        mv2 = movieVersionRepository.saveAndFlush(mv2);
        
        i2.setMovieVersion(mv2);

        assertThrows(DataIntegrityViolationException.class, () -> {
            itemRepository.saveAndFlush(i2);
        });
    }

    @Test
    void ITEM_REPO_008_sameSlotInDifferentPreviews() {
        ShowtimeSchedulePreviewItem i1 = createValidItem();
        itemRepository.saveAndFlush(i1);

        ShowtimeSchedulePreview preview2 = ShowtimeSchedulePreviewTestFactory.createPreview();
        preview2.setPublicId(UUID.randomUUID().toString());
        preview2.setCinema(cinema);
        preview2.setScheduleFrom(LocalDate.now());
        preview2.setScheduleTo(LocalDate.now().plusDays(7));
        preview2.setTimezoneSnapshot("Asia/Ho_Chi_Minh");
        preview2.setStrategy(AutoScheduleStrategy.BALANCED);
        preview2.setStrategyVersion("1.0");
        preview2.setApplyMode(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        preview2.setStatus(SchedulePreviewStatus.GENERATING);
        preview2.setSlotGranularityMinutes(15);
        preview2.setTotalCandidateCount(0);
        preview2.setValidCandidateCount(0);
        preview2.setRejectedCandidateCount(0);
        preview2.setSelectedCandidateCount(0);
        preview2.setGeneratedAt(Instant.now());
        preview2.setExpiresAt(Instant.now().plusSeconds(3600));
        preview2.setGeneratedBy(1001L);
        preview2.setGenerateIdempotencyKey(UUID.randomUUID().toString());
        preview2.setRequestFingerprint("fingerprint");
        preview2 = previewRepository.saveAndFlush(preview2);

        ShowtimeSchedulePreviewItem i2 = createValidItem();
        i2.setPreview(preview2);
        i2.setStartTime(i1.getStartTime());

        ShowtimeSchedulePreviewItem saved = itemRepository.saveAndFlush(i2);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void ITEM_REPO_009_jsonScoreBreakdown() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        String json = "{\"primeTime\": 90, \"utilization\": 80, \"fairness\": 70}";
        item.setScoreBreakdownJson(json);
        itemRepository.saveAndFlush(item);
        
        entityManager.clear();

        ShowtimeSchedulePreviewItem found = itemRepository.findById(item.getId()).orElseThrow();
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            assertThat(mapper.readTree(found.getScoreBreakdownJson())).isEqualTo(mapper.readTree(json));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void ITEM_REPO_010_createdShowtimeNullable() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        item.setCreatedShowtime(null);
        ShowtimeSchedulePreviewItem saved = itemRepository.saveAndFlush(item);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void ITEM_REPO_011_createdShowtimeRelation() {
        Showtime showtime = new Showtime();
        showtime.setPublicId(UUID.randomUUID().toString());
        showtime.setMovie(movie);
        showtime.setMovieVersion(movieVersion);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(Instant.now().plusSeconds(3600));
        showtime.setEndTime(Instant.now().plusSeconds(7200));
        showtime.setStatus(com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus.DRAFT);
        showtime = showtimeRepository.saveAndFlush(showtime);

        ShowtimeSchedulePreviewItem item = createValidItem();
        item.setCreatedShowtime(showtime);
        itemRepository.saveAndFlush(item);
        entityManager.clear();

        ShowtimeSchedulePreviewItem found = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(found.getCreatedShowtime()).isNotNull();
        assertThat(found.getCreatedShowtime().getId()).isEqualTo(showtime.getId());

        itemRepository.delete(found);
        itemRepository.flush();

        assertThat(showtimeRepository.existsById(showtime.getId())).isTrue();
    }

    @Test
    void ITEM_REPO_012_previewCascadeItems() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        preview.addItem(item);
        
        previewRepository.saveAndFlush(preview);
        entityManager.clear();
        
        ShowtimeSchedulePreviewItem saved = itemRepository.findByPublicId(item.getPublicId()).orElseThrow();
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void ITEM_REPO_013_cascadeDeletePreviewItems() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        preview.addItem(item);
        preview = previewRepository.saveAndFlush(preview);
        
        // Retrieve the generated ID by public ID
        ShowtimeSchedulePreviewItem savedItem = itemRepository.findByPublicId(item.getPublicId()).orElseThrow();
        Long itemId = savedItem.getId();
        
        assertThat(itemRepository.existsById(itemId)).isTrue();
        
        previewRepository.delete(preview);
        previewRepository.flush();
        
        assertThat(itemRepository.existsById(itemId)).isFalse();
        
        assertThat(movieRepository.existsById(movie.getId())).isTrue();
        assertThat(cinemaRepository.existsById(cinema.getId())).isTrue();
        assertThat(auditoriumRepository.existsById(auditorium.getId())).isTrue();
    }

    @Test
    void ITEM_REPO_014_detailedFetchQuery() {
        ShowtimeSchedulePreviewItem item = createValidItem();
        itemRepository.saveAndFlush(item);
        entityManager.clear();

        List<ShowtimeSchedulePreviewItem> detailed = itemRepository.findDetailedItemsByPreviewId(preview.getId());
        assertThat(detailed).hasSize(1);
        
        ShowtimeSchedulePreviewItem fetched = detailed.get(0);
        assertThat(fetched.getMovie().getId()).isEqualTo(movie.getId());
        assertThat(fetched.getMovieVersion().getId()).isEqualTo(movieVersion.getId());
        assertThat(fetched.getCinema().getId()).isEqualTo(cinema.getId());
        assertThat(fetched.getAuditorium().getId()).isEqualTo(auditorium.getId());
    }
}
