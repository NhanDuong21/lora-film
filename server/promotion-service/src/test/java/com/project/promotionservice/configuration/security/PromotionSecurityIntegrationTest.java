package com.project.promotionservice.configuration.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionRedemption;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import com.project.promotionservice.promotion.enums.PromotionDistributionMode;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.time.Instant;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromotionSecurityIntegrationTest {

    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Autowired private MockMvc mockMvc;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;
    @Autowired private PromotionCampaignRepository campaignRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private PromotionRedemptionRepository redemptionRepository;

    @Test
    void operationsRoleCanReachItsAdminReservationApi() throws Exception {
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + token("OPERATIONS_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unrelatedAuthenticatedRoleIsForbiddenWithSerializableError() throws Exception {
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + token("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void unauthenticatedAdminRequestReturnsSerializableUnauthorizedError() throws Exception {
        mockMvc.perform(get("/api/admin/reservations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void signedTokenWithNonAccessTypeIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + token("ADMIN", "refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void everyAdminHandlerHasMethodOrClassAuthorizationPolicy() {
        List<String> unprotected = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getKey().getPatternValues().stream()
                        .anyMatch(path -> path.startsWith("/api/admin/")))
                .filter(entry -> !hasAuthorizationPolicy(entry.getValue()))
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .sorted()
                .toList();

        assertThat(unprotected)
                .as("Every /api/admin handler must remain protected when global routing is authenticated()")
                .isEmpty();
    }

    @Test
    void adminRoleWithoutOverrideCapabilityCannotCallOverrideApi() throws Exception {
        String campaignId = "01a92c81-342d-421c-9d8c-89de54746758";
        mockMvc.perform(post("/api/admin/promotion-campaigns/{id}/override-approval", campaignId)
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "ADMIN", List.of("PROMOTION_VIEW"), List.of()))
                        .contentType("application/json")
                        .content("{\"campaignCode\":\"TEST\",\"incidentReference\":\"INC-1\",\"reason\":\"review\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void emergencyStopCapabilityDoesNotGrantForceRelease() throws Exception {
        String campaignId = "01a92c81-342d-421c-9d8c-89de54746758";
        mockMvc.perform(post("/api/admin/promotion-campaigns/{id}/force-release", campaignId)
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "ADMIN", List.of("PROMOTION_EMERGENCY_STOP"), List.of()))
                        .header("Idempotency-Key", "security-test-command")
                        .contentType("application/json")
                        .content("{\"campaignCode\":\"TEST\",\"reason\":\"review\","
                                + "\"impactToken\":\"" + "0".repeat(64)
                                + "\",\"campaignVersion\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCannotReadGlobalCampaignThroughDirectApi() throws Exception {
        PromotionCampaign global = campaign("GLOBAL-SCOPE", CampaignScopeType.GLOBAL, null);
        campaignRepository.saveAndFlush(global);

        mockMvc.perform(get("/api/admin/promotion-campaigns/{id}", global.getPublicId())
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_VIEW"), List.of("cinema-a"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanReadOnlyCampaignInsideAssignedCinemaScope() throws Exception {
        PromotionCampaign own = campaign(
                "OWN-SCOPE", CampaignScopeType.ASSIGNED_CINEMAS, "[\"cinema-a\"]");
        PromotionCampaign other = campaign(
                "OTHER-SCOPE", CampaignScopeType.ASSIGNED_CINEMAS, "[\"cinema-b\"]");
        campaignRepository.saveAllAndFlush(List.of(own, other));
        String token = tokenWithClaims(
                "MANAGER", List.of("PROMOTION_VIEW"), List.of("cinema-a"));

        mockMvc.perform(get("/api/admin/promotion-campaigns/{id}", own.getPublicId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("ASSIGNED_CINEMAS"));
        mockMvc.perform(get("/api/admin/promotion-campaigns/{id}", other.getPublicId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCannotPauseGlobalCampaignThroughDirectApi() throws Exception {
        PromotionCampaign global = campaign("GLOBAL-PAUSE", CampaignScopeType.GLOBAL, null);
        campaignRepository.saveAndFlush(global);

        mockMvc.perform(patch("/api/admin/promotion-campaigns/{id}/status", global.getPublicId())
                        .param("action", "PAUSE")
                        .param("expectedVersion", String.valueOf(global.getVersion()))
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_OPERATE"), List.of("cinema-a"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerWorkspaceShowsCentralAndAssignedCampaignsButNotAnotherCinema() throws Exception {
        PromotionCampaign global = campaign(
                "MANAGER-CENTRAL", CampaignScopeType.GLOBAL, null);
        PromotionCampaign own = campaign(
                "MANAGER-OWN", CampaignScopeType.ASSIGNED_CINEMAS, "[\"cinema-a\"]");
        PromotionCampaign other = campaign(
                "MANAGER-OTHER", CampaignScopeType.ASSIGNED_CINEMAS, "[\"cinema-b\"]");
        campaignRepository.saveAllAndFlush(List.of(global, own, other));

        mockMvc.perform(get("/api/manager/promotions/campaigns")
                        .param("cinemaPublicId", "cinema-a")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_VIEW"), List.of("cinema-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].publicId", hasItem(global.getPublicId())))
                .andExpect(jsonPath("$.data[*].publicId", hasItem(own.getPublicId())))
                .andExpect(jsonPath("$.data[*].publicId", not(hasItem(other.getPublicId()))))
                .andExpect(jsonPath("$.data[?(@.publicId == '%s')].readOnly"
                        .formatted(global.getPublicId()), hasItem(true)));
    }

    @Test
    void managerWorkspaceRejectsClientSelectedCinemaOutsideJwtAssignment() throws Exception {
        mockMvc.perform(get("/api/manager/promotions/workspace")
                        .param("cinemaPublicId", "cinema-b")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_VIEW"), List.of("cinema-a"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerDistributionOptionsNeverExposeAutomationOnlyBenefits() throws Exception {
        PromotionCampaign own = campaign(
                "MANAGER-DISTRIBUTION", CampaignScopeType.ASSIGNED_CINEMAS,
                "[\"cinema-a\"]");
        campaignRepository.saveAndFlush(own);
        Promotion local = promotion(own, "Local recovery",
                PromotionDistributionMode.ASSIGNED_WALLET, "cinema-a");
        Promotion automation = promotion(own, "Automation owned",
                PromotionDistributionMode.AUTOMATION_ONLY, "cinema-a");
        promotionRepository.saveAllAndFlush(List.of(local, automation));

        mockMvc.perform(get("/api/manager/promotions/distribution-options")
                        .param("cinemaPublicId", "cinema-a")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_VIEW"), List.of("cinema-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].publicId", hasItem(local.getPublicId())))
                .andExpect(jsonPath("$.data[*].publicId", not(hasItem(automation.getPublicId()))));
    }

    @Test
    void managerIncidentReadNeedsLocalAuditCapability() throws Exception {
        mockMvc.perform(get("/api/manager/promotions/incidents")
                        .param("cinemaPublicId", "cinema-a")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_VIEW"), List.of("cinema-a"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void staleCampaignTransitionReturnsConflictInsteadOfApplyingOldAllowedAction() throws Exception {
        PromotionCampaign campaign = campaign("STALE-ACTION", CampaignScopeType.GLOBAL, null);
        campaignRepository.saveAndFlush(campaign);

        mockMvc.perform(patch("/api/admin/promotion-campaigns/{id}/status", campaign.getPublicId())
                        .param("action", "PAUSE")
                        .param("expectedVersion", String.valueOf(campaign.getVersion() + 1))
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "ADMIN", List.of("PROMOTION_OPERATE"), List.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Dữ liệu đã thay đổi, vui lòng tải lại"));
    }

    @Test
    void managerCampaignCreationRejectsGlobalScopeInsteadOfExpandingSilently() throws Exception {
        String code = "LOCAL-" + System.nanoTime();
        String body = """
                {
                  "code":"%s","name":"Manager local campaign",
                  "startAt":"%s","endAt":"%s","budgetAmount":100000,
                  "scopeType":"GLOBAL","cinemaPublicIds":["cinema-a"]
                }
                """.formatted(code, Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200));

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_AUTHOR"), List.of("cinema-a")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void managerCampaignCreationUsesOnlyExplicitAssignedCinemaSubset() throws Exception {
        String code = "LOCAL-SUBSET-" + System.nanoTime();
        String body = """
                {
                  "code":"%s","name":"Manager Landmark campaign",
                  "startAt":"%s","endAt":"%s","budgetAmount":100000,
                  "scopeType":"ASSIGNED_CINEMAS","cinemaPublicIds":["cinema-a"]
                }
                """.formatted(code, Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200));

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_AUTHOR"),
                                List.of("cinema-a", "cinema-b")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scopeType").value("ASSIGNED_CINEMAS"))
                .andExpect(jsonPath("$.data.cinemaScope[0]").value("cinema-a"));
    }

    @Test
    void managerCampaignCreationRejectsCinemaOutsideAssignment() throws Exception {
        String code = "LOCAL-OUTSIDE-" + System.nanoTime();
        String body = """
                {
                  "code":"%s","name":"Manager outside campaign",
                  "startAt":"%s","endAt":"%s","budgetAmount":100000,
                  "scopeType":"ASSIGNED_CINEMAS","cinemaPublicIds":["cinema-b"]
                }
                """.formatted(code, Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200));

        mockMvc.perform(post("/api/admin/promotion-campaigns")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_AUTHOR"), List.of("cinema-a")))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerOperationsSearchExcludesOtherCinemaCampaignData() throws Exception {
        PromotionCampaign own = campaign(
                "OPS-OWN", CampaignScopeType.ASSIGNED_CINEMAS, "[\"cinema-a\"]");
        PromotionCampaign other = campaign(
                "OPS-OTHER", CampaignScopeType.ASSIGNED_CINEMAS, "[\"cinema-b\"]");
        campaignRepository.saveAllAndFlush(List.of(own, other));
        PromotionRedemption ownEntry = redemption(own.getPublicId(), "Own benefit");
        PromotionRedemption otherEntry = redemption(other.getPublicId(), "Other benefit");
        redemptionRepository.saveAllAndFlush(List.of(ownEntry, otherEntry));

        mockMvc.perform(get("/api/admin/promotion-operations/search")
                        .header("Authorization", "Bearer " + tokenWithClaims(
                                "MANAGER", List.of("PROMOTION_AUDIT_VIEW"),
                                List.of("cinema-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.redemptionTotal").value(1))
                .andExpect(jsonPath("$.data.redemptions[0].campaignPublicId")
                        .value(own.getPublicId()));
    }

    private boolean hasAuthorizationPolicy(HandlerMethod handler) {
        return AnnotatedElementUtils.findMergedAnnotation(
                handler.getMethod(), PreAuthorize.class) != null
                || AnnotatedElementUtils.findMergedAnnotation(
                handler.getBeanType(), PreAuthorize.class) != null;
    }

    private String token(String role) {
        return token(role, "access");
    }

    private String token(String role, String tokenType) {
        return tokenWithClaims(role, "OPERATIONS_MANAGER".equals(role)
                ? List.of("PROMOTION_AUDIT_VIEW") : List.of(), List.of(), tokenType);
    }

    private String tokenWithClaims(
            String role, List<String> permissions, List<String> cinemaPublicIds) {
        return tokenWithClaims(role, permissions, cinemaPublicIds, "access");
    }

    private String tokenWithClaims(
            String role, List<String> permissions, List<String> cinemaPublicIds,
            String tokenType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Date now = new Date();
        return Jwts.builder()
                .subject("security-test@lorafilm.vn")
                .claim("userId", 123L)
                .claim("role", role)
                .claim("permissions", permissions)
                .claim("cinemaPublicIds", cinemaPublicIds)
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
    }

    private PromotionCampaign campaign(
            String code, CampaignScopeType scopeType, String cinemaScopeJson) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setCode(code);
        campaign.setName(code);
        campaign.setSlug(code.toLowerCase());
        campaign.setStartAt(Instant.now().plusSeconds(3600));
        campaign.setEndAt(Instant.now().plusSeconds(7200));
        campaign.setBudgetAmount(BigDecimal.ZERO);
        campaign.setBudgetRemaining(BigDecimal.ZERO);
        campaign.setScopeType(scopeType);
        campaign.setCinemaScopeJson(cinemaScopeJson);
        return campaign;
    }

    private PromotionRedemption redemption(String campaignPublicId, String name) {
        PromotionRedemption redemption = new PromotionRedemption();
        redemption.setUserPublicId("1001");
        redemption.setPromotionPublicId(java.util.UUID.randomUUID().toString());
        redemption.setCampaignPublicId(campaignPublicId);
        redemption.setPromotionType(PromotionType.AUTO);
        redemption.setPromotionName(name);
        redemption.setPromotionPriority(1);
        redemption.setPromotionStackable(false);
        redemption.setConditionsSnapshotJson("{}");
        redemption.setActionsSnapshotJson("{}");
        redemption.setSequenceNo(1);
        redemption.setStatus(PromotionRedemptionStatus.RESERVED);
        redemption.setOriginalAmount(new BigDecimal("100000.00"));
        redemption.setDiscountAmount(new BigDecimal("10000.00"));
        redemption.setFinalAmount(new BigDecimal("90000.00"));
        return redemption;
    }

    private Promotion promotion(
            PromotionCampaign campaign, String name,
            PromotionDistributionMode distributionMode, String cinemaPublicId) {
        Promotion promotion = new Promotion();
        promotion.setCampaignPublicId(campaign.getPublicId());
        promotion.setPromotionType(PromotionType.VOUCHER);
        promotion.setName(name);
        promotion.setDescription(name);
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setPublicVisible(false);
        promotion.setDistributionMode(distributionMode);
        promotion.setConditionsJson("{\"cinemaPublicIds\":[\"" + cinemaPublicId + "\"]}");
        promotion.setActionsJson("[{\"discountType\":\"FIXED_AMOUNT\",\"value\":10000}]");
        promotion.setValidFrom(Instant.now().minusSeconds(3600));
        promotion.setValidTo(Instant.now().plusSeconds(3600));
        return promotion;
    }
}
