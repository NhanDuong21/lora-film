package com.project.scoreservice.controller;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.RedeemPreviewRequest;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.repository.MembershipTierRepository;
import com.project.scoreservice.repository.UserScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.Optional;
 
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
 
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ScoreControllerIntegrationTest {
 
    @Autowired
    private MockMvc mockMvc;
 
    @Autowired
    private ObjectMapper objectMapper;
 
    @Autowired
    private MembershipTierRepository membershipTierRepository;
 
    @Autowired
    private UserScoreRepository userScoreRepository;

    @Autowired
    private com.project.scoreservice.repository.ScoreHistoryRepository scoreHistoryRepository;

    @Autowired
    private com.project.scoreservice.repository.PointExpirationBucketRepository pointExpirationBucketRepository;

    @Autowired
    private com.project.scoreservice.repository.MembershipTierHistoryRepository membershipTierHistoryRepository;
 
    @BeforeEach
    void setUp() {
        // Clean up user scores to avoid side-effects
        pointExpirationBucketRepository.deleteAll();
        membershipTierHistoryRepository.deleteAll();
        scoreHistoryRepository.deleteAll();
        userScoreRepository.deleteAll();

 
        // Retrieve default SILVER tier seeded by the PostConstruct block in MembershipTierServiceImpl
        MembershipTier silverTier = membershipTierRepository.findByTierName("SILVER")
                .orElseThrow(() -> new RuntimeException("Default tier SILVER not found"));
 
        // Seed user score for authenticated user (ID 15)
        UserScore userScore = UserScore.builder()
                .userId(15L)
                .currentPoints(500)
                .accumulatedPoints(1000)
                .currentTier(silverTier)
                .build();
        userScoreRepository.save(userScore);
    }
 
    @Test
    @WithMockUser(username = "15")
    public void testRedeemPreviewInvalidPointsZero_ReturnsInvalidPointAmount() throws Exception {
        RedeemPreviewRequest request = new RedeemPreviewRequest(1001L, 0);
 
        mockMvc.perform(post("/api/scores/me/redeem-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_INVALID_POINT_AMOUNT")))
                .andExpect(jsonPath("$.message", is("Points must be greater than zero")));
    }
 
    @Test
    @WithMockUser(username = "15")
    public void testRedeemPreviewInvalidPointsNegative_ReturnsInvalidPointAmount() throws Exception {
        RedeemPreviewRequest request = new RedeemPreviewRequest(1001L, -10);
 
        mockMvc.perform(post("/api/scores/me/redeem-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_INVALID_POINT_AMOUNT")))
                .andExpect(jsonPath("$.message", is("Points must be greater than zero")));
    }
}
