package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Enforces row-level cinema ownership in addition to action capabilities. */
@Service
public class PromotionResourceScopeService {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRepository promotionRepository;
    private final ObjectMapper objectMapper;

    public PromotionResourceScopeService(
            PromotionCampaignRepository campaignRepository,
            PromotionRepository promotionRepository,
            ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.promotionRepository = promotionRepository;
        this.objectMapper = objectMapper;
    }

    public CreationScope creationScope(
            CampaignCreateRequest request, UserPrincipal principal) {
        Set<String> requested = normalize(request.getCinemaPublicIds());
        CampaignScopeType requestedType = request.getScopeType() == null
                ? CampaignScopeType.GLOBAL : request.getScopeType();
        if (!isManager(principal)) {
            if (requestedType == CampaignScopeType.ASSIGNED_CINEMAS) {
                if (requested.isEmpty()) {
                    throw invalidScope("A cinema-scoped campaign requires at least one cinema");
                }
                return new CreationScope(requestedType, requested);
            }
            return new CreationScope(CampaignScopeType.GLOBAL, Set.of());
        }
        Set<String> assigned = normalize(assignedCinemas(principal));
        if (assigned.isEmpty()) {
            throw forbidden("Manager has no assigned cinema scope");
        }
        if (requestedType == CampaignScopeType.GLOBAL || requested.isEmpty()) {
            throw invalidScope(
                    "Manager campaigns must select at least one assigned cinema; GLOBAL is not allowed");
        }
        if (!assigned.containsAll(requested)) {
            throw forbidden("Requested campaign scope exceeds the manager's assigned cinemas");
        }
        return new CreationScope(CampaignScopeType.ASSIGNED_CINEMAS, requested);
    }

    public Set<String> accessibleCampaignIds(UserPrincipal principal) {
        if (!isManager(principal)) return Set.of();
        Set<String> assigned = assignedCinemas(principal);
        if (assigned.isEmpty()) return Set.of();
        Set<String> ids = new LinkedHashSet<>();
        campaignRepository.findAll().stream()
                .filter(campaign -> campaign.getDeletedAt() == null)
                .filter(campaign -> canAccess(campaign, assigned))
                .map(PromotionCampaign::getPublicId)
                .forEach(ids::add);
        return Set.copyOf(ids);
    }

    public void requireCampaignAccess(String campaignPublicId, UserPrincipal principal) {
        if (!isManager(principal)) return;
        PromotionCampaign campaign = campaignRepository.findByPublicIdAndDeletedAtIsNull(campaignPublicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
        if (!canAccess(campaign, assignedCinemas(principal))) {
            throw forbidden("Campaign is outside the manager's assigned cinema scope");
        }
    }

    public void requirePromotionAccess(String promotionPublicId, UserPrincipal principal) {
        if (!isManager(principal)) return;
        Promotion promotion = promotionRepository.findByPublicIdAndDeletedAtIsNull(promotionPublicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Promotion not found", HttpStatus.NOT_FOUND));
        requireCampaignAccess(promotion.getCampaignPublicId(), principal);
    }

    public void requireCampaignVersion(String campaignPublicId, int expectedVersion) {
        PromotionCampaign campaign = campaignRepository.findByPublicIdAndDeletedAtIsNull(
                        campaignPublicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
        if (campaign.getVersion() == null || campaign.getVersion() != expectedVersion) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Dữ liệu đã thay đổi, vui lòng tải lại",
                    HttpStatus.CONFLICT);
        }
    }

    public void requirePromotionTargetAllowed(
            String campaignPublicId, JsonNode conditions, UserPrincipal principal) {
        if (!isManager(principal)) return;
        requireCampaignAccess(campaignPublicId, principal);
        Set<String> requested = cinemaTargets(conditions);
        if (requested.isEmpty()) {
            throw forbidden("Manager promotions must explicitly target assigned cinemas");
        }
        Set<String> assigned = assignedCinemas(principal);
        if (!assigned.containsAll(requested)) {
            throw forbidden("Promotion targets a cinema outside the manager's assigned scope");
        }
        PromotionCampaign campaign = campaignRepository.findByPublicIdAndDeletedAtIsNull(campaignPublicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
        if (!readScope(campaign).containsAll(requested)) {
            throw forbidden("Promotion target exceeds its campaign cinema scope");
        }
    }

    public boolean isManager(UserPrincipal principal) {
        return principal != null && principal.getRoles().stream()
                .filter(StringUtils::hasText)
                .map(role -> role.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT))
                .anyMatch("MANAGER"::equals);
    }

    public boolean isAdmin(UserPrincipal principal) {
        return principal != null && principal.getRoles().stream()
                .filter(StringUtils::hasText)
                .map(role -> role.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT))
                .anyMatch("ADMIN"::equals);
    }

    public String scopeJson(CreationScope scope) {
        try {
            return scope.type() == CampaignScopeType.GLOBAL
                    ? null : objectMapper.writeValueAsString(scope.cinemaPublicIds());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize campaign cinema scope", exception);
        }
    }

    private boolean canAccess(PromotionCampaign campaign, Set<String> assigned) {
        if (campaign.getScopeType() != CampaignScopeType.ASSIGNED_CINEMAS) return false;
        Set<String> campaignCinemas = readScope(campaign);
        return !campaignCinemas.isEmpty() && assigned.containsAll(campaignCinemas);
    }

    private Set<String> assignedCinemas(UserPrincipal principal) {
        return principal == null ? Set.of() : principal.getCinemaPublicIds();
    }

    private Set<String> normalize(Set<String> values) {
        if (values == null) return Set.of();
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> readScope(PromotionCampaign campaign) {
        if (campaign.getCinemaScopeJson() == null || campaign.getCinemaScopeJson().isBlank()) {
            return Set.of();
        }
        try {
            JsonNode node = objectMapper.readTree(campaign.getCinemaScopeJson());
            while (node != null && node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            if (node == null || !node.isArray()) return Set.of();
            Set<String> values = new LinkedHashSet<>();
            node.forEach(value -> {
                if (value.isTextual() && StringUtils.hasText(value.asText())) {
                    values.add(value.asText().trim());
                }
            });
            return Set.copyOf(values);
        } catch (Exception ignored) {
            return Set.of();
        }
    }

    private Set<String> cinemaTargets(JsonNode conditions) {
        if (conditions == null || !conditions.isObject()) return Set.of();
        JsonNode node = conditions.has("cinemaPublicIds")
                ? conditions.get("cinemaPublicIds") : conditions.get("cinemaIds");
        if (node == null || !node.isArray()) return Set.of();
        Set<String> values = new LinkedHashSet<>();
        node.forEach(value -> {
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                values.add(value.asText().trim());
            }
        });
        return Set.copyOf(values);
    }

    private BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }

    private BusinessException invalidScope(String message) {
        return new BusinessException(
                ErrorCode.INVALID_REQUEST_PARAMETER, message,
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public record CreationScope(
            CampaignScopeType type,
            Set<String> cinemaPublicIds) {
        public CreationScope {
            cinemaPublicIds = cinemaPublicIds == null ? Set.of() : Set.copyOf(cinemaPublicIds);
        }
    }
}
