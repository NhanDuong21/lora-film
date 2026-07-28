package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionRule;
import org.springframework.data.jpa.domain.Specification;

public final class RuleSpecification {

    private RuleSpecification() {
    }

    public static Specification<PromotionRule> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<PromotionRule> hasCampaignPublicId(String campaignPublicId) {
        return (root, query, cb) -> {
            if (campaignPublicId == null || campaignPublicId.isBlank()) {
                return null;
            }
            return cb.equal(root.get("campaignPublicId"), campaignPublicId);
        };
    }

    public static Specification<PromotionRule> hasCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.isBlank()) {
                return null;
            }
            return cb.equal(root.get("code"), code);
        };
    }

    public static Specification<PromotionRule> isEnabled(Boolean enabled) {
        return (root, query, cb) -> {
            if (enabled == null) {
                return null;
            }
            return cb.equal(root.get("enabled"), enabled);
        };
    }
}
