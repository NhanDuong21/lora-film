package com.project.promotionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.dto.CampaignResponse;
import com.project.promotionservice.dto.CreateCampaignRequest;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.service.PromotionCampaignAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PromotionCampaignAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PromotionCampaignAdminService campaignService;

    @Test
    void createCampaign_ShouldReturn401_WhenAnonymous() throws Exception {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .campaignName("Summer 2026")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void createCampaign_ShouldReturn403_WhenCustomer() throws Exception {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .campaignName("Summer 2026")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createCampaign_ShouldReturn201_WhenAdminAndValid() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .campaignName("Summer 2026")
                .description("Summer discount")
                .startDate(start)
                .endDate(end)
                .isActive(true)
                .build();

        CampaignResponse response = CampaignResponse.builder()
                .campaignId(1L)
                .campaignName("Summer 2026")
                .description("Summer discount")
                .startDate(start)
                .endDate(end)
                .isActive(true)
                .availabilityStatus("UPCOMING")
                .build();

        when(campaignService.createCampaign(any(CreateCampaignRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.campaignId").value(1))
                .andExpect(jsonPath("$.data.campaignName").value("Summer 2026"))
                .andExpect(jsonPath("$.data.availabilityStatus").value("UPCOMING"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createCampaign_ShouldReturn400_WhenMissingName() throws Exception {
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .campaignName("")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createCampaign_ShouldReturn400_WhenNameTooLong() throws Exception {
        String longName = "A".repeat(151);
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .campaignName(longName)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createCampaign_ShouldReturn400_WhenInvalidDateRange() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        LocalDateTime end = LocalDateTime.now().plusDays(5); // end before start
        CreateCampaignRequest request = CreateCampaignRequest.builder()
                .campaignName("Summer 2026")
                .startDate(start)
                .endDate(end)
                .build();

        when(campaignService.createCampaign(any(CreateCampaignRequest.class)))
                .thenThrow(new BusinessException("endDate must be after startDate", "CAMPAIGN_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CAMPAIGN_INVALID_DATE_RANGE"));
    }

    @Test
    @WithMockUser(authorities = "PROMOTION_READ")
    void getCampaigns_ShouldReturn200_WhenAuthorized() throws Exception {
        com.project.promotionservice.dto.CampaignPageResponse pageResponse = com.project.promotionservice.dto.CampaignPageResponse.builder()
                .content(java.util.Collections.emptyList())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();

        when(campaignService.getCampaigns(any(), any(), any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/promotion-campaigns")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getCampaigns_ShouldReturn400_WhenInvalidSortProperty() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/promotion-campaigns")
                .param("sort", "unallowedField,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PROMOTION_INVALID_SORT"));
    }

    @Test
    @WithMockUser(authorities = "PROMOTION_READ")
    void getCampaignById_ShouldReturn200_WhenFound() throws Exception {
        CampaignResponse response = CampaignResponse.builder()
                .campaignId(1L)
                .campaignName("Summer 2026")
                .promotionCount(5)
                .isActive(true)
                .availabilityStatus("ACTIVE")
                .build();

        when(campaignService.getCampaignById(1L)).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/promotion-campaigns/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.campaignId").value(1))
                .andExpect(jsonPath("$.data.promotionCount").value(5));
    }

    @Test
    @WithMockUser(authorities = "PROMOTION_READ")
    void getCampaignById_ShouldReturn404_WhenNotFound() throws Exception {
        when(campaignService.getCampaignById(999L))
                .thenThrow(new BusinessException("Campaign not found with id: 999", "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/promotion-campaigns/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAMPAIGN_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateCampaign_ShouldReturn200_WhenValid() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        com.project.promotionservice.dto.UpdateCampaignRequest request = com.project.promotionservice.dto.UpdateCampaignRequest.builder()
                .campaignName("Summer 2026 Updated")
                .description("Summer discount")
                .startDate(start)
                .endDate(end)
                .isActive(true)
                .build();

        CampaignResponse response = CampaignResponse.builder()
                .campaignId(1L)
                .campaignName("Summer 2026 Updated")
                .description("Summer discount")
                .startDate(start)
                .endDate(end)
                .isActive(true)
                .availabilityStatus("UPCOMING")
                .build();

        when(campaignService.updateCampaign(any(Long.class), any(com.project.promotionservice.dto.UpdateCampaignRequest.class))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/promotion-campaigns/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.campaignName").value("Summer 2026 Updated"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateCampaign_ShouldReturn404_WhenNotFound() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        com.project.promotionservice.dto.UpdateCampaignRequest request = com.project.promotionservice.dto.UpdateCampaignRequest.builder()
                .campaignName("Summer 2026 Updated")
                .startDate(start)
                .endDate(end)
                .build();

        when(campaignService.updateCampaign(any(Long.class), any(com.project.promotionservice.dto.UpdateCampaignRequest.class)))
                .thenThrow(new BusinessException("Campaign not found with id: 999", "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/promotion-campaigns/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAMPAIGN_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateCampaign_ShouldReturn400_WhenInvalidDateRange() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        LocalDateTime end = LocalDateTime.now().plusDays(5);
        com.project.promotionservice.dto.UpdateCampaignRequest request = com.project.promotionservice.dto.UpdateCampaignRequest.builder()
                .campaignName("Summer 2026 Updated")
                .startDate(start)
                .endDate(end)
                .build();

        when(campaignService.updateCampaign(any(Long.class), any(com.project.promotionservice.dto.UpdateCampaignRequest.class)))
                .thenThrow(new BusinessException("endDate must be after startDate", "CAMPAIGN_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/promotion-campaigns/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CAMPAIGN_INVALID_DATE_RANGE"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateCampaignStatus_ShouldReturn200_WhenValid() throws Exception {
        com.project.promotionservice.dto.UpdateCampaignStatusRequest request = com.project.promotionservice.dto.UpdateCampaignStatusRequest.builder()
                .isActive(false)
                .build();

        CampaignResponse response = CampaignResponse.builder()
                .campaignId(1L)
                .campaignName("Summer 2026")
                .isActive(false)
                .availabilityStatus("DISABLED")
                .build();

        when(campaignService.updateCampaignStatus(any(Long.class), any(com.project.promotionservice.dto.UpdateCampaignStatusRequest.class))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/promotion-campaigns/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.availabilityStatus").value("DISABLED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateCampaignStatus_ShouldReturn404_WhenNotFound() throws Exception {
        com.project.promotionservice.dto.UpdateCampaignStatusRequest request = com.project.promotionservice.dto.UpdateCampaignStatusRequest.builder()
                .isActive(false)
                .build();

        when(campaignService.updateCampaignStatus(any(Long.class), any(com.project.promotionservice.dto.UpdateCampaignStatusRequest.class)))
                .thenThrow(new BusinessException("Campaign not found with id: 999", "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/promotion-campaigns/999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAMPAIGN_NOT_FOUND"));
    }
}
