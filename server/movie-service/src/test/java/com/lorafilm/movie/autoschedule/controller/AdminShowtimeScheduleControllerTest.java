package com.lorafilm.movie.autoschedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.ActiveProfiles;

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
    void getPreview_shouldReturn200_whenAdmin() throws Exception {
        ShowtimeSchedulePreviewResponse response = new ShowtimeSchedulePreviewResponse();
        response.setPreviewPublicId(previewId);

        when(service.getPreview(previewId)).thenReturn(response);

        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previewPublicId").value(previewId));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getPreview_shouldReturn403_whenCustomer() throws Exception {
        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPreview_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/admin/showtime-schedules/{id}", previewId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSelections_shouldReturn200_whenValidRequest() throws Exception {
        UpdatePreviewItemSelectionRequest itemReq = new UpdatePreviewItemSelectionRequest();
        itemReq.setItemPublicId("item-1");
        itemReq.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(1L);
        request.setItems(List.of(itemReq));

        ShowtimeSchedulePreviewResponse response = new ShowtimeSchedulePreviewResponse();
        response.setPreviewPublicId(previewId);

        when(service.updateSelections(eq(previewId), any())).thenReturn(response);

        mockMvc.perform(put("/api/admin/showtime-schedules/{id}/items", previewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSelections_shouldReturn400_whenMissingExpectedVersion() throws Exception {
        UpdatePreviewItemSelectionRequest itemReq = new UpdatePreviewItemSelectionRequest();
        itemReq.setItemPublicId("item-1");
        itemReq.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setItems(List.of(itemReq));

        mockMvc.perform(put("/api/admin/showtime-schedules/{id}/items", previewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSelections_shouldReturn404_whenPreviewNotFound() throws Exception {
        UpdatePreviewItemSelectionRequest itemReq = new UpdatePreviewItemSelectionRequest();
        itemReq.setItemPublicId("item-1");
        itemReq.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(1L);
        request.setItems(List.of(itemReq));

        when(service.updateSelections(eq(previewId), any())).thenThrow(new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));

        mockMvc.perform(put("/api/admin/showtime-schedules/{id}/items", previewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("AUTO_SCHEDULE_PREVIEW_NOT_FOUND"));
    }
}
