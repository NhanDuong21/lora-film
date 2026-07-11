package com.project.promotionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.dto.ConfirmUsageRequest;
import com.project.promotionservice.dto.RevertUsageRequest;
import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.DiscountType;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.repository.PromotionCampaignRepository;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.repository.PromotionUsageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InternalPromotionLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionCampaignRepository campaignRepository;

    @Autowired
    private PromotionUsageRepository promotionUsageRepository;

    @Value("${app.internal-token:secret-internal-token}")
    private String validInternalToken;

    private PromotionCampaign campaign;
    private Promotion promotion;
    private PromotionUsage reservedUsage;

    @BeforeEach
    void setUp() {
        cleanupDatabase();

        LocalDateTime now = LocalDateTime.now();

        campaign = campaignRepository.save(PromotionCampaign.builder()
                .campaignName("Lifecycle Test Campaign")
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        promotion = promotionRepository.save(Promotion.builder()
                .campaign(campaign)
                .promotionCode("LIFE2026")
                .description("Lifecycle promo")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10.0))
                .minOrderAmount(BigDecimal.valueOf(10000.0))
                .usageLimit(10)
                .usedCount(1)
                .limitPerUser(1)
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        reservedUsage = promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(promotion)
                .userId(99L)
                .bookingId(8888L)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(BigDecimal.valueOf(20000.0))
                .discountAmount(BigDecimal.valueOf(2000.0))
                .finalAmount(BigDecimal.valueOf(18000.0))
                .expiresAt(now.plusMinutes(15))
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        promotionUsageRepository.deleteAllInBatch();
        promotionRepository.deleteAllInBatch();
        campaignRepository.deleteAllInBatch();
    }

    // ==========================================
    // SECURITY TESTS
    // ==========================================

    @Test
    void confirmUsage_ShouldReturn401_WhenMissingInternalToken() throws Exception {
        ConfirmUsageRequest request = new ConfirmUsageRequest(8888L, LocalDateTime.now());
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmUsage_ShouldReturn403_WhenInvalidInternalToken() throws Exception {
        ConfirmUsageRequest request = new ConfirmUsageRequest(8888L, LocalDateTime.now());
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/confirm")
                        .header("X-Internal-Token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // CONFIRM ENDPOINT TESTS
    // ==========================================

    @Test
    void confirmUsage_ShouldSucceed_WhenValidReservedUsage() throws Exception {
        LocalDateTime confirmTime = LocalDateTime.now();
        ConfirmUsageRequest request = new ConfirmUsageRequest(8888L, confirmTime);

        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/confirm")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Promotion usage confirmed successfully")))
                .andExpect(jsonPath("$.data.status", is("APPLIED")))
                .andExpect(jsonPath("$.data.usageId", is(reservedUsage.getId().intValue())))
                .andExpect(jsonPath("$.data.bookingId", is(8888)))
                .andExpect(jsonPath("$.data.revertedAt", nullValue()))
                .andExpect(jsonPath("$.data.revertReason", nullValue()));

        PromotionUsage updated = promotionUsageRepository.findById(reservedUsage.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PromotionUsageStatus.APPLIED);
        assertThat(updated.getConfirmedAt()).isNotNull();
    }

    @Test
    void confirmUsage_ShouldSucceedWithServerTime_WhenConfirmedAtIsNull() throws Exception {
        ConfirmUsageRequest request = new ConfirmUsageRequest(8888L, null);

        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/confirm")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.confirmedAt").exists());
    }

    @Test
    void confirmUsage_ShouldReturnIdempotentSuccess_WhenAlreadyConfirmed() throws Exception {
        // Confirm first time
        ConfirmUsageRequest request = new ConfirmUsageRequest(8888L, LocalDateTime.now());
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/confirm")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Confirm second time (idempotent)
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/confirm")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.idempotent", is(true)));
    }

    @Test
    void confirmUsage_ShouldReturn409_WhenUsageIsReverted() throws Exception {
        // Change status to REVERTED
        reservedUsage.setStatus(PromotionUsageStatus.REVERTED);
        promotionUsageRepository.saveAndFlush(reservedUsage);

        ConfirmUsageRequest request = new ConfirmUsageRequest(8888L, LocalDateTime.now());
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/confirm")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_USAGE_INVALID_TRANSITION")));
    }

    @Test
    void confirmUsage_ShouldReturn409_WhenBookingIdMismatch() throws Exception {
        ConfirmUsageRequest request = new ConfirmUsageRequest(9999L, LocalDateTime.now());
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/confirm")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_MISMATCH")));
    }

    // ==========================================
    // REVERT ENDPOINT TESTS
    // ==========================================

    @Test
    void revertUsage_ShouldSucceed_WhenValidReservedUsage() throws Exception {
        RevertUsageRequest request = new RevertUsageRequest(8888L, "Booking cancelled");

        int initialPromoUsedCount = promotionRepository.findById(promotion.getId()).orElseThrow().getUsedCount();

        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/revert")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Promotion usage reverted successfully")))
                .andExpect(jsonPath("$.data.status", is("REVERTED")))
                .andExpect(jsonPath("$.data.revertReason", is("Booking cancelled")))
                .andExpect(jsonPath("$.data.revertedAt").exists())
                .andExpect(jsonPath("$.data.confirmedAt", nullValue()));

        PromotionUsage updated = promotionUsageRepository.findById(reservedUsage.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PromotionUsageStatus.REVERTED);

        Promotion promoUpdated = promotionRepository.findById(promotion.getId()).orElseThrow();
        assertThat(promoUpdated.getUsedCount()).isEqualTo(initialPromoUsedCount - 1);
    }

    @Test
    void revertUsage_ShouldReturnIdempotentSuccess_WhenAlreadyReverted() throws Exception {
        RevertUsageRequest request = new RevertUsageRequest(8888L, "Booking cancelled");

        // Revert first time
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/revert")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        int countAfterFirstRevert = promotionRepository.findById(promotion.getId()).orElseThrow().getUsedCount();

        // Revert second time (idempotent)
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/revert")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.idempotent", is(true)));

        int countAfterSecondRevert = promotionRepository.findById(promotion.getId()).orElseThrow().getUsedCount();
        assertThat(countAfterSecondRevert).isEqualTo(countAfterFirstRevert); // no double decrement
    }

    @Test
    void revertUsage_ShouldReturn409_WhenUsageIsApplied() throws Exception {
        // Change status to APPLIED
        reservedUsage.setStatus(PromotionUsageStatus.APPLIED);
        promotionUsageRepository.saveAndFlush(reservedUsage);

        RevertUsageRequest request = new RevertUsageRequest(8888L, "Booking cancelled");
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/revert")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_USAGE_INVALID_TRANSITION")));
    }

    @Test
    void revertUsage_ShouldReturn400_WhenReasonIsInvalid() throws Exception {
        // Reason null or blank
        RevertUsageRequest request = new RevertUsageRequest(8888L, "   ");
        mockMvc.perform(post("/internal/promotions/usages/" + reservedUsage.getId() + "/revert")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    // ==========================================
    // GET BY BOOKING ID TESTS
    // ==========================================

    @Test
    void getUsageByBookingId_ShouldSucceed_WhenUsageExists() throws Exception {
        mockMvc.perform(get("/internal/promotions/bookings/8888")
                        .header("X-Internal-Token", validInternalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Promotion usage retrieved successfully")))
                .andExpect(jsonPath("$.data.bookingId", is(8888)))
                .andExpect(jsonPath("$.data.promotionCode", is("LIFE2026")))
                .andExpect(jsonPath("$.data.originalAmount").value(20000.0))
                .andExpect(jsonPath("$.data.discountAmount").value(2000.0))
                .andExpect(jsonPath("$.data.finalAmount").value(18000.0))
                .andExpect(jsonPath("$.data.expiresAt").exists());
    }

    @Test
    void getUsageByBookingId_ShouldReturn404_WhenUsageDoesNotExist() throws Exception {
        mockMvc.perform(get("/internal/promotions/bookings/99999")
                        .header("X-Internal-Token", validInternalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_USAGE_NOT_FOUND")));
    }
}
