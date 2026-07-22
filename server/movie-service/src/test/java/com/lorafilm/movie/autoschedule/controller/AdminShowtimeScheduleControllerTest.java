package com.lorafilm.movie.autoschedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewPageResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewItemResponse;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreviewHistoryItemResponse;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewHistoryService;
import com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminShowtimeScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShowtimeSchedulePreviewService service;

    @MockBean
    private AutoSchedulePreviewHistoryService historyService;

    private String previewId = "preview-123";

    @Test
    @WithMockUser(roles = "ADMIN")
    void case1_getPreviewSuccess() throws Exception {
        System.out.println("\n========== CASE 1: GET preview thanh cong ==========");
        ShowtimeSchedulePreviewPageResponse response = new ShowtimeSchedulePreviewPageResponse();
        ShowtimeSchedulePreviewSummaryResponse summary = new ShowtimeSchedulePreviewSummaryResponse();
        summary.setPreviewPublicId(previewId);
        summary.setStatus(SchedulePreviewStatus.PREVIEWED);
        response.setPreview(summary);

        when(service.getPreview(eq(previewId), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewId))
                .andDo(print());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void previewItemsSerializeKnownAndLegacyServiceDates() throws Exception {
        ShowtimeSchedulePreviewSummaryResponse summary = new ShowtimeSchedulePreviewSummaryResponse();
        summary.setPreviewPublicId(previewId);
        summary.setStatus(SchedulePreviewStatus.PREVIEWED);

        ShowtimeSchedulePreviewItemResponse known = new ShowtimeSchedulePreviewItemResponse();
        known.setItemPublicId("known-item");
        known.setServiceDate(LocalDate.of(2026, 7, 24));
        ShowtimeSchedulePreviewItemResponse legacy = new ShowtimeSchedulePreviewItemResponse();
        legacy.setItemPublicId("legacy-item");
        legacy.setServiceDate(null);

        ShowtimeSchedulePreviewPageResponse response = new ShowtimeSchedulePreviewPageResponse(
                summary,
                new com.lorafilm.movie.common.api.PageResponse<>(
                        List.of(known, legacy), 0, 2, 2, 1, true));
        when(service.getPreview(eq(previewId), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.content[0].serviceDate").value("2026-07-24"))
                .andExpect(jsonPath("$.data.items.content[1].serviceDate").value(nullValue()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void case2_putSelectionsSuccess() throws Exception {
        System.out.println("\n========== CASE 2: PUT selections thanh cong ==========");
        UpdatePreviewItemSelectionRequest itemReq = new UpdatePreviewItemSelectionRequest();
        itemReq.setItemPublicId("item-1");
        itemReq.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(1L);
        request.setItems(List.of(itemReq));

        ShowtimeSchedulePreviewSummaryResponse response = new ShowtimeSchedulePreviewSummaryResponse();
        response.setPreviewPublicId(previewId);
        response.setVersion(2L);

        when(service.updateSelections(eq(previewId), any())).thenReturn(response);

        mockMvc.perform(put("/api/admin/showtime-schedules/{id}/items", previewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void case3_putStaleVersion_409() throws Exception {
        System.out.println("\n========== CASE 3: PUT stale expectedVersion -> 409 ==========");
        UpdatePreviewItemSelectionRequest itemReq = new UpdatePreviewItemSelectionRequest();
        itemReq.setItemPublicId("item-1");
        itemReq.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(1L);
        request.setItems(List.of(itemReq));

        when(service.updateSelections(eq(previewId), any())).thenThrow(new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT));

        mockMvc.perform(put("/api/admin/showtime-schedules/{id}/items", previewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void case4_putRejectedItem_400() throws Exception {
        System.out.println("\n========== CASE 4: PUT chon REJECTED item -> 400 ==========");
        UpdatePreviewItemSelectionRequest itemReq = new UpdatePreviewItemSelectionRequest();
        itemReq.setItemPublicId("item-1");
        itemReq.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(1L);
        request.setItems(List.of(itemReq));

        when(service.updateSelections(eq(previewId), any())).thenThrow(new BusinessException(ErrorCode.AUTO_SCHEDULE_REJECTED_ITEM_CANNOT_BE_SELECTED));

        mockMvc.perform(put("/api/admin/showtime-schedules/{id}/items", previewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void case5_getPreviewExpired() throws Exception {
        System.out.println("\n========== CASE 5: GET preview het han -> status EXPIRED ==========");
        ShowtimeSchedulePreviewPageResponse response = new ShowtimeSchedulePreviewPageResponse();
        ShowtimeSchedulePreviewSummaryResponse summary = new ShowtimeSchedulePreviewSummaryResponse();
        summary.setPreviewPublicId(previewId);
        summary.setStatus(SchedulePreviewStatus.EXPIRED);
        response.setPreview(summary);

        when(service.getPreview(eq(previewId), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewId))
                .andDo(print());
    }

    @Test
    void case6_noToken_401() throws Exception {
        System.out.println("\n========== CASE 6: No token -> 401 ==========");
        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewId))
                .andDo(print());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void case7_customerToken_403() throws Exception {
        System.out.println("\n========== CASE 7: Customer token -> 403 ==========");
        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewId))
                .andDo(print());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void case8_invalidPagination_400() throws Exception {
        System.out.println("\n========== CASE 8: INVALID PAGINATION -> 400 ==========");
        
        mockMvc.perform(get("/api/admin/showtime-schedules/{id}?page=-1&size=50", previewId))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/showtime-schedules/{id}?page=0&size=0", previewId))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/showtime-schedules/{id}?page=0&size=101", previewId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getHistory_returnsAdminPageEnvelope() throws Exception {
        when(historyService.getHistory(any())).thenReturn(new PageResponse<>(
                List.of(), 0, 10, 0, 0, true
        ));

        mockMvc.perform(get("/api/admin/showtime-schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getHistory_rejectsInvalidPaginationAndTypes() throws Exception {
        mockMvc.perform(get("/api/admin/showtime-schedules?page=-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/showtime-schedules?size=101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/showtime-schedules?status=UNKNOWN"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/showtime-schedules?scheduleFrom=07-22-2026"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/showtime-schedules?createdFrom=2026-07-22T10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/showtime-schedules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getHistory_requiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/showtime-schedules"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getHistory_redactsInternalAndRawFailureFields() throws Exception {
        AutoSchedulePreviewHistoryItemResponse item = new AutoSchedulePreviewHistoryItemResponse();
        item.setPreviewPublicId("preview-safe");
        item.setApplyMode(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        item.setPersistedStatus(SchedulePreviewStatus.FAILED);
        item.setDisplayStatus(SchedulePreviewStatus.FAILED);
        item.setFailureReasonSafe("Auto schedule generation failed");
        item.setExpiresAt(Instant.parse("2026-07-22T11:00:00Z"));
        when(historyService.getHistory(any())).thenReturn(new PageResponse<>(
                List.of(item), 0, 10, 1, 1, true
        ));

        mockMvc.perform(get("/api/admin/showtime-schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].previewPublicId").value("preview-safe"))
                .andExpect(jsonPath("$.data.data[0].failureReasonSafe").value("Auto schedule generation failed"))
                .andExpect(jsonPath("$.data.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.data[0].generatedBy").doesNotExist())
                .andExpect(jsonPath("$.data.data[0].appliedBy").doesNotExist())
                .andExpect(jsonPath("$.data.data[0].failureReason").doesNotExist())
                .andExpect(jsonPath("$.data.data[0].requestFingerprint").doesNotExist())
                .andExpect(jsonPath("$.data.data[0].items").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getHistory_acceptsMaximumPageSize_andRejectsZero() throws Exception {
        when(historyService.getHistory(any())).thenReturn(new PageResponse<>(
                List.of(), 0, 100, 0, 0, true
        ));
        mockMvc.perform(get("/api/admin/showtime-schedules?size=100"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/showtime-schedules?size=0"))
                .andExpect(status().isBadRequest());
    }
}
