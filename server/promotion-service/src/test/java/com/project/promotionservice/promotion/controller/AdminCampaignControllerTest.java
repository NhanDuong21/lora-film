package com.project.promotionservice.promotion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.configuration.security.filter.InternalTokenFilter;
import com.project.promotionservice.configuration.security.filter.JwtAuthenticationFilter;
import com.project.promotionservice.common.filter.RequestLoggingFilter;
import com.project.promotionservice.configuration.security.jwt.JwtTokenProvider;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.response.CampaignDetailResponse;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.service.CampaignService;
import com.project.promotionservice.promotion.service.ApprovalService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminCampaignController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CampaignService campaignService;

    @MockBean
    private ApprovalService approvalService;

    // Mock security-related beans to prevent bootstrap failures
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private InternalTokenFilter internalTokenFilter;
    @MockBean
    private RequestLoggingFilter requestLoggingFilter;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createCampaign_success() throws Exception {
        CampaignCreateRequest request = new CampaignCreateRequest();
        request.setCode("WINTER2026");
        request.setName("Winter 2026 Promo");
        request.setCampaignType(com.project.promotionservice.promotion.enums.CampaignType.COUPON);
        request.setTimezone("Asia/Ho_Chi_Minh");
        request.setStartAt(Instant.now());
        request.setEndAt(Instant.now().plusSeconds(3600));
        request.setBudgetAmount(new BigDecimal("50000.00"));

        CampaignResponse response = new CampaignResponse();
        response.setPublicId("winter-campaign-uuid");
        response.setCode("WINTER2026");
        response.setName("Winter 2026 Promo");

        when(campaignService.createCampaign(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("winter-campaign-uuid"))
                .andExpect(jsonPath("$.data.code").value("WINTER2026"));
    }

    @Test
    void getCampaign_success() throws Exception {
        String campaignId = "3aa198f5-8175-44cb-a31b-d378ca4bfae1";
        CampaignDetailResponse response = new CampaignDetailResponse();
        response.setPublicId(campaignId);
        response.setCode("PROMO1");
        response.setName("Promo 1 Detail");
        response.setRules(Collections.emptyList());

        when(campaignService.getCampaign(campaignId)).thenReturn(response);

        mockMvc.perform(get("/api/admin/promotion-campaigns/{id}", campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value(campaignId))
                .andExpect(jsonPath("$.data.code").value("PROMO1"));
    }
}
