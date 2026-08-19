package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class PromotionSpecifications {

    private PromotionSpecifications() {
    }

    public static Specification<Promotion> filter(
            String campaignPublicId,
            PromotionType type,
            PromotionStatus status,
            Boolean publicVisible,
            String keyword) {
        return filter(campaignPublicId, type, status, publicVisible, keyword, null);
    }

    public static Specification<Promotion> filter(
            String campaignPublicId,
            PromotionType type,
            PromotionStatus status,
            Boolean publicVisible,
            String keyword,
            Collection<String> accessibleCampaignIds) {
        return (root, query, cb) -> {
            var predicate = cb.isNull(root.get("deletedAt"));
            if (accessibleCampaignIds != null) {
                predicate = accessibleCampaignIds.isEmpty()
                        ? cb.and(predicate, cb.disjunction())
                        : cb.and(predicate,
                                root.get("campaignPublicId").in(accessibleCampaignIds));
            }
            if (campaignPublicId != null && !campaignPublicId.isBlank()) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("campaignPublicId"), campaignPublicId));
            }
            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("promotionType"), type));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (publicVisible != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("publicVisible"), publicVisible));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern)));
            }
            return predicate;
        };
    }
}
