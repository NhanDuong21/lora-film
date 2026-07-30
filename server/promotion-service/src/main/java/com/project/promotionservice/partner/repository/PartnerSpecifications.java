package com.project.promotionservice.partner.repository;

import com.project.promotionservice.partner.entity.Partner;
import com.project.promotionservice.partner.entity.PartnerSettlement;
import com.project.promotionservice.partner.enums.PartnerStatus;
import com.project.promotionservice.partner.enums.SettlementStatus;
import org.springframework.data.jpa.domain.Specification;

public final class PartnerSpecifications {
    private PartnerSpecifications() {}

    public static Specification<Partner> partnerSearch(String keyword, PartnerStatus status) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates.getExpressions().add(cb.isNull(root.get("deletedAt")));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.getExpressions().add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (status != null) {
                predicates.getExpressions().add(cb.equal(root.get("status"), status));
            }
            return predicates;
        };
    }

    public static Specification<PartnerSettlement> settlementSearch(
            String partnerPublicId, String campaignPublicId, SettlementStatus status) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates.getExpressions().add(cb.isNull(root.get("deletedAt")));
            if (partnerPublicId != null) {
                predicates.getExpressions().add(cb.equal(root.get("partnerPublicId"), partnerPublicId));
            }
            if (campaignPublicId != null) {
                predicates.getExpressions().add(cb.equal(root.get("campaignPublicId"), campaignPublicId));
            }
            if (status != null) {
                predicates.getExpressions().add(cb.equal(root.get("status"), status));
            }
            return predicates;
        };
    }
}
