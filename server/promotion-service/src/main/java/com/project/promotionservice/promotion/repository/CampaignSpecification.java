package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class CampaignSpecification {

    private CampaignSpecification() {
    }

    public static Specification<PromotionCampaign> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<PromotionCampaign> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<PromotionCampaign> hasCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.isBlank()) {
                return null;
            }
            return cb.equal(root.get("code"), code);
        };
    }

    public static Specification<PromotionCampaign> hasStatus(CampaignStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<PromotionCampaign> startsAfter(Instant from) {
        return (root, query, cb) -> {
            if (from == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("startAt"), from);
        };
    }

    public static Specification<PromotionCampaign> endsBefore(Instant to) {
        return (root, query, cb) -> {
            if (to == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("endAt"), to);
        };
    }
}
