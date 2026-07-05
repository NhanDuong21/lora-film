package com.project.promotionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.dto.BookingResponse;
import com.project.promotionservice.dto.PromotionResponse;
import com.project.promotionservice.dto.PromotionValidationRequest;
import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.DiscountType;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.repository.PromotionCampaignRepository;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.repository.PromotionUsageRepository;
import com.project.promotionservice.service.booking.BookingServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PromotionControllerTest {

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

    @MockBean
    private BookingServiceClient bookingServiceClient;

    private PromotionCampaign activeCampaign;
    private PromotionCampaign inactiveCampaign;
    private Promotion activePercentagePromo;
    private Promotion activeFixedPromo;
    private Promotion inactivePromo;
    private Promotion upcomingPromo;
    private Promotion expiredPromo;
    private Promotion outOfUsagePromo;

    @BeforeEach
    void setUp() {
        cleanupDatabase();

        LocalDateTime now = LocalDateTime.now();

        // 1. Campaigns
        activeCampaign = campaignRepository.save(PromotionCampaign.builder()
                .campaignName("Active Summer Campaign")
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(10))
                .active(true)
                .build());

        inactiveCampaign = campaignRepository.save(PromotionCampaign.builder()
                .campaignName("Inactive Campaign")
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(10))
                .active(false)
                .build());

        // 2. Active PERCENTAGE Promotion
        activePercentagePromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("PERCENT10")
                .description("10% off up to 30k")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10"))
                .maxDiscountAmount(new BigDecimal("30000"))
                .minOrderAmount(new BigDecimal("100000"))
                .usageLimit(100)
                .usedCount(5)
                .limitPerUser(2)
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        // 3. Active FIXED_AMOUNT Promotion
        activeFixedPromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("FIXED50K")
                .description("50k off min 200k")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000"))
                .minOrderAmount(new BigDecimal("200000"))
                .usageLimit(50)
                .usedCount(2)
                .limitPerUser(1)
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        // 4. Inactive Promotion
        inactivePromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("INACTIVECODE")
                .description("Disabled promo")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("10000"))
                .minOrderAmount(BigDecimal.ZERO)
                .usageLimit(10)
                .usedCount(0)
                .limitPerUser(1)
                .startDate(now.minusDays(2))
                .endDate(now.plusDays(5))
                .active(false)
                .build());

        // 5. Upcoming Promotion
        upcomingPromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("UPCOMINGCODE")
                .description("Starts next week")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("15"))
                .minOrderAmount(BigDecimal.ZERO)
                .usageLimit(100)
                .usedCount(0)
                .limitPerUser(1)
                .startDate(now.plusDays(7))
                .endDate(now.plusDays(15))
                .active(true)
                .build());

        // 6. Expired Promotion
        expiredPromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("EXPIREDCODE")
                .description("Expired promo")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("50"))
                .minOrderAmount(BigDecimal.ZERO)
                .usageLimit(100)
                .usedCount(10)
                .limitPerUser(1)
                .startDate(now.minusDays(10))
                .endDate(now.minusDays(2))
                .active(true)
                .build());

        // 7. Out of Usage Promotion
        outOfUsagePromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("FULLUSECODE")
                .description("All codes claimed")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20"))
                .minOrderAmount(BigDecimal.ZERO)
                .usageLimit(5)
                .usedCount(5)
                .limitPerUser(1)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(5))
                .active(true)
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
    // 1. PUBLIC PROMOTION APIS - active promotions list
    // ==========================================

    @Test
    void getActivePromotions_ShouldReturnAllActivePromotions() throws Exception {
        mockMvc.perform(get("/api/promotions/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].promotionCode", containsInAnyOrder("PERCENT10", "FIXED50K")))
                .andExpect(jsonPath("$.data.content[0].availabilityStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data.content[0].usedCount").doesNotExist()) // Omitted for public api
                .andExpect(jsonPath("$.data.content[0].usageLimit").doesNotExist());
    }

    @Test
    void getActivePromotions_ShouldFilterByDiscountType() throws Exception {
        mockMvc.perform(get("/api/promotions/active")
                .param("discountType", "FIXED_AMOUNT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].promotionCode", is("FIXED50K")));
    }

    @Test
    void getActivePromotions_ShouldFilterByMinOrderAmount() throws Exception {
        // FIXED50K has minOrderAmount=200000, PERCENT10 has minOrderAmount=100000
        // Querying with minOrderAmount=150000 should return only PERCENT10 because order amount is at least minOrderAmount of promotion.
        // Wait, minOrderAmount parameter is order amount of customer purchase, so it returns promo with minOrderAmount <= parameter
        mockMvc.perform(get("/api/promotions/active")
                .param("minOrderAmount", "150000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].promotionCode", is("PERCENT10")));
    }

    @Test
    void getActivePromotions_ShouldFail_WhenSortParamInvalid() throws Exception {
        mockMvc.perform(get("/api/promotions/active")
                .param("sort", "unallowedField,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_INVALID_SORT")));
    }

    // ==========================================
    // 2. PUBLIC PROMOTION APIS - detail
    // ==========================================

    @Test
    void getPromotionDetail_ShouldReturnDetails_WhenFound() throws Exception {
        mockMvc.perform(get("/api/promotions/" + activePercentagePromo.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.promotionId", is(activePercentagePromo.getId().intValue())))
                .andExpect(jsonPath("$.data.campaignName", is("Active Summer Campaign")))
                .andExpect(jsonPath("$.data.limitPerUser", is(2)))
                .andExpect(jsonPath("$.data.usedCount").doesNotExist());
    }

    @Test
    void getPromotionDetail_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/promotions/999999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_NOT_FOUND")));
    }

    // ==========================================
    // 3. SECURITY TESTS FOR PROTECTED APIS
    // ==========================================

    @Test
    void validatePromotion_ShouldReturn401_WhenAnonymous() throws Exception {
        PromotionValidationRequest request = new PromotionValidationRequest("PERCENT10", 1001L);
        mockMvc.perform(post("/api/promotions/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void previewDiscount_ShouldReturn401_WhenAnonymous() throws Exception {
        PromotionValidationRequest request = new PromotionValidationRequest("PERCENT10", 1001L);
        mockMvc.perform(post("/api/promotions/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 4. VALIDATION & PREVIEW BUSINESS LOGIC TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturnSuccess_WhenEligiblePercentage() throws Exception {
        Long bookingId = 1001L;
        String authHeader = "Bearer token";

        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .bookingCode("LORA-B1")
                .totalAmount(new BigDecimal("200000"))
                .status("PENDING_PAYMENT")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("PERCENT10", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.originalAmount", is(200000)))
                .andExpect(jsonPath("$.data.discountAmount", is(20000))) // 10% of 200k = 20k
                .andExpect(jsonPath("$.data.finalAmount", is(180000)))
                .andExpect(jsonPath("$.data.promotionId", is(activePercentagePromo.getId().intValue())));
        
        // Assert read-only / side effects check
        Promotion freshPromo = promotionRepository.findById(activePercentagePromo.getId()).orElseThrow();
        assertThat(freshPromo.getUsedCount()).isEqualTo(5); // unchanged
        assertThat(promotionUsageRepository.count()).isEqualTo(0); // no usage created
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturnSuccess_WhenEligibleFixed() throws Exception {
        Long bookingId = 1001L;

        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .bookingCode("LORA-B2")
                .totalAmount(new BigDecimal("250000"))
                .status("PENDING_PAYMENT")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("FIXED50K", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.originalAmount", is(250000)))
                .andExpect(jsonPath("$.data.discountAmount", is(50000))) // 50k off
                .andExpect(jsonPath("$.data.finalAmount", is(200000)));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void previewDiscount_ShouldReturnResponse_WhenEligible() throws Exception {
        Long bookingId = 1001L;

        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .bookingCode("LORA-B1")
                .totalAmount(new BigDecimal("200000"))
                .status("PENDING_PAYMENT")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("PERCENT10", bookingId);

        mockMvc.perform(post("/api/promotions/preview")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.previewOnly", is(true)))
                .andExpect(jsonPath("$.data.currency", is("VND")))
                .andExpect(jsonPath("$.data.valid").doesNotExist()) // Omitted in preview
                .andExpect(jsonPath("$.data.originalAmount", is(200000)))
                .andExpect(jsonPath("$.data.discountAmount", is(20000)))
                .andExpect(jsonPath("$.data.finalAmount", is(180000)));

        // No side effect assert
        Promotion freshPromo = promotionRepository.findById(activePercentagePromo.getId()).orElseThrow();
        assertThat(freshPromo.getUsedCount()).isEqualTo(5); // unchanged
    }

    // ==========================================
    // 5. NEGATIVE AND ERROR CONTEXT APIS
    // ==========================================

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn404_WhenPromoCodeNotFound() throws Exception {
        Long bookingId = 1001L;
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250000"))
                .status("PENDING_PAYMENT")
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("NON_EXIST_PROMO", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_NOT_FOUND")));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn409_WhenPromoDisabled() throws Exception {
        Long bookingId = 1001L;
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250000"))
                .status("PENDING_PAYMENT")
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("INACTIVECODE", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_DISABLED")));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn409_WhenPromoUpcoming() throws Exception {
        Long bookingId = 1001L;
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250000"))
                .status("PENDING_PAYMENT")
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("UPCOMINGCODE", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_NOT_STARTED")))
                .andExpect(jsonPath("$.data.startDate", notNullValue()));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn409_WhenPromoExpired() throws Exception {
        Long bookingId = 1001L;
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250000"))
                .status("PENDING_PAYMENT")
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("EXPIREDCODE", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_EXPIRED")));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn409_WhenPromoOutOfUsage() throws Exception {
        Long bookingId = 1001L;
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250000"))
                .status("PENDING_PAYMENT")
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("FULLUSECODE", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_USAGE_LIMIT_REACHED")));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn409_WhenPerUserLimitReached() throws Exception {
        Long bookingId = 1001L;
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250000"))
                .status("PENDING_PAYMENT")
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        // Seed 2 usages for user 15, matching limitPerUser = 2 of activePercentagePromo
        promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(activePercentagePromo)
                .userId(15L)
                .bookingId(2001L)
                .status(PromotionUsageStatus.APPLIED)
                .originalAmount(new BigDecimal("200000"))
                .discountAmount(new BigDecimal("20000"))
                .finalAmount(new BigDecimal("180000"))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build());

        promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(activePercentagePromo)
                .userId(15L)
                .bookingId(2002L)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(new BigDecimal("200000"))
                .discountAmount(new BigDecimal("20000"))
                .finalAmount(new BigDecimal("180000"))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build());

        PromotionValidationRequest request = new PromotionValidationRequest("PERCENT10", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_USER_LIMIT_REACHED")));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn409_WhenMinOrderAmountNotMet() throws Exception {
        Long bookingId = 1001L;
        // minOrderAmount is 200k, passing 150k
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("150000"))
                .status("PENDING_PAYMENT")
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("FIXED50K", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_MINIMUM_AMOUNT_NOT_MET")))
                .andExpect(jsonPath("$.data.minimumAmount", is(200000.00)))
                .andExpect(jsonPath("$.data.currentAmount", is(150000)));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn409_WhenBookingNotEligible() throws Exception {
        Long bookingId = 1001L;
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250000"))
                .status("CONFIRMED") // NOT PENDING_PAYMENT
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        PromotionValidationRequest request = new PromotionValidationRequest("PERCENT10", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_NOT_ELIGIBLE")));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturn409_WhenBookingAlreadyApplied() throws Exception {
        Long bookingId = 1001L;
        BookingResponse mockBooking = BookingResponse.builder()
                .bookingId(bookingId)
                .totalAmount(new BigDecimal("250000"))
                .status("PENDING_PAYMENT")
                .build();
        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenReturn(mockBooking);

        // Seed active usage for this bookingId
        promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(activePercentagePromo)
                .userId(15L)
                .bookingId(bookingId)
                .status(PromotionUsageStatus.RESERVED)
                .originalAmount(new BigDecimal("250000"))
                .discountAmount(new BigDecimal("25000"))
                .finalAmount(new BigDecimal("225000"))
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());

        PromotionValidationRequest request = new PromotionValidationRequest("PERCENT10", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_ALREADY_APPLIED")));
    }

    @Test
    @WithMockUser(username = "15", authorities = "ROLE_USER")
    void validatePromotion_ShouldReturnBookingServiceError_WhenServiceClientThrows() throws Exception {
        Long bookingId = 1001L;

        when(bookingServiceClient.getBooking(eq(bookingId), any())).thenThrow(
                new BusinessException("Booking not found", "PROMOTION_BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND)
        );

        PromotionValidationRequest request = new PromotionValidationRequest("PERCENT10", bookingId);

        mockMvc.perform(post("/api/promotions/validate")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_NOT_FOUND")));
    }
}
