package com.project.scoreservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.ScoreAdjustmentRequest;
import com.project.scoreservice.dto.ScoreAdjustmentType;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.UserScoreRepository;
import com.project.scoreservice.repository.ScoreHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminScoreControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipTierRepository membershipTierRepository;

    @Autowired
    private UserScoreRepository userScoreRepository;

    @Autowired
    private ScoreHistoryRepository scoreHistoryRepository;

    @Autowired
    private com.project.scoreservice.repository.PointExpirationBucketRepository pointExpirationBucketRepository;

    @Autowired
    private com.project.scoreservice.repository.MembershipTierHistoryRepository membershipTierHistoryRepository;

    private MembershipTier silver;
    private MembershipTier gold;

    @BeforeEach
    void setUp() {
        pointExpirationBucketRepository.deleteAll();
        membershipTierHistoryRepository.deleteAll();
        scoreHistoryRepository.deleteAll();
        userScoreRepository.deleteAll();


        silver = membershipTierRepository.findByTierName("SILVER")
                .orElseGet(() -> membershipTierRepository.save(new MembershipTier(null, "SILVER", 0, new BigDecimal("0.05"), "Silver tier", null, null)));
        gold = membershipTierRepository.findByTierName("GOLD")
                .orElseGet(() -> membershipTierRepository.save(new MembershipTier(null, "GOLD", 400, new BigDecimal("0.07"), "Gold tier", null, null)));

        UserScore userScore = new UserScore(15L, 100, 100, silver, null, null);
        userScoreRepository.save(userScore);
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_READ"})
    public void testGetUserScoreDetail_ExistingUser_ReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/scores/users/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(15)))
                .andExpect(jsonPath("$.data.currentPoints", is(100)))
                .andExpect(jsonPath("$.data.accumulatedPoints", is(100)))
                .andExpect(jsonPath("$.data.currentTier.tierName", is("SILVER")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_READ"})
    public void testGetUserScoreDetail_UserNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/admin/scores/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_ACCOUNT_NOT_FOUND")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"ROLE_USER"})
    public void testGetUserScoreDetail_ForbiddenForNormalUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/scores/users/15"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetUserScoreDetail_UnauthorizedForAnonymous_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/scores/users/15"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_READ"})
    public void testGetUserHistory_SortingValidationWhitelist_Returns400() throws Exception {
        mockMvc.perform(get("/api/admin/scores/users/15/history")
                        .param("sort", "invalidColumn,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_INVALID_QUERY")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_ADJUST"})
    public void testAdjustScore_AddPointsWithoutAccumulated_Returns201() throws Exception {
        ScoreAdjustmentRequest request = new ScoreAdjustmentRequest(
                ScoreAdjustmentType.ADD, 50, false, "Good behavior reward", "REQ-ADD-001"
        );

        mockMvc.perform(post("/api/admin/scores/users/15/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.currentPoints", is(150)))
                .andExpect(jsonPath("$.data.accumulatedPoints", is(100))) // unchanged
                .andExpect(jsonPath("$.data.currentTier", is("SILVER")))
                .andExpect(jsonPath("$.data.idempotent", is(false)));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_ADJUST"})
    public void testAdjustScore_AddPointsWithAccumulatedUpgrade_Returns201() throws Exception {
        ScoreAdjustmentRequest request = new ScoreAdjustmentRequest(
                ScoreAdjustmentType.ADD, 400, true, "Adding points to trigger upgrade", "REQ-ADD-002"
        );

        mockMvc.perform(post("/api/admin/scores/users/15/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.currentPoints", is(500)))
                .andExpect(jsonPath("$.data.accumulatedPoints", is(500))) // changed
                .andExpect(jsonPath("$.data.currentTier", is("GOLD")))
                .andExpect(jsonPath("$.data.tierChanged", is(true)))
                .andExpect(jsonPath("$.data.idempotent", is(false)));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_ADJUST"})
    public void testAdjustScore_DeductPointsBalanceUnderflow_Returns409() throws Exception {
        ScoreAdjustmentRequest request = new ScoreAdjustmentRequest(
                ScoreAdjustmentType.DEDUCT, 150, false, "Deducting too much", "REQ-DED-001"
        );

        mockMvc.perform(post("/api/admin/scores/users/15/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_BALANCE_WOULD_BE_NEGATIVE")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_ADJUST"})
    public void testAdjustScore_IdempotencySuccessRetry_Returns200() throws Exception {
        ScoreAdjustmentRequest request = new ScoreAdjustmentRequest(
                ScoreAdjustmentType.ADD, 50, false, "Idempotent add test", "REQ-IDEM-001"
        );

        // First request - Created (201)
        mockMvc.perform(post("/api/admin/scores/users/15/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.idempotent", is(false)));

        // Second request - Ok (200)
        mockMvc.perform(post("/api/admin/scores/users/15/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotent", is(true)));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_ADJUST"})
    public void testAdjustScore_IdempotencyConflictPayload_Returns409() throws Exception {
        ScoreAdjustmentRequest request1 = new ScoreAdjustmentRequest(
                ScoreAdjustmentType.ADD, 50, false, "Idempotent add test", "REQ-IDEM-002"
        );

        ScoreAdjustmentRequest request2 = new ScoreAdjustmentRequest(
                ScoreAdjustmentType.DEDUCT, 50, false, "Idempotent deduct test with same ID", "REQ-IDEM-002"
        );

        // First request - Created
        mockMvc.perform(post("/api/admin/scores/users/15/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second request with different payload - Conflict (409)
        mockMvc.perform(post("/api/admin/scores/users/15/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("SCORE_ADJUSTMENT_IDEMPOTENCY_CONFLICT")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"SCORE_MANAGE"})
    public void testRecalculateTier_SingleUserSuccessfulRecalculation_Returns200() throws Exception {
        // Manually alter user's accumulated points without updating their currentTier to simulate inconsistency
        UserScore userScore = userScoreRepository.findByUserId(15L).get();
        userScore.setAccumulatedPoints(500); // Silver is normally threshold < 400. 500 should be Gold.
        userScoreRepository.saveAndFlush(userScore);

        mockMvc.perform(post("/api/admin/scores/users/15/recalculate-tier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.previousTier", is("SILVER")))
                .andExpect(jsonPath("$.data.currentTier", is("GOLD")))
                .andExpect(jsonPath("$.data.tierChanged", is(true)));
    }
}
