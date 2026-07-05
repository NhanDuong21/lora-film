package com.project.scoreservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.dto.CreateMembershipTierRequest;
import com.project.scoreservice.dto.UpdateMembershipTierRequest;
import com.project.scoreservice.entity.MembershipTier;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminMembershipTierControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembershipTierRepository membershipTierRepository;

    @Autowired
    private UserScoreRepository userScoreRepository;

    private MembershipTier silver;
    private MembershipTier gold;

    @BeforeEach
    void setUp() {
        userScoreRepository.deleteAll();

        silver = membershipTierRepository.findByTierName("SILVER")
                .orElseThrow(() -> new RuntimeException("Default tier SILVER not found"));

        gold = membershipTierRepository.findByTierName("GOLD")
                .orElseThrow(() -> new RuntimeException("Default tier GOLD not found"));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_MANAGE"})
    public void testCreateTier_ValidRequest_Returns201Created() throws Exception {
        CreateMembershipTierRequest request = new CreateMembershipTierRequest(
                " platinum ", 1500, new BigDecimal("0.12"), "Platinum description"
        );

        mockMvc.perform(post("/api/admin/membership-tiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tierName", is("PLATINUM"))) // Normalized name
                .andExpect(jsonPath("$.data.minPoints", is(1500)))
                .andExpect(jsonPath("$.data.earningRate", is(0.12)));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_MANAGE"})
    public void testCreateTier_DuplicateName_Returns409Conflict() throws Exception {
        CreateMembershipTierRequest request = new CreateMembershipTierRequest(
                "gold", 1500, new BigDecimal("0.10"), "Gold duplicate"
        );

        mockMvc.perform(post("/api/admin/membership-tiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_TIER_NAME_ALREADY_EXISTS")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_MANAGE"})
    public void testCreateTier_DuplicateThreshold_Returns409Conflict() throws Exception {
        CreateMembershipTierRequest request = new CreateMembershipTierRequest(
                "PLATINUM", 400, new BigDecimal("0.10"), "Platinum duplicate threshold"
        );

        mockMvc.perform(post("/api/admin/membership-tiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_TIER_THRESHOLD_CONFLICT")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_READ"})
    public void testGetTiers_ReturnsSortedList() throws Exception {
        mockMvc.perform(get("/api/admin/membership-tiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].tierName", is("SILVER")))
                .andExpect(jsonPath("$.data[1].tierName", is("GOLD")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_READ"})
    public void testGetTierDetail_ExistingTier_ReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/membership-tiers/" + gold.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tierName", is("GOLD")))
                .andExpect(jsonPath("$.data.minPoints", is(400)));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_READ"})
    public void testGetTierDetail_NonExistingTier_Returns404() throws Exception {
        mockMvc.perform(get("/api/admin/membership-tiers/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_TIER_NOT_FOUND")));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_MANAGE"})
    public void testUpdateTier_ValidRequestNoPointsChange_ReturnsSuccess() throws Exception {
        UpdateMembershipTierRequest request = new UpdateMembershipTierRequest(
                "GOLD", 400, new BigDecimal("0.08"), "Gold update description"
        );

        mockMvc.perform(put("/api/admin/membership-tiers/" + gold.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.earningRate", is(0.08)))
                .andExpect(jsonPath("$.data.recalculationRequired", is(false)));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_MANAGE"})
    public void testUpdateTier_ValidRequestWithPointsChange_ReturnsSuccessRecalculationTrue() throws Exception {
        UpdateMembershipTierRequest request = new UpdateMembershipTierRequest(
                "GOLD", 500, new BigDecimal("0.07"), "Gold threshold update"
        );

        mockMvc.perform(put("/api/admin/membership-tiers/" + gold.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.minPoints", is(500)))
                .andExpect(jsonPath("$.data.recalculationRequired", is(true)));
    }

    @Test
    @WithMockUser(username = "999", authorities = {"MEMBERSHIP_TIER_MANAGE"})
    public void testUpdateTier_LowestTierViolation_Returns409() throws Exception {
        // Attempting to change SILVER (minPoints = 0) to minPoints = 100, leaving no lowest fallback tier
        UpdateMembershipTierRequest request = new UpdateMembershipTierRequest(
                "SILVER", 100, new BigDecimal("0.05"), "Silver invalid update"
        );

        mockMvc.perform(put("/api/admin/membership-tiers/" + silver.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SCORE_TIER_CONFIGURATION_INVALID")));
    }
}
