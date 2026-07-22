package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ShowtimeSchedulePreviewPaginationIntegrationTest {

    @MockBean
    private com.lorafilm.movie.common.security.CurrentUserProvider currentUserProvider;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://localhost:3307/movie_db_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "12345678");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private ShowtimeSchedulePreviewRepository previewRepository;

    @Autowired
    private ShowtimeSchedulePreviewItemRepository itemRepository;

    @Autowired
    private com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService previewService;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieVersionRepository movieVersionRepository;

    private ShowtimeSchedulePreview preview;
    private MovieVersion movieVersion;
    private Auditorium auditorium;
    private Movie movie;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Cinema cinema = new Cinema();
        cinema.setName("Test Cinema");
        cinema.setPublicId(java.util.UUID.randomUUID().toString());
        cinema.setSlug(java.util.UUID.randomUUID().toString());
        cinema.setCity("Test City");
        cinema.setAddress("Test Address");
        cinema.setStatus(com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE);
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinemaRepository.save(cinema);

        preview = ShowtimeSchedulePreview.createGenerating(
                cinema,
                LocalDate.of(2023, 10, 1),
                LocalDate.of(2023, 10, 1),
                30,
                60,
                java.util.UUID.randomUUID().toString(),
                "fingerprint",
                1L,
                Instant.now()
        );
        preview.markPreviewed();
        previewRepository.save(preview);
        
        auditorium = new Auditorium();
        auditorium.setPublicId(java.util.UUID.randomUUID().toString());
        auditorium.setName("Test Auditorium");
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCinema(cinema);
        auditorium.setCapacity(100);
        auditorium.setCleaningBufferMinutes(15);
        auditoriumRepository.save(auditorium);

        movie = new Movie();
        movie.setPublicId(java.util.UUID.randomUUID().toString());
        movie.setSlug(java.util.UUID.randomUUID().toString());
        movie.setTitle("Test Movie");
        movie.setOriginalTitle("Original Movie");
        movie.setSynopsis("Synopsis");
        movie.setAgeRating(com.lorafilm.movie.movie.domain.enums.AgeRating.T13);
        movie.setCountry("US");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.now().minusDays(1));
        movie.setEndDate(LocalDate.now().plusDays(60));
        movieRepository.save(movie);

        movieVersion = new MovieVersion();
        movieVersion.setPublicId(java.util.UUID.randomUUID().toString());
        movieVersion.setVersionName("2D SUB");
        movieVersion.setMovie(movie);
        movieVersion.setFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat.TWO_D);
        movieVersion.setAudioLanguage("EN");
        movieVersion.setStatus(ActiveStatus.ACTIVE);
        movieVersionRepository.save(movieVersion);
        
        // Add 250 dummy items for testing pagination
        for (int i = 0; i < 250; i++) {
            ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
            item.setPreview(preview);
            item.setPublicId(java.util.UUID.randomUUID().toString());
            item.setRankingPosition(i + 1);
            // 2023-10-01 10:00:00 UTC (17:00:00 Asia/Ho_Chi_Minh) + i hours
            item.setStartTime(Instant.parse("2023-10-01T10:00:00Z").plus(i, ChronoUnit.HOURS));
            item.setEndTime(item.getStartTime().plus(2, ChronoUnit.HOURS));
            item.setOccupancyEndTime(item.getEndTime().plus(15, ChronoUnit.MINUTES));
            if (i % 2 == 0) {
                item.setServiceDate(LocalDate.of(2023, 10, 1).plusDays(i / 24));
            }
            item.setMovie(movie);
            item.setMovieVersion(movieVersion);
            item.setCinema(cinema);
            item.setAuditorium(auditorium);
            item.setValidationStatus(PreviewItemValidationStatus.VALID);
            item.setApplyStatus(PreviewItemApplyStatus.PENDING);
            item.setScore(java.math.BigDecimal.ZERO);
            item.setSelected(false);
            itemRepository.save(item);
        }
        
            itemRepository.flush();
        });
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE showtime_schedule_preview_items");
        jdbcTemplate.execute("TRUNCATE TABLE showtime_schedule_previews");
        jdbcTemplate.execute("TRUNCATE TABLE auditoriums");
        jdbcTemplate.execute("TRUNCATE TABLE cinema_operating_hours");
        jdbcTemplate.execute("TRUNCATE TABLE cinemas");
        jdbcTemplate.execute("TRUNCATE TABLE movie_versions");
        jdbcTemplate.execute("TRUNCATE TABLE movies");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    void shouldPaginateCorrectly() {
        ShowtimeSchedulePreviewItemQuery query = new ShowtimeSchedulePreviewItemQuery();
        Specification<ShowtimeSchedulePreviewItem> spec = ShowtimeSchedulePreviewItemSpecification.filterBy(
                preview.getId(),
                query,
                preview.getTimezoneSnapshot()
        );

        // Page 1 (size 50)
        Page<ShowtimeSchedulePreviewItem> page1 = itemRepository.findAll(spec, PageRequest.of(0, 50));
        assertThat(page1.getTotalElements()).isEqualTo(250);
        assertThat(page1.getTotalPages()).isEqualTo(5);
        assertThat(page1.getContent()).hasSize(50);
        assertThat(page1.getNumber()).isEqualTo(0);
        assertThat(page1.isFirst()).isTrue();
        assertThat(page1.isLast()).isFalse();
        assertThat(page1.getContent()).anyMatch(item -> item.getServiceDate() != null);
        assertThat(page1.getContent()).anyMatch(item -> item.getServiceDate() == null);

        // Page 2 (size 50) - verify different content
        Page<ShowtimeSchedulePreviewItem> page2 = itemRepository.findAll(spec, PageRequest.of(1, 50));
        assertThat(page2.getContent()).hasSize(50);
        assertThat(page2.getNumber()).isEqualTo(1);
        assertThat(page1.getContent().get(0).getId()).isNotEqualTo(page2.getContent().get(0).getId());

        // Page 5 (size 50)
        Page<ShowtimeSchedulePreviewItem> page5 = itemRepository.findAll(spec, PageRequest.of(4, 50));
        assertThat(page5.getContent()).hasSize(50);
        assertThat(page5.getNumber()).isEqualTo(4);
        assertThat(page5.isFirst()).isFalse();
        assertThat(page5.isLast()).isTrue();
    }

    @Test
    void shouldFilterByDateAndSortByStartTime() {
        ShowtimeSchedulePreviewItemQuery query = new ShowtimeSchedulePreviewItemQuery();
        // Filter by specific date in Asia/Ho_Chi_Minh
        // First item starts at 2023-10-01T17:00:00+07:00
        // Items from i=0 to i=6 are on 2023-10-01 (17:00 to 23:00)
        query.setDate(LocalDate.of(2023, 10, 1));
        
        Specification<ShowtimeSchedulePreviewItem> spec = ShowtimeSchedulePreviewItemSpecification.filterBy(
                preview.getId(),
                query,
                preview.getTimezoneSnapshot()
        );

        Sort sort = Sort.by(Sort.Direction.DESC, "startTime");
        Page<ShowtimeSchedulePreviewItem> result = itemRepository.findAll(spec, PageRequest.of(0, 10, sort));
        
        // 7 items (i=0 to 6)
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getContent()).anyMatch(item -> item.getServiceDate() != null);
        assertThat(result.getContent()).anyMatch(item -> item.getServiceDate() == null);
        assertThat(result.getContent().get(0).getRankingPosition()).isEqualTo(7); // Because DESC sort and rank was i+1
        assertThat(result.getContent().get(6).getRankingPosition()).isEqualTo(1);
    }

    @Test
    void testSwapSelection_IncrementsVersion() {
        // Find 2 items
        List<ShowtimeSchedulePreviewItem> items = itemRepository.findAllByPreviewIdOrderByRankingPositionAscIdAsc(preview.getId()).subList(0, 2);
        ShowtimeSchedulePreviewItem item1 = items.get(0);
        ShowtimeSchedulePreviewItem item2 = items.get(1);

        // Make item1 selected = true, item2 selected = false initially
        item1.setSelected(true);
        item2.setSelected(false);
        transactionTemplate.executeWithoutResult(status -> {
            itemRepository.saveAll(List.of(item1, item2));
            itemRepository.flush();
            ShowtimeSchedulePreview p = previewRepository.findById(preview.getId()).orElseThrow();
            p.setSelectedCandidateCount(1);
            previewRepository.saveAndFlush(p);
        });

        ShowtimeSchedulePreview currentPreview = previewRepository.findById(preview.getId()).orElseThrow();
        Long oldVersion = currentPreview.getVersion();
        
        com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest req1 = new com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest(item1.getPublicId(), false);
        com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest req2 = new com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest(item2.getPublicId(), true);

        com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest request = new com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(oldVersion);
        request.setItems(List.of(req1, req2));

        org.mockito.Mockito.when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        previewService.updateSelections(preview.getPublicId(), request);

        // Fetch again from DB
        ShowtimeSchedulePreview updatedPreview = previewRepository.findById(preview.getId()).orElseThrow();
        
        // Assert version increased exactly by 1
        assertThat(updatedPreview.getVersion()).isEqualTo(oldVersion + 1);
        
        // Assert selected count didn't change but selections swapped
        ShowtimeSchedulePreviewItem updatedItem1 = itemRepository.findById(item1.getId()).orElseThrow();
        ShowtimeSchedulePreviewItem updatedItem2 = itemRepository.findById(item2.getId()).orElseThrow();
        assertThat(updatedItem1.getSelected()).isFalse();
        assertThat(updatedItem2.getSelected()).isTrue();
    }

    @Test
    void shouldHandleExpiryCorrectly() {
        ShowtimeSchedulePreviewItemQuery query = new ShowtimeSchedulePreviewItemQuery();
        var response1 = previewService.getPreview(preview.getPublicId(), query);
        assertThat(response1.getPreview().getStatus()).isEqualTo(com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus.PREVIEWED);

        jdbcTemplate.execute("UPDATE showtime_schedule_previews SET generated_at = DATE_SUB(NOW(), INTERVAL 60 MINUTE), expires_at = DATE_SUB(NOW(), INTERVAL 30 MINUTE) WHERE id = " + preview.getId());

        var response2 = previewService.getPreview(preview.getPublicId(), query);
        assertThat(response2.getPreview().getStatus()).isEqualTo(com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus.EXPIRED);
        assertThat(response2.getItems().getTotalElements()).isEqualTo(250);

        com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest req = new com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest();
        req.setExpectedVersion(response2.getPreview().getVersion());
        req.setItems(List.of());
        org.junit.jupiter.api.Assertions.assertThrows(
            com.lorafilm.movie.common.exception.BusinessException.class,
            () -> previewService.updateSelections(preview.getPublicId(), req)
        );
    }
}
