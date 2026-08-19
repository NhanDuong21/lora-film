package com.project.promotionservice.common.monitoring;

import com.project.promotionservice.common.monitoring.PromotionOperationsSearchResponse.LedgerItem;
import com.project.promotionservice.promotion.entity.PromotionRedemption;
import com.project.promotionservice.promotion.entity.PromotionRedemptionAdjustment;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.repository.PromotionRedemptionAdjustmentRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReleaseReasonType;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PromotionOperationsSearchService {

    private final PromotionReservationRepository reservationRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionRedemptionAdjustmentRepository adjustmentRepository;

    public PromotionOperationsSearchService(
            PromotionReservationRepository reservationRepository,
            PromotionRedemptionRepository redemptionRepository,
            PromotionRedemptionAdjustmentRepository adjustmentRepository) {
        this.reservationRepository = reservationRepository;
        this.redemptionRepository = redemptionRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    @Transactional(readOnly = true)
    public PromotionOperationsSearchResponse search(
            String queryText, String campaignPublicId, String promotionPublicId,
            String reservationPublicId, String bookingPublicId,
            String paymentPublicId, String customerReference,
            ReleaseReasonType releaseReasonType, String status,
            Instant from, Instant to, int limit) {
        PageRequest pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PromotionReservation> reservations = reservationRepository.findAll(
                reservationSpec(queryText, campaignPublicId, promotionPublicId,
                        reservationPublicId, bookingPublicId, paymentPublicId,
                        customerReference, releaseReasonType, status, from, to), pageable);
        Page<PromotionRedemption> redemptions = redemptionRepository.findAll(
                redemptionSpec(queryText, campaignPublicId, promotionPublicId,
                        reservationPublicId, bookingPublicId, paymentPublicId,
                        customerReference, status, from, to), pageable);
        Page<PromotionRedemptionAdjustment> adjustments = adjustmentRepository.findAll(
                adjustmentSpec(queryText, reservationPublicId, status, from, to), pageable);
        return new PromotionOperationsSearchResponse(
                reservations.map(this::reservationItem).getContent(),
                redemptions.map(this::redemptionItem).getContent(),
                adjustments.map(this::adjustmentItem).getContent(),
                reservations.getTotalElements(), redemptions.getTotalElements(),
                adjustments.getTotalElements());
    }

    private Specification<PromotionReservation> reservationSpec(
            String text, String campaignId, String promotionId, String reservationId,
            String bookingId, String paymentId, String customer,
            ReleaseReasonType reasonType, String status, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            addEqual(predicates, cb, root, "publicId", reservationId);
            addEqual(predicates, cb, root, "bookingPublicId", bookingId);
            addEqual(predicates, cb, root, "paymentPublicId", paymentId);
            addEqual(predicates, cb, root, "userPublicId", customer);
            if (reasonType != null) predicates.add(cb.equal(root.get("releaseReasonType"), reasonType));
            ReservationStatus reservationStatus = enumValue(ReservationStatus.class, status);
            if (status != null && reservationStatus == null) predicates.add(cb.disjunction());
            if (reservationStatus != null) predicates.add(cb.equal(root.get("status"), reservationStatus));
            addRange(predicates, cb, root, "createdAt", from, to);
            if (hasText(text)) {
                String pattern = "%" + text.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        like(cb, root, "publicId", pattern), like(cb, root, "reservationCode", pattern),
                        like(cb, root, "bookingPublicId", pattern), like(cb, root, "orderPublicId", pattern),
                        like(cb, root, "paymentPublicId", pattern), like(cb, root, "userPublicId", pattern),
                        like(cb, root, "customerPhone", pattern), like(cb, root, "sourceReference", pattern),
                        like(cb, root, "reasonDetail", pattern)));
            }
            if (hasText(campaignId) || hasText(promotionId)) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<PromotionRedemption> redemption = subquery.from(PromotionRedemption.class);
                List<Predicate> sub = new ArrayList<>();
                sub.add(cb.equal(redemption.get("reservationPublicId"), root.get("publicId")));
                if (hasText(campaignId)) sub.add(cb.equal(redemption.get("campaignPublicId"), campaignId));
                if (hasText(promotionId)) sub.add(cb.equal(redemption.get("promotionPublicId"), promotionId));
                subquery.select(redemption.get("id")).where(sub.toArray(Predicate[]::new));
                predicates.add(cb.exists(subquery));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<PromotionRedemption> redemptionSpec(
            String text, String campaignId, String promotionId, String reservationId,
            String bookingId, String paymentId, String customer, String status,
            Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            addEqual(predicates, cb, root, "campaignPublicId", campaignId);
            addEqual(predicates, cb, root, "promotionPublicId", promotionId);
            addEqual(predicates, cb, root, "reservationPublicId", reservationId);
            addEqual(predicates, cb, root, "bookingPublicId", bookingId);
            addEqual(predicates, cb, root, "paymentPublicId", paymentId);
            addEqual(predicates, cb, root, "userPublicId", customer);
            PromotionRedemptionStatus redemptionStatus = enumValue(PromotionRedemptionStatus.class, status);
            if (status != null && redemptionStatus == null) predicates.add(cb.disjunction());
            if (redemptionStatus != null) predicates.add(cb.equal(root.get("status"), redemptionStatus));
            addRange(predicates, cb, root, "createdAt", from, to);
            if (hasText(text)) {
                String pattern = "%" + text.trim().toLowerCase() + "%";
                predicates.add(cb.or(like(cb, root, "publicId", pattern),
                        like(cb, root, "promotionName", pattern), like(cb, root, "promotionCode", pattern),
                        like(cb, root, "reservationPublicId", pattern), like(cb, root, "bookingPublicId", pattern),
                        like(cb, root, "paymentPublicId", pattern), like(cb, root, "userPublicId", pattern),
                        like(cb, root, "customerPhone", pattern), like(cb, root, "rollbackReason", pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<PromotionRedemptionAdjustment> adjustmentSpec(
            String text, String reservationId, String status, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            addEqual(predicates, cb, root, "reservationPublicId", reservationId);
            addEqual(predicates, cb, root, "adjustmentType", status);
            addRange(predicates, cb, root, "occurredAt", from, to);
            if (hasText(text)) {
                String pattern = "%" + text.trim().toLowerCase() + "%";
                predicates.add(cb.or(like(cb, root, "publicId", pattern),
                        like(cb, root, "reservationPublicId", pattern),
                        like(cb, root, "redemptionPublicId", pattern),
                        like(cb, root, "reasonCode", pattern), like(cb, root, "reason", pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private LedgerItem reservationItem(PromotionReservation item) {
        return new LedgerItem("RESERVATION", item.getPublicId(), item.getStatus().name(),
                null, null, item.getPublicId(), item.getBookingPublicId(), item.getOrderPublicId(),
                item.getPaymentPublicId(), item.getUserPublicId(),
                item.getReleaseReasonType() == null ? null : item.getReleaseReasonType().name(),
                item.getReasonDetail(), item.getSourceReference(), item.getDiscountAmount(),
                item.getCreatedAt());
    }

    private LedgerItem redemptionItem(PromotionRedemption item) {
        return new LedgerItem("REDEMPTION", item.getPublicId(), item.getStatus().name(),
                item.getCampaignPublicId(), item.getPromotionPublicId(), item.getReservationPublicId(),
                item.getBookingPublicId(), item.getOrderPublicId(), item.getPaymentPublicId(),
                item.getUserPublicId(), null, item.getRollbackReason(), null,
                item.getDiscountAmount(), item.getCreatedAt());
    }

    private LedgerItem adjustmentItem(PromotionRedemptionAdjustment item) {
        return new LedgerItem("ADJUSTMENT", item.getPublicId(), item.getAdjustmentType(),
                null, item.getRedemptionPublicId(), item.getReservationPublicId(),
                null, null, null, null, item.getReasonCode(), item.getReason(), null,
                item.getDiscountAmount(), item.getOccurredAt());
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static <T> void addEqual(List<Predicate> list, jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<T> root, String field, String value) {
        if (hasText(value)) list.add(cb.equal(root.get(field), value.trim()));
    }
    private static <T> void addRange(List<Predicate> list, jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<T> root, String field, Instant from, Instant to) {
        if (from != null) list.add(cb.greaterThanOrEqualTo(root.get(field), from));
        if (to != null) list.add(cb.lessThanOrEqualTo(root.get(field), to));
    }
    private static <T> Predicate like(jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<T> root, String field, String pattern) {
        return cb.like(cb.lower(root.get(field).as(String.class)), pattern);
    }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (!hasText(value)) return null;
        try { return Enum.valueOf(type, value.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
