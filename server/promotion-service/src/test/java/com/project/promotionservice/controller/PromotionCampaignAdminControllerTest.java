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
}
