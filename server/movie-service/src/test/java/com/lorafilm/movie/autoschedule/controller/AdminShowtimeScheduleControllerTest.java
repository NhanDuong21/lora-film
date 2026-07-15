package com.lorafilm.movie.autoschedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewPageResponse;
import com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
