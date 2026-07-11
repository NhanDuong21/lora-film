package com.project.promotionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.dto.ApplyPromotionRequest;
import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.DiscountType;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.repository.PromotionCampaignRepository;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.repository.PromotionUsageRepository;
import com.project.promotionservice.service.booking.BookingContext;
import com.project.promotionservice.service.booking.BookingInternalClient;
import com.project.promotionservice.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InternalPromotionControllerTest {

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
    private BookingInternalClient bookingInternalClient;

    @MockBean
    private com.project.promotionservice.service.booking.BookingServiceClient bookingServiceClient;

    @Value("${app.internal-token:secret-internal-token}")
    private String validInternalToken;

    private PromotionCampaign activeCampaign;
    private Promotion percentagePromo;
    private Promotion fixedPromo;
    private Promotion limitedPromo;

    @BeforeEach
    void setUp() {
        cleanupDatabase();

        LocalDateTime now = LocalDateTime.now();

        activeCampaign = campaignRepository.save(PromotionCampaign.builder()
                .campaignName("Internal Test Campaign")
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        percentagePromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("PERCENT10")
                .description("10% off movie tickets")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10.0))
                .maxDiscountAmount(BigDecimal.valueOf(50000.0))
                .minOrderAmount(BigDecimal.valueOf(100000.0))
                .usageLimit(100)
                .usedCount(0)
                .limitPerUser(2)
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        fixedPromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("FIXED50")
                .description("50k off")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000.0))
                .minOrderAmount(BigDecimal.valueOf(150000.0))
                .usageLimit(100)
                .usedCount(0)
                .limitPerUser(1)
                .startDate(now.minusDays(5))
                .endDate(now.plusDays(5))
                .active(true)
                .build());

        limitedPromo = promotionRepository.save(Promotion.builder()
                .campaign(activeCampaign)
                .promotionCode("LIMIT1")
                .description("Limit 1 usage")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(20000.0))
                .minOrderAmount(BigDecimal.valueOf(50000.0))
                .usageLimit(1)
                .usedCount(0)
                .limitPerUser(1)
                .startDate(now.minusDays(5))
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

    private BookingContext buildMockBooking(Long bookingId, Long userId, BigDecimal amount, LocalDateTime expiresAt) {
        return BookingContext.builder()
                .bookingId(bookingId)
                .userId(userId)
                .status("PENDING_PAYMENT")
                .expiresAt(expiresAt)
                .amount(amount)
                .build();
    }

    // ==========================================
    // SECURITY TESTS
    // ==========================================

    @Test
    void applyPromotion_ShouldReturn401_WhenMissingInternalToken() throws Exception {
        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(1001L)
                .userId(15L)
                .bookingAmount(BigDecimal.valueOf(200000))
                .bookingExpiresAt(LocalDateTime.now().plusHours(1))
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INTERNAL_UNAUTHORIZED")));
    }

    @Test
    void applyPromotion_ShouldReturn403_WhenInvalidInternalToken() throws Exception {
        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(1001L)
                .userId(15L)
                .bookingAmount(BigDecimal.valueOf(200000))
                .bookingExpiresAt(LocalDateTime.now().plusHours(1))
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN")));
    }

    // ==========================================
    // SUCCESS CASES
    // ==========================================

    @Test
    void applyPromotion_ShouldApplyPercentageDiscount_WhenValid() throws Exception {
        Long bookingId = 1001L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(200000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, userId, amount, expiresAt));

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("percent10 ") // testing case insensitive and trim
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.originalAmount", is(200000)))
                .andExpect(jsonPath("$.data.discountAmount", is(20000))) // 10% of 200k = 20k
                .andExpect(jsonPath("$.data.finalAmount", is(180000)))
                .andExpect(jsonPath("$.data.usageStatus", is("RESERVED")))
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // Verify usedCount incremented
        Promotion updatedPromo = promotionRepository.findById(percentagePromo.getId()).orElseThrow();
        assertThat(updatedPromo.getUsedCount()).isEqualTo(1);
    }

    @Test
    void applyPromotion_ShouldApplyFixedDiscount_WhenValid() throws Exception {
        Long bookingId = 1002L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(200000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, userId, amount, expiresAt));

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("FIXED50")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.discountAmount", is(50000)))
                .andExpect(jsonPath("$.data.finalAmount", is(150000)))
                .andExpect(jsonPath("$.data.idempotent", is(false)));
    }

    // ==========================================
    // IDEMPOTENCY TESTS
    // ==========================================

    @Test
    void applyPromotion_ShouldReturnExistingUsage_WhenSameBookingAndPromoApplied() throws Exception {
        Long bookingId = 1003L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(200000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // Pre-create usage
        promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(percentagePromo)
                .bookingId(bookingId)
                .userId(userId)
                .originalAmount(amount)
                .discountAmount(BigDecimal.valueOf(20000))
                .finalAmount(BigDecimal.valueOf(180000))
                .status(PromotionUsageStatus.RESERVED)
                .expiresAt(expiresAt)
                .build());

        // Increment used count to mock prior state
        percentagePromo.setUsedCount(1);
        promotionRepository.save(percentagePromo);

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("already applied")))
                .andExpect(jsonPath("$.data.idempotent", is(true)))
                .andExpect(jsonPath("$.data.bookingId", is(bookingId.intValue())));

        // Verify usedCount did NOT increment further
        Promotion updatedPromo = promotionRepository.findById(percentagePromo.getId()).orElseThrow();
        assertThat(updatedPromo.getUsedCount()).isEqualTo(1);
    }

    @Test
    void applyPromotion_ShouldReturnConflict_WhenSameBookingAndDifferentPromoApplied() throws Exception {
        Long bookingId = 1004L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(200000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // Pre-apply percentage promo
        promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(percentagePromo)
                .bookingId(bookingId)
                .userId(userId)
                .originalAmount(amount)
                .discountAmount(BigDecimal.valueOf(20000))
                .finalAmount(BigDecimal.valueOf(180000))
                .status(PromotionUsageStatus.RESERVED)
                .expiresAt(expiresAt)
                .build());

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("FIXED50")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_ALREADY_APPLIED")));
    }

    @Test
    void applyPromotion_ShouldReturnConflict_WhenSamePromoRevertedBefore() throws Exception {
        Long bookingId = 1005L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(200000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // Pre-apply and revert
        promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(percentagePromo)
                .bookingId(bookingId)
                .userId(userId)
                .originalAmount(amount)
                .discountAmount(BigDecimal.valueOf(20000))
                .finalAmount(BigDecimal.valueOf(180000))
                .status(PromotionUsageStatus.REVERTED)
                .expiresAt(expiresAt)
                .build());

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_USAGE_ALREADY_REVERTED")));
    }

    // ==========================================
    // VALIDATION TESTS
    // ==========================================

    @Test
    void applyPromotion_ShouldReturn404_WhenPromoCodeNotFound() throws Exception {
        Long bookingId = 1006L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(200000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, userId, amount, expiresAt));

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("NOTFOUND")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_NOT_FOUND")));
    }

    @Test
    void applyPromotion_ShouldReturn409_WhenPromoDisabled() throws Exception {
        Long bookingId = 1007L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(200000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // Disable promotion
        percentagePromo.setActive(false);
        promotionRepository.save(percentagePromo);

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, userId, amount, expiresAt));

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_DISABLED")));
    }

    @Test
    void applyPromotion_ShouldReturn409_WhenMinOrderAmountNotMet() throws Exception {
        Long bookingId = 1008L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(50000); // FIXED50 requires 150000
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, userId, amount, expiresAt));

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("FIXED50")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_MINIMUM_AMOUNT_NOT_MET")))
                .andExpect(jsonPath("$.data.minimumAmount", is(150000.0)))
                .andExpect(jsonPath("$.data.currentAmount", is(50000)));
    }

    @Test
    void applyPromotion_ShouldReturn409_WhenGlobalUsageLimitReached() throws Exception {
        Long bookingId = 1009L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(100000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // Exhaust global usage limit
        limitedPromo.setUsedCount(1);
        promotionRepository.save(limitedPromo);

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, userId, amount, expiresAt));

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("LIMIT1")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_USAGE_LIMIT_REACHED")));
    }

    @Test
    void applyPromotion_ShouldReturn409_WhenUserLimitReached() throws Exception {
        Long bookingId = 1010L;
        Long userId = 15L;
        BigDecimal amount = BigDecimal.valueOf(200000);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // User already has usage for fixedPromo (limit 1)
        promotionUsageRepository.save(PromotionUsage.builder()
                .promotion(fixedPromo)
                .bookingId(999L)
                .userId(userId)
                .originalAmount(amount)
                .discountAmount(BigDecimal.valueOf(50000))
                .finalAmount(BigDecimal.valueOf(150000))
                .status(PromotionUsageStatus.RESERVED)
                .expiresAt(expiresAt)
                .build());

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, userId, amount, expiresAt));

        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("FIXED50")
                .bookingId(bookingId)
                .userId(userId)
                .bookingAmount(amount)
                .bookingExpiresAt(expiresAt)
                .build();

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_USER_LIMIT_REACHED")));
    }

    // ==========================================
    // BOOKING SERVICE VALIDATION TESTS
    // ==========================================

    @Test
    void applyPromotion_ShouldReturn404_WhenBookingNotFoundInBookingService() throws Exception {
        Long bookingId = 9999L; // StubClient throws 404 for this ID
        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(bookingId)
                .userId(15L)
                .bookingAmount(BigDecimal.valueOf(200000))
                .bookingExpiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenThrow(new BusinessException("Booking not found", "PROMOTION_BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_NOT_FOUND")));
    }

    @Test
    void applyPromotion_ShouldReturn409_WhenBookingOwnerMismatch() throws Exception {
        Long bookingId = 1002L; // StubClient sets userId = 16 for this ID
        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(bookingId)
                .userId(15L)
                .bookingAmount(BigDecimal.valueOf(200000))
                .bookingExpiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // Simulate returning context with mismatch user
        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, 16L, BigDecimal.valueOf(200000), LocalDateTime.now().plusHours(1)));

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_OWNERSHIP_MISMATCH")));
    }

    @Test
    void applyPromotion_ShouldReturn409_WhenBookingInvalidStatus() throws Exception {
        Long bookingId = 1003L; // StubClient sets status = CANCELLED for this ID
        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(bookingId)
                .userId(15L)
                .bookingAmount(BigDecimal.valueOf(200000))
                .bookingExpiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(BookingContext.builder()
                        .bookingId(bookingId)
                        .userId(15L)
                        .status("CANCELLED")
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .amount(BigDecimal.valueOf(200000))
                        .build());

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_NOT_ELIGIBLE")));
    }

    @Test
    void applyPromotion_ShouldReturn409_WhenBookingExpired() throws Exception {
        Long bookingId = 1004L; // StubClient sets expired time
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(5);
        ApplyPromotionRequest request = ApplyPromotionRequest.builder()
                .promotionCode("PERCENT10")
                .bookingId(bookingId)
                .userId(15L)
                .bookingAmount(BigDecimal.valueOf(200000))
                .bookingExpiresAt(expiredTime)
                .build();

        when(bookingInternalClient.getBookingContext(eq(bookingId)))
                .thenReturn(buildMockBooking(bookingId, 15L, BigDecimal.valueOf(200000), expiredTime));

        mockMvc.perform(post("/internal/promotions/apply")
                        .header("X-Internal-Token", validInternalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("PROMOTION_BOOKING_NOT_ELIGIBLE")));
    }
}
