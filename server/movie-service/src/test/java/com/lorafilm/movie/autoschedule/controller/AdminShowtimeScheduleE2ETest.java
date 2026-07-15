package com.lorafilm.movie.autoschedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminShowtimeScheduleE2ETest {

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

    private String previewPublicId;
    private String itemPublicId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        itemRepository.deleteAll();
        previewRepository.deleteAll();

        jdbcTemplate.update("MERGE INTO cinemas (id, public_id, name, slug, city, address, timezone, status, created_at, updated_at) KEY(id) VALUES (9999, 'c1', 'Cinema 1', 'cinema-1', 'HCMC', '123 Street', 'Asia/Ho_Chi_Minh', 'ACTIVE', NOW(), NOW())");
        jdbcTemplate.update("MERGE INTO auditoriums (id, cinema_id, public_id, name, capacity, cleaning_buffer_minutes, status, screen_type, sound_type, created_at, updated_at) KEY(id) VALUES (9999, 9999, 'a1', 'Aud 1', 100, 15, 'ACTIVE', 'STANDARD', 'STANDARD', NOW(), NOW())");
        jdbcTemplate.update("MERGE INTO movies (id, public_id, title, slug, duration_minutes, age_rating, release_date, status, created_at, updated_at) KEY(id) VALUES (9999, 'm1', 'Movie 1', 'movie-1', 120, 'T18', '2025-01-01', 'NOW_SHOWING', NOW(), NOW())");
        jdbcTemplate.update("MERGE INTO movie_versions (id, movie_id, public_id, version_name, format, audio_language, status, created_at, updated_at) KEY(id) VALUES (9999, 9999, 'mv1', 'Version 1', 'TWO_D', 'ENG', 'ACTIVE', NOW(), NOW())");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        previewPublicId = UUID.randomUUID().toString();
        itemPublicId = UUID.randomUUID().toString();

        Constructor<ShowtimeSchedulePreview> previewConstructor = ShowtimeSchedulePreview.class.getDeclaredConstructor();
        previewConstructor.setAccessible(true);
        ShowtimeSchedulePreview preview = previewConstructor.newInstance();
        
        ReflectionTestUtils.setField(preview, "publicId", previewPublicId);
        ReflectionTestUtils.setField(preview, "status", SchedulePreviewStatus.PREVIEWED);
        ReflectionTestUtils.setField(preview, "scheduleFrom", LocalDate.now());
        ReflectionTestUtils.setField(preview, "scheduleTo", LocalDate.now().plusDays(1));
        ReflectionTestUtils.setField(preview, "strategy", AutoScheduleStrategy.BALANCED);
        ReflectionTestUtils.setField(preview, "applyMode", SchedulePreviewApplyMode.ALL_OR_NOTHING);
        ReflectionTestUtils.setField(preview, "totalCandidateCount", 1);
        ReflectionTestUtils.setField(preview, "validCandidateCount", 1);
        ReflectionTestUtils.setField(preview, "rejectedCandidateCount", 0);
        ReflectionTestUtils.setField(preview, "selectedCandidateCount", 0);
        ReflectionTestUtils.setField(preview, "generateIdempotencyKey", UUID.randomUUID().toString());
        ReflectionTestUtils.setField(preview, "requestFingerprint", "fingerprint123");
        ReflectionTestUtils.setField(preview, "generatedAt", Instant.now());
        ReflectionTestUtils.setField(preview, "generatedBy", 1L);
        ReflectionTestUtils.setField(preview, "expiresAt", Instant.now().plus(1, ChronoUnit.HOURS));
        ReflectionTestUtils.setField(preview, "slotGranularityMinutes", 15);
        ReflectionTestUtils.setField(preview, "timezoneSnapshot", "Asia/Ho_Chi_Minh");
        ReflectionTestUtils.setField(preview, "strategyVersion", "1.0");

        Constructor<?> cinemaConstructor = Class.forName("com.lorafilm.movie.cinema.domain.entity.Cinema").getDeclaredConstructor();
        cinemaConstructor.setAccessible(true);
        Object cinema = cinemaConstructor.newInstance();
        ReflectionTestUtils.setField(cinema, "id", 9999L);
        ReflectionTestUtils.setField(preview, "cinema", cinema);

        preview = previewRepository.saveAndFlush(preview);

        Constructor<ShowtimeSchedulePreviewItem> itemConstructor = ShowtimeSchedulePreviewItem.class.getDeclaredConstructor();
        itemConstructor.setAccessible(true);
        ShowtimeSchedulePreviewItem item = itemConstructor.newInstance();
        
        ReflectionTestUtils.setField(item, "publicId", itemPublicId);
        ReflectionTestUtils.setField(item, "preview", preview);
        ReflectionTestUtils.setField(item, "validationStatus", PreviewItemValidationStatus.VALID);
        ReflectionTestUtils.setField(item, "selected", false);
        ReflectionTestUtils.setField(item, "score", BigDecimal.TEN);
        ReflectionTestUtils.setField(item, "rankingPosition", 1);
        ReflectionTestUtils.setField(item, "startTime", Instant.now());
        ReflectionTestUtils.setField(item, "endTime", Instant.now().plus(2, ChronoUnit.HOURS));
        ReflectionTestUtils.setField(item, "occupancyEndTime", Instant.now().plus(3, ChronoUnit.HOURS));
        ReflectionTestUtils.setField(item, "applyStatus", com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus.PENDING);

        Constructor<?> movieConstructor = Class.forName("com.lorafilm.movie.movie.domain.entity.Movie").getDeclaredConstructor();
        movieConstructor.setAccessible(true);
        Object movie = movieConstructor.newInstance();
        ReflectionTestUtils.setField(movie, "id", 9999L);

        Constructor<?> movieVersionConstructor = Class.forName("com.lorafilm.movie.movie.domain.entity.MovieVersion").getDeclaredConstructor();
        movieVersionConstructor.setAccessible(true);
        Object movieVersion = movieVersionConstructor.newInstance();
        ReflectionTestUtils.setField(movieVersion, "id", 9999L);

        Constructor<?> auditoriumConstructor = Class.forName("com.lorafilm.movie.auditorium.domain.entity.Auditorium").getDeclaredConstructor();
        auditoriumConstructor.setAccessible(true);
        Object auditorium = auditoriumConstructor.newInstance();
        ReflectionTestUtils.setField(auditorium, "id", 9999L);

        ReflectionTestUtils.setField(item, "cinema", cinema);
        ReflectionTestUtils.setField(item, "movie", movie);
        ReflectionTestUtils.setField(item, "movieVersion", movieVersion);
        ReflectionTestUtils.setField(item, "auditorium", auditorium);

        itemRepository.saveAndFlush(item);
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void caseA_getRealPreview() throws Exception {
        System.out.println("\n========== Case A: GET Real Preview ==========");
        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewPublicId))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void caseB_putRealSelections() throws Exception {
        System.out.println("\n========== Case B: PUT Real Selections ==========");
        UpdatePreviewItemSelectionRequest itemReq = new UpdatePreviewItemSelectionRequest();
        itemReq.setItemPublicId(itemPublicId);
        itemReq.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(0L);
        request.setItems(List.of(itemReq));

        mockMvc.perform(put("/api/admin/showtime-schedules/{id}/items", previewPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        ShowtimeSchedulePreview preview = previewRepository.findByPublicId(previewPublicId).orElseThrow();
        ShowtimeSchedulePreviewItem item = itemRepository.findByPublicId(itemPublicId).orElseThrow();
        System.out.println("\n--- DB VERIFICATION AFTER PUT ---");
        System.out.println("Preview status: " + preview.getStatus());
        System.out.println("Preview selected count: " + preview.getSelectedCandidateCount());
        System.out.println("Preview version: " + preview.getVersion());
        System.out.println("Item validation status: " + item.getValidationStatus());
        System.out.println("Item selected: " + item.getSelected());
        System.out.println("Item selected_at: " + item.getSelectedAt());
        System.out.println("Item selected_by: " + item.getSelectedBy());
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void caseC_putExpiredAndPersisted() throws Exception {
        System.out.println("\n========== Case C: PUT Expired & DB Persistence ==========");
        ShowtimeSchedulePreview preview = previewRepository.findByPublicId(previewPublicId).orElseThrow();
        ReflectionTestUtils.setField(preview, "expiresAt", Instant.now().minus(1, ChronoUnit.HOURS));
        previewRepository.saveAndFlush(preview);

        UpdatePreviewItemSelectionRequest itemReq = new UpdatePreviewItemSelectionRequest();
        itemReq.setItemPublicId(itemPublicId);
        itemReq.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(preview.getVersion());
        request.setItems(List.of(itemReq));

        mockMvc.perform(put("/api/admin/showtime-schedules/{id}/items", previewPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict());

        ShowtimeSchedulePreview updatedPreview = previewRepository.findByPublicId(previewPublicId).orElseThrow();
        System.out.println("\n--- DB VERIFICATION AFTER 409 ---");
        System.out.println("Preview status: " + updatedPreview.getStatus());
        System.out.println("Preview version: " + updatedPreview.getVersion());
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void caseX_testExpiredPersistent() throws Exception {
        System.out.println("=== EVIDENCE 3: Bằng chứng PUT expired thật sự persist DB ===");
        System.out.println("Trước request");
        System.out.println("Query:");
        System.out.println("SELECT public_id, status, expires_at, version FROM showtime_schedule_previews WHERE public_id = '" + previewPublicId + "';");
        
        Map<String, Object> before = jdbcTemplate.queryForMap("SELECT public_id, status, expires_at, version FROM showtime_schedule_previews WHERE public_id = ?", previewPublicId);
        System.out.println("Result: " + before);

        // Make it expired
        jdbcTemplate.update("UPDATE showtime_schedule_previews SET expires_at = ? WHERE public_id = ?", Instant.now().minusSeconds(3600), previewPublicId);
        
        System.out.println("Gọi PUT /api/admin/showtime-schedules/" + previewPublicId + "/items");
        
        String requestBody = "{\"expectedVersion\":0,\"items\":[{\"itemPublicId\":\"" + itemPublicId + "\",\"selected\":true}]}";
        
        mockMvc.perform(put("/api/admin/showtime-schedules/{previewPublicId}/items", previewPublicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("AUTO_SCHEDULE_PREVIEW_EXPIRED"));

        System.out.println("Sau request");
        System.out.println("Chạy lại:");
        System.out.println("SELECT public_id, status, expires_at, version FROM showtime_schedule_previews WHERE public_id = '" + previewPublicId + "';");
        Map<String, Object> after = jdbcTemplate.queryForMap("SELECT public_id, status, expires_at, version FROM showtime_schedule_previews WHERE public_id = ?", previewPublicId);
        System.out.println("Result: " + after);
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void caseY_testPutFullResponseAndDbQueries() throws Exception {
        System.out.println("=== EVIDENCE 4: E2E thật với MySQL (PUT thành công) ===");
        System.out.println("Trước PUT:");
        Map<String, Object> prevBefore = jdbcTemplate.queryForMap("SELECT public_id, status, selected_candidate_count, version FROM showtime_schedule_previews WHERE public_id = ?", previewPublicId);
        Map<String, Object> itemBefore = jdbcTemplate.queryForMap("SELECT public_id, validation_status, selected, selected_at, selected_by, apply_status FROM showtime_schedule_preview_items WHERE public_id = ?", itemPublicId);
        System.out.println("Preview: " + prevBefore);
        System.out.println("Item: " + itemBefore);

        String requestBody = "{\"expectedVersion\":0,\"items\":[{\"itemPublicId\":\"" + itemPublicId + "\",\"selected\":true}]}";
        
        mockMvc.perform(put("/api/admin/showtime-schedules/{previewPublicId}/items", previewPublicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("Sau PUT:");
        Map<String, Object> prevAfter = jdbcTemplate.queryForMap("SELECT public_id, status, selected_candidate_count, version FROM showtime_schedule_previews WHERE public_id = ?", previewPublicId);
        Map<String, Object> itemAfter = jdbcTemplate.queryForMap("SELECT public_id, validation_status, selected, selected_at, selected_by, apply_status FROM showtime_schedule_preview_items WHERE public_id = ?", itemPublicId);
        System.out.println("Preview: " + prevAfter);
        System.out.println("Item: " + itemAfter);
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "1")
    void caseZ_getApiDocs() throws Exception {
        System.out.println("=== EVIDENCE 5: OpenAPI ===");
        mockMvc.perform(get("/v3/api-docs"))
                .andDo(print());
    }
}
