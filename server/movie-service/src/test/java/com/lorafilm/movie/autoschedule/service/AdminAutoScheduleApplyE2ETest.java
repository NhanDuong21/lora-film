package com.lorafilm.movie.autoschedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.ApplyShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.domain.enums.PriceDayType;
import com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus;
import com.lorafilm.movie.pricing.domain.enums.PricingSource;
import com.lorafilm.movie.pricing.repository.PricePolicyRepository;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public class AdminAutoScheduleApplyE2ETest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("movie_db_test")
            .withUsername("root")
            .withPassword("12345678");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ShowtimeSchedulePreviewRepository previewRepository;
    @Autowired
    private ShowtimeSchedulePreviewItemRepository itemRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private CinemaRepository cinemaRepository;
    @Autowired
    private AuditoriumRepository auditoriumRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private MovieVersionRepository movieVersionRepository;
    @Autowired
    private SeatTypeRepository seatTypeRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private PricePolicyRepository pricePolicyRepository;
    @Autowired
    private ShowtimePriceRepository showtimePriceRepository;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    private String previewPublicId;
    private Long initialVersion;
    private SeatType standardSeatType;
    private PricePolicy standardPolicy;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM cinema_operating_hours");
        jdbcTemplate.execute("DELETE FROM showtime_status_history");
        jdbcTemplate.execute("DELETE FROM showtime_prices");
        itemRepository.deleteAllInBatch();
        previewRepository.deleteAllInBatch();
        jdbcTemplate.execute("DELETE FROM showtimes");
        jdbcTemplate.execute("DELETE FROM price_policy_rules");
        jdbcTemplate.execute("DELETE FROM price_policies");
        jdbcTemplate.execute("DELETE FROM seats");
        jdbcTemplate.execute("DELETE FROM seat_types");
        auditoriumRepository.deleteAllInBatch();
        cinemaRepository.deleteAllInBatch();
        movieVersionRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();

        Cinema cinema = new Cinema();
        cinema.setPublicId("CINEMA_" + UUID.randomUUID().toString().substring(0, 8));
        cinema.setSlug("cinema-" + System.currentTimeMillis());
        cinema.setName("E2E Cinema");
        cinema.setCity("Test City");
        cinema.setAddress("Test Address");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema = cinemaRepository.saveAndFlush(cinema);

        for (int i = 1; i <= 7; i++) {
            jdbcTemplate.update("INSERT INTO cinema_operating_hours (cinema_id, day_of_week, open_time, close_time, is_closed, created_at, updated_at) VALUES (?, ?, '00:00:00', '23:59:59', false, NOW(), NOW())", cinema.getId(), i);
        }

        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId("AUD_" + UUID.randomUUID().toString().substring(0, 8));
        auditorium.setName("E2E Auditorium");
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCinema(cinema);
        auditorium.setCapacity(100);
        auditorium.setCleaningBufferMinutes(15);
        auditorium = auditoriumRepository.saveAndFlush(auditorium);

        standardSeatType = new SeatType();
        standardSeatType.setPublicId(UUID.randomUUID().toString());
        standardSeatType.setCode(SeatTypeCode.STANDARD);
        standardSeatType.setName("Standard");
        standardSeatType.setStatus(ActiveStatus.ACTIVE);
        standardSeatType = seatTypeRepository.saveAndFlush(standardSeatType);

        Seat seat = new Seat();
        seat.setPublicId(UUID.randomUUID().toString());
        seat.setAuditorium(auditorium);
        seat.setSeatType(standardSeatType);
        seat.setRowLabel("A");
        seat.setSeatNumber(1);
        seat.setSeatCode("A1");
        seat.setPositionRow(1);
        seat.setPositionColumn(1);
        seat.setStatus(SeatStatus.ACTIVE);
        seatRepository.saveAndFlush(seat);

        standardPolicy = new PricePolicy();
        standardPolicy.setPublicId(UUID.randomUUID().toString());
        standardPolicy.setName("Auto Schedule E2E policy");
        standardPolicy.setCinema(cinema);
        standardPolicy.setEffectiveFrom(LocalDate.now().minusDays(1));
        standardPolicy.setEffectiveTo(LocalDate.now().plusDays(60));
        standardPolicy.setStatus(PricePolicyStatus.ACTIVE);
        standardPolicy.setCurrency("VND");
        standardPolicy.setPriority(0);
        standardPolicy.setActivatedAt(Instant.now());
        standardPolicy.setActivatedBy(999L);

        PricePolicyRule standardRule = new PricePolicyRule();
        standardRule.setPublicId(UUID.randomUUID().toString());
        standardRule.setSeatType(standardSeatType);
        standardRule.setDayType(PriceDayType.ALL_DAYS);
        standardRule.setPrice(new BigDecimal("75000.00"));
        standardRule.setActive(true);
        standardPolicy.addRule(standardRule);
        standardPolicy = pricePolicyRepository.saveAndFlush(standardPolicy);

        Movie movie = new Movie();
        movie.setPublicId("MOVIE_" + UUID.randomUUID().toString().substring(0, 8));
        movie.setSlug("movie-" + System.currentTimeMillis());
        movie.setTitle("E2E Movie");
        movie.setOriginalTitle("Original Title");
        movie.setSynopsis("Test Synopsis");
        movie.setAgeRating(com.lorafilm.movie.movie.domain.enums.AgeRating.T13);
        movie.setCountry("US");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.now().minusDays(1));
        movie.setEndDate(LocalDate.now().plusDays(60));
        movie = movieRepository.saveAndFlush(movie);

        MovieVersion version = new MovieVersion();
        version.setPublicId("VER_" + UUID.randomUUID().toString().substring(0, 8));
        version.setVersionName("2D SUB");
        version.setMovie(movie);
        version.setFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat.TWO_D);
        version.setAudioLanguage("Vietnamese");
        version.setStatus(ActiveStatus.ACTIVE);
        version = movieVersionRepository.saveAndFlush(version);

        ShowtimeSchedulePreview preview = ShowtimeSchedulePreview.createGenerating(
                cinema, LocalDate.now(), LocalDate.now(), 15, 60, "hash123", "fingerprint", 1L, Instant.now()
        );
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "publicId", "PREVIEW_" + UUID.randomUUID().toString().substring(0, 8));
        preview = previewRepository.saveAndFlush(preview);
        preview.markPreviewed();
        preview.setSelectedCandidateCount(1);
        preview = previewRepository.saveAndFlush(preview);

        previewPublicId = preview.getPublicId();
        initialVersion = preview.getVersion();

        Instant baseTime = Instant.now().plus(1, ChronoUnit.DAYS);

        // Valid Item 1
        ShowtimeCandidate c1 = new ShowtimeCandidate();
        c1.setMovie(movie);
        c1.setMovieVersion(version);
        c1.setCinema(cinema);
        c1.setAuditorium(auditorium);
        c1.setStartTime(baseTime);
        c1.setEndTime(baseTime.plus(120, ChronoUnit.MINUTES));
        c1.setOccupancyEndTime(baseTime.plus(135, ChronoUnit.MINUTES));
        c1.setScore(BigDecimal.TEN);
        c1.setScoreBreakdown(Map.of("test", BigDecimal.TEN));
        c1.setRankingPosition(1);
        c1.setValidationStatus(PreviewItemValidationStatus.VALID);
        c1.setSelected(true);
        ShowtimeSchedulePreviewItem item1 = ShowtimeSchedulePreviewItem.createItem(preview, c1);
        org.springframework.test.util.ReflectionTestUtils.setField(item1, "publicId", "ITEM_1_" + UUID.randomUUID().toString().substring(0, 8));
        itemRepository.saveAndFlush(item1);

        when(currentUserProvider.getCurrentUserId()).thenReturn(999L);
    }
    
    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM cinema_operating_hours");
        jdbcTemplate.execute("DELETE FROM showtime_status_history");
        jdbcTemplate.execute("DELETE FROM showtime_prices");
        itemRepository.deleteAllInBatch();
        previewRepository.deleteAllInBatch();
        jdbcTemplate.execute("DELETE FROM showtimes");
        jdbcTemplate.execute("DELETE FROM price_policy_rules");
        jdbcTemplate.execute("DELETE FROM price_policies");
        jdbcTemplate.execute("DELETE FROM seats");
        jdbcTemplate.execute("DELETE FROM seat_types");
        auditoriumRepository.deleteAllInBatch();
        cinemaRepository.deleteAllInBatch();
        movieVersionRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
    }

    @Test
    void runAllE2ETestsForReport() throws Exception {
        System.out.println("\n========== E2E EVIDENCE START ==========\n");
        int showtimeCountBeforeApply =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        int priceCountBeforeApply =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtime_prices", Integer.class);
        System.out.printf(
                "Scenario A counts before apply: showtimes=%d, showtime_prices=%d%n",
                showtimeCountBeforeApply,
                priceCountBeforeApply);
        
        System.out.println("--- 5. Happy-path E2E đầy đủ ---");
        ApplyShowtimeSchedulePreviewRequest req = new ApplyShowtimeSchedulePreviewRequest();
        req.setExpectedVersion(initialVersion);
        req.setIdempotencyKey("apply-phase5-e2e-001");
        
        System.out.println("Request:\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(req));
        
        MvcResult res = mockMvc.perform(post("/api/admin/showtime-schedules/{previewPublicId}/apply", previewPublicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
                
        System.out.println("Response:\n" + res.getResponse().getContentAsString());
        
        System.out.println("\n--- 6. DB evidence sau apply thành công ---");
        System.out.println("Preview:");
        List<Map<String, Object>> previewRows = jdbcTemplate.queryForList(
                "SELECT public_id, status, selected_candidate_count, applied_at, applied_by, apply_idempotency_key, version FROM showtime_schedule_previews WHERE public_id = ?", previewPublicId);
        previewRows.forEach(System.out::println);
        
        System.out.println("Items:");
        List<Map<String, Object>> itemRows = jdbcTemplate.queryForList(
                "SELECT public_id, selected, validation_status, apply_status, created_showtime_id, apply_error_code, apply_error_message FROM showtime_schedule_preview_items WHERE preview_id = (SELECT id FROM showtime_schedule_previews WHERE public_id = ?) ORDER BY ranking_position, id", previewPublicId);
        itemRows.forEach(System.out::println);
        
        System.out.println("Showtimes:");
        List<Map<String, Object>> showtimeRows = jdbcTemplate.queryForList(
                "SELECT public_id, movie_id, movie_version_id, cinema_id, auditorium_id, start_time, end_time, status FROM showtimes WHERE id IN (SELECT created_showtime_id FROM showtime_schedule_preview_items WHERE preview_id = (SELECT id FROM showtime_schedule_previews WHERE public_id = ?))", previewPublicId);
        showtimeRows.forEach(System.out::println);

        Showtime createdShowtime = showtimeRepository.findAll().stream()
                .filter(showtime -> showtime.getBatchId() != null)
                .findFirst()
                .orElseThrow();
        List<ShowtimePrice> snapshots =
                showtimePriceRepository.findByShowtimeIdWithSeatType(createdShowtime.getId());
        assertThat(snapshots).hasSize(1);
        ShowtimePrice snapshot = snapshots.get(0);
        assertThat(snapshot.getPrice()).isEqualByComparingTo("75000.00");
        assertThat(snapshot.getCurrency()).isEqualTo("VND");
        assertThat(snapshot.getSeatType().getPublicId()).isEqualTo(standardSeatType.getPublicId());
        assertThat(snapshot.getSeatTypeNameSnapshot()).isEqualTo("Standard");
        assertThat(snapshot.getSeatTypeCodeSnapshot()).isEqualTo("STANDARD");
        assertThat(snapshot.getPricingSource()).isEqualTo(PricingSource.POLICY);
        assertThat(snapshot.getResolutionTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(snapshot.getResolvedAt()).isNotNull();
        assertThat(snapshot.getSourcePolicy().getPublicId()).isEqualTo(standardPolicy.getPublicId());
        assertThat(snapshot.getSourceRule()).isNotNull();
        System.out.println(
                "Scenario A immutable snapshot row:\n"
                        + jdbcTemplate.queryForMap(
                                """
                                SELECT sp.price,
                                       sp.currency,
                                       sp.seat_type_name_snapshot,
                                       sp.seat_type_code_snapshot,
                                       sp.pricing_source,
                                       sp.resolution_timezone,
                                       sp.resolved_at,
                                       pp.public_id AS source_policy_public_id,
                                       ppr.public_id AS source_rule_public_id
                                  FROM showtime_prices sp
                                  JOIN price_policies pp ON pp.id = sp.source_policy_id
                                  JOIN price_policy_rules ppr ON ppr.id = sp.source_rule_id
                                 WHERE sp.showtime_id = ?
                                """,
                                createdShowtime.getId()));

        int showtimeCountAfterApply =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        int priceCountAfterApply =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtime_prices", Integer.class);
        System.out.printf(
                "Scenario A counts after apply: showtimes=%d, showtime_prices=%d%n",
                showtimeCountAfterApply,
                priceCountAfterApply);
        assertThat(showtimeCountAfterApply - showtimeCountBeforeApply).isEqualTo(1);
        assertThat(priceCountAfterApply - priceCountBeforeApply).isEqualTo(1);

        MvcResult openPreviewResult = mockMvc.perform(
                        get("/api/admin/showtimes/batch/{batchId}/status-preview",
                                createdShowtime.getBatchId())
                                .param("targetStatus", "OPEN_FOR_BOOKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.eligibleCount").value(1))
                .andExpect(jsonPath("$.data.skippedCount").value(0))
                .andExpect(jsonPath("$.data.actionAllowed").value(true))
                .andReturn();
        System.out.println(
                "Scenario A OPEN preview response:\n"
                        + openPreviewResult.getResponse()
                                .getContentAsString(StandardCharsets.UTF_8));
        
        System.out.println("\n--- 8. Idempotency replay evidence ---");
        MvcResult replayRes = mockMvc.perform(post("/api/admin/showtime-schedules/{previewPublicId}/apply", previewPublicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        System.out.println("Lần 2 Replay Response:\n" + replayRes.getResponse().getContentAsString());
        int showtimeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        System.out.println("Showtime count sau replay: " + showtimeCount);
        
        System.out.println("\n--- 11. Stale version ---");
        ApplyShowtimeSchedulePreviewRequest staleReq = new ApplyShowtimeSchedulePreviewRequest();
        staleReq.setExpectedVersion(initialVersion - 1);
        staleReq.setIdempotencyKey("apply-phase5-e2e-002");
        
        MvcResult staleRes = mockMvc.perform(post("/api/admin/showtime-schedules/{previewPublicId}/apply", previewPublicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(staleReq)))
                .andReturn();
        System.out.println("Stale Version Response:\n" + staleRes.getResponse().getContentAsString());
        
        System.out.println("\n========== E2E EVIDENCE END ==========");
    }

    @Test
    void missingPricingFailsApplyPreflightBeforeCreatingAnyShowtime() throws Exception {
        jdbcTemplate.execute("DELETE FROM price_policy_rules");
        jdbcTemplate.execute("DELETE FROM price_policies");

        int showtimeCountBeforeApply =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        int priceCountBeforeApply =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtime_prices", Integer.class);
        System.out.printf(
                "Scenario B counts before apply: showtimes=%d, showtime_prices=%d%n",
                showtimeCountBeforeApply,
                priceCountBeforeApply);

        ApplyShowtimeSchedulePreviewRequest request = new ApplyShowtimeSchedulePreviewRequest();
        request.setExpectedVersion(initialVersion);
        request.setIdempotencyKey("apply-missing-pricing-e2e");

        MvcResult firstAttempt = mockMvc.perform(
                        post("/api/admin/showtime-schedules/{previewPublicId}/apply", previewPublicId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PRICING_INCOMPLETE"))
                .andExpect(jsonPath("$.data.complete").value(false))
                .andExpect(jsonPath("$.data.totalCandidateCount").value(1))
                .andExpect(jsonPath("$.data.completeCandidateCount").value(0))
                .andExpect(jsonPath("$.data.incompleteCandidateCount").value(1))
                .andExpect(jsonPath("$.data.reasonGroups[0].reasonCode").value("PRICING_INCOMPLETE"))
                .andExpect(jsonPath("$.data.reasonGroups[0].displayMessage")
                        .value("Thiếu chính sách hoặc quy tắc giá hiệu lực cho một hoặc nhiều loại ghế."))
                .andExpect(jsonPath("$.data.reasonGroups[0].count").value(1))
                .andExpect(jsonPath("$.data.reasonGroups[0].auditoriums[0].name")
                        .value("E2E Auditorium"))
                .andExpect(jsonPath("$.data.reasonGroups[0].seatTypes[0].name")
                        .value(standardSeatType.getName()))
                .andReturn();
        System.out.println(
                "Scenario B first HTTP 409 response:\n"
                        + firstAttempt.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(showtimeRepository.findAll().stream()
                .filter(showtime -> showtime.getBatchId() != null)).isEmpty();
        int showtimeCountAfterFirstAttempt =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        int priceCountAfterFirstAttempt =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtime_prices", Integer.class);
        assertThat(showtimeCountAfterFirstAttempt).isEqualTo(showtimeCountBeforeApply);
        assertThat(priceCountAfterFirstAttempt).isEqualTo(priceCountBeforeApply);
        assertThat(previewRepository.findByPublicId(previewPublicId).orElseThrow().getStatus())
                .isEqualTo(SchedulePreviewStatus.PREVIEWED);
        Long previewId = previewRepository.findByPublicId(previewPublicId).orElseThrow().getId();
        assertThat(itemRepository.findDetailedItemsByPreviewId(previewId).getFirst().getApplyStatus())
                .isEqualTo(PreviewItemApplyStatus.PENDING);

        MvcResult retryAttempt = mockMvc.perform(
                        post("/api/admin/showtime-schedules/{previewPublicId}/apply", previewPublicId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRICING_INCOMPLETE"))
                .andExpect(jsonPath("$.data.incompleteCandidateCount").value(1))
                .andReturn();
        System.out.println(
                "Scenario B retry HTTP 409 response:\n"
                        + retryAttempt.getResponse().getContentAsString(StandardCharsets.UTF_8));

        int showtimeCountAfterRetry =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        int priceCountAfterRetry =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtime_prices", Integer.class);
        SchedulePreviewStatus previewStatusAfterRetry =
                previewRepository.findByPublicId(previewPublicId).orElseThrow().getStatus();
        PreviewItemApplyStatus itemStatusAfterRetry =
                itemRepository.findDetailedItemsByPreviewId(previewId).getFirst().getApplyStatus();
        System.out.printf(
                "Scenario B counts/status after retry: showtimes=%d, showtime_prices=%d, "
                        + "preview=%s, item=%s%n",
                showtimeCountAfterRetry,
                priceCountAfterRetry,
                previewStatusAfterRetry,
                itemStatusAfterRetry);
        assertThat(showtimeCountAfterRetry).isEqualTo(showtimeCountBeforeApply);
        assertThat(priceCountAfterRetry).isEqualTo(priceCountBeforeApply);
        assertThat(previewStatusAfterRetry).isEqualTo(SchedulePreviewStatus.PREVIEWED);
        assertThat(itemStatusAfterRetry).isEqualTo(PreviewItemApplyStatus.PENDING);
    }
    
    @Test
    void runAtomicRollbackE2ETest() throws Exception {
        System.out.println("\n--- 7. Atomic rollback evidence ---");
        // Create an overlapping showtime right before applying to simulate revalidation conflict
        Instant baseTime = Instant.now().plus(1, ChronoUnit.DAYS);
        Long cinemaId = jdbcTemplate.queryForObject("SELECT id FROM cinemas LIMIT 1", Long.class);
        Long auditoriumId = jdbcTemplate.queryForObject("SELECT id FROM auditoriums LIMIT 1", Long.class);
        Long movieId = jdbcTemplate.queryForObject("SELECT id FROM movies LIMIT 1", Long.class);
        Long movieVersionId = jdbcTemplate.queryForObject("SELECT id FROM movie_versions LIMIT 1", Long.class);
        
        jdbcTemplate.update("INSERT INTO showtimes (public_id, cinema_id, auditorium_id, movie_id, movie_version_id, start_time, end_time, booking_open_time, booking_close_time, status, version, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', 0, NOW(), NOW())",
                "ST_CONFLICT", cinemaId, auditoriumId, movieId, movieVersionId, baseTime, baseTime.plus(120, ChronoUnit.MINUTES), baseTime.minus(7, ChronoUnit.DAYS), baseTime);

        int countBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        System.out.println("Showtimes Count Before Apply: " + countBefore);

        ApplyShowtimeSchedulePreviewRequest req = new ApplyShowtimeSchedulePreviewRequest();
        req.setExpectedVersion(initialVersion);
        req.setIdempotencyKey("apply-phase5-e2e-rollback");
        
        MvcResult res = mockMvc.perform(post("/api/admin/showtime-schedules/{previewPublicId}/apply", previewPublicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
                
        System.out.println("Response:\n" + res.getResponse().getContentAsString());
        
        int countAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        System.out.println("Showtimes Count After Apply: " + countAfter);
        
        System.out.println("Preview:");
        List<Map<String, Object>> previewRows = jdbcTemplate.queryForList(
                "SELECT public_id, status, applied_at, applied_by, apply_idempotency_key FROM showtime_schedule_previews WHERE public_id = ?", previewPublicId);
        previewRows.forEach(System.out::println);
    }
    
    @Test
    void runExpiredPreviewE2ETest() throws Exception {
        System.out.println("\n--- 10. Expired Preview ---");
        // Update the preview to be expired
        jdbcTemplate.update("UPDATE showtime_schedule_previews SET status = 'PREVIEWED', expires_at = ? WHERE public_id = ?", 
                Instant.now().minus(1, ChronoUnit.HOURS), previewPublicId);
                
        ApplyShowtimeSchedulePreviewRequest req = new ApplyShowtimeSchedulePreviewRequest();
        req.setExpectedVersion(initialVersion);
        req.setIdempotencyKey("apply-phase5-e2e-expired");
        
        System.out.println("Trước Apply: status = PREVIEWED, expiresAt <= now");
        
        MvcResult res = mockMvc.perform(post("/api/admin/showtime-schedules/{previewPublicId}/apply", previewPublicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
                
        System.out.println("Response:\n" + res.getResponse().getContentAsString());
        
        System.out.println("DB status sau request:");
        List<Map<String, Object>> previewRows = jdbcTemplate.queryForList(
                "SELECT public_id, status FROM showtime_schedule_previews WHERE public_id = ?", previewPublicId);
        previewRows.forEach(System.out::println);
        
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM showtimes", Integer.class);
        System.out.println("0 Showtime mới: (Showtimes count = " + count + ")");
    }
}
