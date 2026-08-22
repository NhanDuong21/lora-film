package com.project.promotionservice.promotion.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.promotion.dto.request.PromotionUpsertRequest;
import com.project.promotionservice.promotion.dto.response.PromotionResponse;
import com.project.promotionservice.promotion.dto.response.WalletPromotionResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.PromotionStackingBlockedReason;
import com.project.promotionservice.promotion.enums.PromotionDistributionMode;
import com.project.promotionservice.promotion.enums.PromotionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

    private final ObjectMapper objectMapper;

    public PromotionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Promotion create(PromotionUpsertRequest request) {
        Promotion promotion = new Promotion();
        apply(promotion, request);
        return promotion;
    }

    public void apply(Promotion promotion, PromotionUpsertRequest request) {
        promotion.setCampaignPublicId(request.campaignPublicId().trim());
        promotion.setPromotionType(request.promotionType());
        promotion.setCode(normalizeCode(request.code()));
        promotion.setName(request.name().trim());
        promotion.setDescription(blankToNull(request.description()));
        promotion.setPublicVisible(request.publicVisible());
        promotion.setDistributionMode(distributionMode(request));
        promotion.setPriority(request.priority());
        promotion.setStackable(request.stackable());
        promotion.setConditionsJson(write(request.conditionsJson()));
        promotion.setActionsJson(write(request.actionsJson()));
        promotion.setMetadataJson(writeNullable(request.metadataJson()));
        promotion.setMaxRedemptions(request.maxRedemptions());
        promotion.setMaxRedemptionsPerUser(request.maxRedemptionsPerUser());
        promotion.setValidFrom(request.validFrom());
        promotion.setValidTo(request.validTo());
    }

    public PromotionResponse response(
            Promotion promotion, PromotionCampaign campaign) {
        boolean promotionStackable = Boolean.TRUE.equals(promotion.getStackable());
        boolean campaignStackable = campaign != null
                && Boolean.TRUE.equals(campaign.getStackable());
        PromotionStackingBlockedReason blockedReason = null;
        if (!promotionStackable) {
            blockedReason = PromotionStackingBlockedReason.PROMOTION_STACKING_DISABLED;
        } else if (campaign == null) {
            blockedReason = PromotionStackingBlockedReason.CAMPAIGN_NOT_AVAILABLE;
        } else if (!campaignStackable) {
            blockedReason = PromotionStackingBlockedReason.CAMPAIGN_STACKING_DISABLED;
        }
        return new PromotionResponse(
                promotion.getPublicId(), promotion.getCampaignPublicId(),
                promotion.getClonedFromPublicId(),
                promotion.getPromotionType(), promotion.getCode(), promotion.getName(),
                promotion.getDescription(), promotion.getStatus(),
                Boolean.TRUE.equals(promotion.getPublicVisible()),
                effectiveDistributionMode(promotion),
                campaign != null && Boolean.TRUE.equals(campaign.getTestData()),
                campaign == null ? null : campaign.getEnvironmentTag(),
                promotion.getPriority(),
                promotionStackable, campaignStackable,
                promotionStackable && campaignStackable, blockedReason,
                read(promotion.getConditionsJson()),
                read(promotion.getActionsJson()), readNullable(promotion.getMetadataJson()),
                promotion.getMaxRedemptions(), promotion.getRedemptionCount(),
                promotion.getMaxRedemptionsPerUser(), promotion.getValidFrom(),
                promotion.getValidTo(), promotion.getCreatedAt(), promotion.getUpdatedAt());
    }

    public WalletPromotionResponse wallet(
            UserPromotion wallet, Promotion promotion, PromotionCampaign campaign) {
        return new WalletPromotionResponse(
                wallet.getPublicId(), wallet.getUserPublicId(), wallet.getStatus(),
                wallet.getClaimedAt(), wallet.getValidFrom(), wallet.getValidTo(),
                wallet.getUsageCount(), wallet.getMaxUsage(), true, null, null,
                response(promotion, campaign));
    }

    private String normalizeCode(String code) {
        String normalized = blankToNull(code);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private PromotionDistributionMode distributionMode(PromotionUpsertRequest request) {
        JsonNode metadata = request.metadataJson();
        String configured = metadata == null ? null
                : metadata.path("distributionMode").asText(null);
        if (configured == null || configured.isBlank()) {
            configured = metadata == null ? null
                    : metadata.path("distributionModel").asText(null);
        }
        if (configured != null) {
            try {
                return switch (configured.trim().toUpperCase()) {
                    case "AUTO" -> PromotionDistributionMode.AUTO_APPLY;
                    case "VOUCHER_PUBLIC" -> PromotionDistributionMode.CLAIMABLE_WALLET;
                    case "VOUCHER_PRIVATE" -> PromotionDistributionMode.ASSIGNED_WALLET;
                    case "COUPON_PRIVATE" -> PromotionDistributionMode.PERSONAL_CODE;
                    default -> PromotionDistributionMode.valueOf(
                            configured.trim().toUpperCase());
                };
            } catch (IllegalArgumentException ignored) {
                // Fall through to the backwards-compatible semantic default.
            }
        }
        if (request.promotionType() == PromotionType.AUTO) {
            return PromotionDistributionMode.AUTO_APPLY;
        }
        if (request.promotionType() == PromotionType.COUPON) {
            return PromotionDistributionMode.PERSONAL_CODE;
        }
        return Boolean.TRUE.equals(request.publicVisible())
                ? PromotionDistributionMode.CLAIMABLE_WALLET
                : PromotionDistributionMode.ASSIGNED_WALLET;
    }

    public PromotionDistributionMode effectiveDistributionMode(Promotion promotion) {
        if (promotion.getDistributionMode() != null) {
            return promotion.getDistributionMode();
        }
        if (promotion.getPromotionType() == PromotionType.AUTO) {
            return PromotionDistributionMode.AUTO_APPLY;
        }
        if (promotion.getPromotionType() == PromotionType.COUPON) {
            return PromotionDistributionMode.PERSONAL_CODE;
        }
        return Boolean.TRUE.equals(promotion.getPublicVisible())
                ? PromotionDistributionMode.CLAIMABLE_WALLET
                : PromotionDistributionMode.ASSIGNED_WALLET;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String write(JsonNode value) {
        if (value == null) {
            throw invalidJson();
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw invalidJson();
        }
    }

    private String writeNullable(JsonNode value) {
        return value == null || value.isNull() ? null : write(value);
    }

    private JsonNode read(String value) {
        JsonNode parsed = readNullable(value);
        if (parsed == null) {
            throw invalidJson();
        }
        return parsed;
    }

    private JsonNode readNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw invalidJson();
        }
    }

    private BusinessException invalidJson() {
        return new BusinessException(
                "PROMOTION_JSON_INVALID", "Promotion JSON configuration is invalid",
                HttpStatus.BAD_REQUEST);
    }
}
