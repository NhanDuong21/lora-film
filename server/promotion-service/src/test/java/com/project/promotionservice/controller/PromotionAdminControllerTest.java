package com.project.promotionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.dto.CreatePromotionRequest;
import com.project.promotionservice.dto.PromotionResponse;
import com.project.promotionservice.enums.DiscountType;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.service.PromotionAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PromotionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PromotionAdminService promotionService;

    @Test
    void createPromotion_ShouldReturn401_WhenAnonymous() throws Exception {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .promotionCode("SALE50")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("100.0"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .campaignId(1L)
                .build();

        mockMvc.perform(post("/api/admin/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void createPromotion_ShouldReturn403_WhenCustomer() throws Exception {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .promotionCode("SALE50")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("100.0"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .campaignId(1L)
                .build();

        mockMvc.perform(post("/api/admin/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createPromotion_ShouldReturn201_WhenFixedAmountValid() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .promotionCode("SALE50")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(start)
                .endDate(end)
                .campaignId(1L)
                .isActive(true)
                .build();

        PromotionResponse response = PromotionResponse.builder()
                .promotionId(10L)
                .promotionCode("SALE50")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .usageLimit(100)
                .limitPerUser(1)
                .usedCount(0)
                .startDate(start)
                .endDate(end)
                .isActive(true)
                .availabilityStatus("UPCOMING")
                .campaignId(1L)
                .build();

        when(promotionService.createPromotion(any(CreatePromotionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotionId").value(10))
                .andExpect(jsonPath("$.data.promotionCode").value("SALE50"))
                .andExpect(jsonPath("$.data.discountType").value("FIXED_AMOUNT"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createPromotion_ShouldReturn400_WhenCodeInvalidPattern() throws Exception {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .promotionCode("sale50!") // lower case + special character
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("100.0"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .campaignId(1L)
                .build();

        mockMvc.perform(post("/api/admin/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createPromotion_ShouldReturn400_WhenOutsideCampaignDates() throws Exception {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .promotionCode("SALE50")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(5))
                .campaignId(1L)
                .build();

        when(promotionService.createPromotion(any(CreatePromotionRequest.class)))
                .thenThrow(new BusinessException("Promotion dates must be within campaign dates", "PROMOTION_OUTSIDE_CAMPAIGN_RANGE", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/admin/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PROMOTION_OUTSIDE_CAMPAIGN_RANGE"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createPromotion_ShouldReturn400_WhenPercentageExceeds100() throws Exception {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .promotionCode("SALE101")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("101.00"))
                .maxDiscountAmount(new BigDecimal("10000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .campaignId(1L)
                .build();

        when(promotionService.createPromotion(any(CreatePromotionRequest.class)))
                .thenThrow(new BusinessException("PERCENTAGE discount value must be between 0.01 and 100.00", "PROMOTION_INVALID_DISCOUNT_VALUE", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/admin/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PROMOTION_INVALID_DISCOUNT_VALUE"));
    }

    @Test
    @WithMockUser(authorities = "PROMOTION_READ")
    void getPromotions_ShouldReturn200_WhenAuthorized() throws Exception {
        com.project.promotionservice.dto.PromotionPageResponse pageResponse = com.project.promotionservice.dto.PromotionPageResponse.builder()
                .content(java.util.Collections.emptyList())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();

        when(promotionService.getPromotions(any(), any(), any(), any(), any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/promotions")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getPromotions_ShouldReturn400_WhenInvalidSortProperty() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/promotions")
                .param("sort", "unallowedField,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PROMOTION_INVALID_SORT"));
    }

    @Test
    @WithMockUser(authorities = "PROMOTION_READ")
    void getPromotionById_ShouldReturn200_WhenFound() throws Exception {
        PromotionResponse response = PromotionResponse.builder()
                .promotionId(1L)
                .promotionCode("SUMMER50")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.0"))
                .isActive(true)
                .availabilityStatus("ACTIVE")
                .build();

        when(promotionService.getPromotionById(1L)).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotionId").value(1))
                .andExpect(jsonPath("$.data.promotionCode").value("SUMMER50"));
    }

    @Test
    @WithMockUser(authorities = "PROMOTION_READ")
    void getPromotionById_ShouldReturn404_WhenNotFound() throws Exception {
        when(promotionService.getPromotionById(999L))
                .thenThrow(new BusinessException("Promotion not found with id: 999", "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/promotions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROMOTION_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updatePromotion_ShouldReturn200_WhenValid() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        com.project.promotionservice.dto.UpdatePromotionRequest request = com.project.promotionservice.dto.UpdatePromotionRequest.builder()
                .promotionCode("SUMMER50UP")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(start)
                .endDate(end)
                .campaignId(1L)
                .isActive(true)
                .build();

        PromotionResponse response = PromotionResponse.builder()
                .promotionId(1L)
                .promotionCode("SUMMER50UP")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(start)
                .endDate(end)
                .isActive(true)
                .availabilityStatus("UPCOMING")
                .campaignId(1L)
                .build();

        when(promotionService.updatePromotion(any(Long.class), any(com.project.promotionservice.dto.UpdatePromotionRequest.class))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/promotions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotionCode").value("SUMMER50UP"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updatePromotion_ShouldReturn404_WhenNotFound() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        com.project.promotionservice.dto.UpdatePromotionRequest request = com.project.promotionservice.dto.UpdatePromotionRequest.builder()
                .promotionCode("SUMMER50UP")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("200000.00"))
                .usageLimit(100)
                .limitPerUser(1)
                .startDate(start)
                .endDate(end)
                .campaignId(1L)
                .build();

        when(promotionService.updatePromotion(any(Long.class), any(com.project.promotionservice.dto.UpdatePromotionRequest.class)))
                .thenThrow(new BusinessException("Promotion not found with id: 999", "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/promotions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROMOTION_NOT_FOUND"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updatePromotionStatus_ShouldReturn200_WhenValid() throws Exception {
        com.project.promotionservice.dto.UpdatePromotionStatusRequest request = com.project.promotionservice.dto.UpdatePromotionStatusRequest.builder()
                .isActive(false)
                .build();

        PromotionResponse response = PromotionResponse.builder()
                .promotionId(1L)
                .promotionCode("SUMMER50")
                .isActive(false)
                .availabilityStatus("DISABLED")
                .build();

        when(promotionService.updatePromotionStatus(any(Long.class), any(com.project.promotionservice.dto.UpdatePromotionStatusRequest.class))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/promotions/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.availabilityStatus").value("DISABLED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updatePromotionStatus_ShouldReturn404_WhenNotFound() throws Exception {
        com.project.promotionservice.dto.UpdatePromotionStatusRequest request = com.project.promotionservice.dto.UpdatePromotionStatusRequest.builder()
                .isActive(false)
                .build();

        when(promotionService.updatePromotionStatus(any(Long.class), any(com.project.promotionservice.dto.UpdatePromotionStatusRequest.class)))
                .thenThrow(new BusinessException("Promotion not found with id: 999", "PROMOTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/promotions/999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROMOTION_NOT_FOUND"));
    }
}
