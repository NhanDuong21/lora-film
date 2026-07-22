package com.lorafilm.movie.autoschedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerateRequestNormalizer;
import com.lorafilm.movie.autoschedule.service.AutoScheduleRequestFingerprintService;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.cinema.scheduler.CinemaStatusScheduler;
import com.lorafilm.movie.integration.tmdb.scheduler.TmdbPersonSyncScheduler;
import com.lorafilm.movie.integration.tmdb.scheduler.TmdbSyncScheduler;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminAutoScheduleGenerateE2ETest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://localhost:3307/movie_db_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "12345678");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShowtimeSchedulePreviewRepository previewRepository;

    @Autowired
    private ShowtimeSchedulePreviewItemRepository itemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private AutoScheduleGenerateRequestNormalizer normalizer;

    @Autowired
    private AutoScheduleRequestFingerprintService fingerprintService;

    @MockBean
    private TmdbSyncScheduler tmdbSyncScheduler;

    @MockBean
    private TmdbPersonSyncScheduler tmdbPersonSyncScheduler;

    @MockBean
    private CinemaStatusScheduler cinemaStatusScheduler;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
        previewRepository.deleteAll();

        // Ensure we don't have dupes if tests share the DB
        jdbcTemplate.execute("DELETE FROM movie_versions WHERE id = 8888");
        jdbcTemplate.execute("DELETE FROM movies WHERE id = 8888");
        jdbcTemplate.execute("DELETE FROM auditoriums WHERE id = 8888");
        jdbcTemplate.execute("DELETE FROM cinema_operating_hours WHERE cinema_id = 8888");
        jdbcTemplate.execute("DELETE FROM cinemas WHERE id = 8888");

        jdbcTemplate.update("INSERT INTO cinemas (id, public_id, name, slug, city, address, timezone, status, created_at, updated_at) VALUES (8888, 'c-gen-1', 'Cinema Gen 1', 'cinema-gen-1', 'HCMC', '123 Street', 'Asia/Ho_Chi_Minh', 'ACTIVE', NOW(), NOW())");
        
        // Add operating hours for Cinema 8888 (Every day 08:00 - 23:00)
        for (int i = 1; i <= 7; i++) {
            jdbcTemplate.update("INSERT INTO cinema_operating_hours (cinema_id, day_of_week, open_time, close_time, is_closed, created_at, updated_at) VALUES (8888, ?, '08:00:00', '23:00:00', false, NOW(), NOW())", i);
        }

        jdbcTemplate.update("INSERT INTO auditoriums (id, cinema_id, public_id, name, capacity, cleaning_buffer_minutes, status, screen_type, sound_type, created_at, updated_at) VALUES (8888, 8888, 'a-gen-1', 'Aud Gen 1', 100, 15, 'ACTIVE', 'STANDARD', 'STANDARD', NOW(), NOW())");
        jdbcTemplate.update("INSERT INTO movies (id, public_id, title, slug, duration_minutes, age_rating, release_date, status, created_at, updated_at) VALUES (8888, 'm-gen-1', 'Movie Gen 1', 'movie-gen-1', 120, 'T18', '2025-01-01', 'NOW_SHOWING', NOW(), NOW())");
        jdbcTemplate.update("INSERT INTO movie_versions (id, movie_id, public_id, version_name, format, audio_language, status, created_at, updated_at) VALUES (8888, 8888, 'mv-gen-1', 'Version Gen 1', 'TWO_D', 'ENG', 'ACTIVE', NOW(), NOW())");
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void testGeneratePreview_HappyPath() throws Exception {
        GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
        request.setCinemaPublicId("c-gen-1");
        request.setScheduleFrom(LocalDate.now());
        request.setScheduleTo(LocalDate.now());
        request.setAuditoriumPublicIds(List.of("a-gen-1"));
        request.setMovieVersionPublicIds(List.of("mv-gen-1"));
        request.setSlotGranularityMinutes(30);
        request.setPreviewTtlMinutes(60);
        
        String idempotencyKey = UUID.randomUUID().toString();
        request.setIdempotencyKey(idempotencyKey);

        mockMvc.perform(post("/api/admin/showtime-schedules/generate-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREVIEWED"));

        // Verify Database
        Optional<ShowtimeSchedulePreview> previewOpt = previewRepository.findByGenerateIdempotencyKey(idempotencyKey);
        assertTrue(previewOpt.isPresent());
        ShowtimeSchedulePreview p = previewOpt.get();
        assertEquals(SchedulePreviewStatus.PREVIEWED, p.getStatus());
        assertEquals(27, p.getTotalCandidateCount());
        System.out.println("=== DB STATE EVIDENCE ===");
        System.out.println("PREVIEW ROW: ID=" + p.getId() + 
            ", PublicID=" + p.getPublicId() + 
            ", Status=" + p.getStatus() + 
            ", Strategy=" + p.getStrategy() + 
            ", StrategyVersion=" + p.getStrategyVersion() +
            ", ApplyMode=" + p.getApplyMode() +
            ", TotalCandidates=" + p.getTotalCandidateCount() + 
            ", ValidCandidates=" + p.getValidCandidateCount() +
            ", RejectedCandidates=" + p.getRejectedCandidateCount() +
            ", SelectedCandidates=" + p.getSelectedCandidateCount() +
            ", Selected=" + p.getSelectedCandidateCount() + 
            ", RequestFingerprint=" + p.getRequestFingerprint() +
            ", TimezoneSnapshot=" + p.getTimezoneSnapshot() +
            ", GeneratedBy=" + p.getGeneratedBy() +
            ", ExpiresAt=" + p.getExpiresAt() + 
            ", GeneratedAt=" + p.getGeneratedAt() +
            ", Version=" + p.getVersion());

        // Validate Preview Invariants
        assertEquals(p.getTotalCandidateCount(), p.getValidCandidateCount() + p.getRejectedCandidateCount());
        assertTrue(p.getSelectedCandidateCount() <= p.getValidCandidateCount());
        assertEquals(AutoScheduleStrategyVersions.CURRENT, p.getStrategyVersion());
        assertEquals("ALL_OR_NOTHING", p.getApplyMode().name());
        assertNotNull(p.getRequestFingerprint());
        assertEquals(64, p.getRequestFingerprint().length());
        assertEquals(1L, p.getGeneratedBy());
        assertTrue(p.getExpiresAt().isAfter(p.getGeneratedAt()));

            
        List<ShowtimeSchedulePreviewItem> items = itemRepository.findAll();
        assertEquals(27, items.size());
        assertEquals(27, p.getValidCandidateCount());
        assertEquals(0, p.getRejectedCandidateCount());
        for (ShowtimeSchedulePreviewItem item : items) {
            System.out.println("ITEM ROW: PublicID=" + item.getPublicId() + 
                ", ValidationStatus=" + item.getValidationStatus() + 
                ", Selected=" + item.getSelected() + 
                ", Score=" + item.getScore() + 
                ", RankingPosition=" + item.getRankingPosition() + 
                ", RejectionReason=" + item.getRejectionReason() +
                ", StartTime=" + item.getStartTime() +
                ", EndTime=" + item.getEndTime() +
                ", OccupancyEndTime=" + item.getOccupancyEndTime());
        }

        System.out.println("=== JSON PERSISTENCE VERIFICATION ===");
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        System.out.println("VERSION()=" + version);
        System.out.println("DATABASE()=" + database);
        if (!items.isEmpty()) {
            ShowtimeSchedulePreviewItem firstItem = items.get(0);
            String jsonType = jdbcTemplate.queryForObject("SELECT JSON_TYPE(score_breakdown_json) FROM showtime_schedule_preview_items WHERE id = ?", String.class, firstItem.getId());
            String baseScore = jdbcTemplate.queryForObject("SELECT JSON_EXTRACT(score_breakdown_json, '$.base') FROM showtime_schedule_preview_items WHERE id = ?", String.class, firstItem.getId());
            String rawJson = jdbcTemplate.queryForObject("SELECT score_breakdown_json FROM showtime_schedule_preview_items WHERE id = ?", String.class, firstItem.getId());
            System.out.println("JSON_TYPE=" + jsonType);
            System.out.println("JSON_EXTRACT(base)=" + baseScore);
            System.out.println("RAW_JSON=" + rawJson);
        }

        System.out.println("=== END DB STATE EVIDENCE ===");
    }
    
    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void testGeneratePreview_IdempotencyReplay() throws Exception {
        GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
        request.setCinemaPublicId("c-gen-1");
        request.setScheduleFrom(LocalDate.now());
        request.setScheduleTo(LocalDate.now());
        request.setAuditoriumPublicIds(List.of("a-gen-1"));
        request.setMovieVersionPublicIds(List.of("mv-gen-1"));
        request.setSlotGranularityMinutes(30);
        request.setPreviewTtlMinutes(60);
        
        String idempotencyKey = UUID.randomUUID().toString();
        request.setIdempotencyKey(idempotencyKey);

        // First call
        mockMvc.perform(post("/api/admin/showtime-schedules/generate-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Replay with exact same request
        mockMvc.perform(post("/api/admin/showtime-schedules/generate-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
                
        // Only one preview should exist with this key
        long count = previewRepository.findAll().stream().filter(p -> idempotencyKey.equals(p.getGenerateIdempotencyKey())).count();
        assertEquals(1L, count);
    }
    
    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void testGeneratePreview_IdempotencyConflict() throws Exception {
        GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
        request.setCinemaPublicId("c-gen-1");
        request.setScheduleFrom(LocalDate.now());
        request.setScheduleTo(LocalDate.now());
        request.setAuditoriumPublicIds(List.of("a-gen-1"));
        request.setMovieVersionPublicIds(List.of("mv-gen-1"));
        request.setSlotGranularityMinutes(30);
        request.setPreviewTtlMinutes(60);
        
        String idempotencyKey = UUID.randomUUID().toString();
        request.setIdempotencyKey(idempotencyKey);

        // First call
        mockMvc.perform(post("/api/admin/showtime-schedules/generate-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Change request but keep same key
        request.setSlotGranularityMinutes(15);
        
        mockMvc.perform(post("/api/admin/showtime-schedules/generate-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void legacyV1ReplayIsVersionAwareImmutableAndNewKeyCreatesS2() throws Exception {
        GenerateShowtimeSchedulePreviewRequest legacyRequest = requestWithKey("legacy-" + UUID.randomUUID());
        var normalized = normalizer.normalize(legacyRequest);
        String legacyFingerprint = fingerprintService.generateFingerprint(
                normalized, AutoScheduleStrategyVersions.LEGACY_BALANCED_V1);
        var cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull("c-gen-1").orElseThrow();
        ShowtimeSchedulePreview legacy = ShowtimeSchedulePreview.createGenerating(
                cinema, normalized.getScheduleFrom(), normalized.getScheduleTo(),
                normalized.getSlotGranularityMinutes(), normalized.getPreviewTtlMinutes(),
                normalized.getIdempotencyKey(), legacyFingerprint, 1L,
                java.time.Instant.parse("2026-01-01T00:00:00Z"));
        legacy.setStrategyVersion(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1);
        legacy.setStatus(SchedulePreviewStatus.PREVIEWED);
        legacy.setTotalCandidateCount(7);
        legacy.setValidCandidateCount(5);
        legacy.setRejectedCandidateCount(2);
        legacy.setSelectedCandidateCount(3);
        legacy = previewRepository.saveAndFlush(legacy);

        Long legacyId = legacy.getId();
        Long legacyEntityVersion = legacy.getVersion();
        String legacyPublicId = legacy.getPublicId();
        java.time.Instant legacyGeneratedAt = legacy.getGeneratedAt();

        mockMvc.perform(post("/api/admin/showtime-schedules/generate-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(legacyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewPublicId").value(legacyPublicId));

        GenerateShowtimeSchedulePreviewRequest changed = requestWithKey(legacyRequest.getIdempotencyKey());
        changed.setSlotGranularityMinutes(15);
        mockMvc.perform(post("/api/admin/showtime-schedules/generate-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changed)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REUSED"));

        GenerateShowtimeSchedulePreviewRequest newRequest = requestWithKey("s2-" + UUID.randomUUID());
        mockMvc.perform(post("/api/admin/showtime-schedules/generate-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRequest)))
                .andExpect(status().isOk());
        ShowtimeSchedulePreview created = previewRepository
                .findByGenerateIdempotencyKey(newRequest.getIdempotencyKey()).orElseThrow();
        assertEquals(AutoScheduleStrategyVersions.CURRENT, created.getStrategyVersion());

        ShowtimeSchedulePreview unchanged = previewRepository.findById(legacyId).orElseThrow();
        assertEquals(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1, unchanged.getStrategyVersion());
        assertEquals(legacyFingerprint, unchanged.getRequestFingerprint());
        assertEquals(legacyEntityVersion, unchanged.getVersion());
        assertEquals(legacyGeneratedAt, unchanged.getGeneratedAt());
        assertEquals(7, unchanged.getTotalCandidateCount());
        assertEquals(5, unchanged.getValidCandidateCount());
        assertEquals(2, unchanged.getRejectedCandidateCount());
        assertEquals(3, unchanged.getSelectedCandidateCount());
        assertTrue(itemRepository.findAllByPreviewIdOrderByRankingPositionAscIdAsc(legacyId).isEmpty(),
                "legacy replay must not regenerate old preview items");
    }

    private GenerateShowtimeSchedulePreviewRequest requestWithKey(String idempotencyKey) {
        GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
        request.setCinemaPublicId("c-gen-1");
        request.setScheduleFrom(LocalDate.now());
        request.setScheduleTo(LocalDate.now());
        request.setAuditoriumPublicIds(List.of("a-gen-1"));
        request.setMovieVersionPublicIds(List.of("mv-gen-1"));
        request.setSlotGranularityMinutes(30);
        request.setPreviewTtlMinutes(60);
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }
}
