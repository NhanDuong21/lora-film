package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.promotion.service.PromotionConditionEvaluator.EvaluationContext;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.promotion.dto.request.PromotionCheckoutRequest;
import com.project.promotionservice.promotion.dto.response.AppliedPromotionResponse;
import com.project.promotionservice.promotion.dto.response.PromotionCheckoutResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class PromotionEngineService {

    private static final Set<PromotionRedemptionStatus> CAPACITY_STATUSES =
            EnumSet.of(PromotionRedemptionStatus.RESERVED,
                    PromotionRedemptionStatus.CONFIRMED);
    private static final Set<PromotionRedemptionStatus> RESERVED_STATUS =
            EnumSet.of(PromotionRedemptionStatus.RESERVED);
    private static final int MAX_COMPATIBILITY_CANDIDATES = 20;

    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository walletRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionConditionEvaluator conditionEvaluator;
    private final PromotionDiscountCalculator discountCalculator;
    private final ObjectMapper objectMapper;

    public PromotionEngineService(
            PromotionRepository promotionRepository,
            UserPromotionRepository walletRepository,
            PromotionRedemptionRepository redemptionRepository,
            PromotionCampaignRepository campaignRepository,
            PromotionConditionEvaluator conditionEvaluator,
            PromotionDiscountCalculator discountCalculator,
            ObjectMapper objectMapper) {
        this.promotionRepository = promotionRepository;
        this.walletRepository = walletRepository;
        this.redemptionRepository = redemptionRepository;
        this.campaignRepository = campaignRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.discountCalculator = discountCalculator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PromotionCheckoutResponse preview(PromotionCheckoutRequest request) {
        BigDecimal original = money(request.originalAmount());
        Instant now = Instant.now();
        List<Candidate> candidates = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> addedPromotions = new HashSet<>();

        for (Promotion promotion : promotionRepository.findRuntimeCandidates(
                PromotionType.AUTO, PromotionStatus.ACTIVE, now)) {
            addCandidate(candidates, warnings, addedPromotions,
                    promotion, null, request, original, now, false);
        }

        List<String> selectedWalletIds = request.selectedUserPromotionPublicIds() == null
                ? List.of() : request.selectedUserPromotionPublicIds();
        for (String walletPublicId : selectedWalletIds) {
            UserPromotion wallet = requireWallet(walletPublicId, request.userPublicId(), now);
            Promotion promotion = requirePromotion(wallet.getPromotionPublicId());
            if (promotion.getPromotionType() == PromotionType.AUTO) {
                throw invalid("AUTO promotion cannot be selected from a wallet");
            }
            addCandidate(candidates, warnings, addedPromotions,
                    promotion, wallet, request, original, now, true);
        }

        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            Promotion coupon = promotionRepository
                    .findByPromotionTypeAndCodeIgnoreCaseAndDeletedAtIsNull(
                            PromotionType.COUPON, request.couponCode().trim())
                    .orElseThrow(() -> new BusinessException(
                            "COUPON_NOT_FOUND", "Coupon code was not found",
                            HttpStatus.NOT_FOUND));
            Optional<UserPromotion> wallet = walletRepository
                    .findByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNull(
                            request.userPublicId(), coupon.getPublicId());
            wallet.ifPresent(value -> requireWallet(
                    value.getPublicId(), request.userPublicId(), now));
            addCandidate(candidates, warnings, addedPromotions,
                    coupon, wallet.orElse(null), request, original, now, true);
        }

        List<Candidate> selected = selectBest(candidates, original);
        List<AppliedPromotionResponse> applied = allocate(selected, original);
        BigDecimal discount = applied.stream()
                .map(AppliedPromotionResponse::discountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PromotionCheckoutResponse(
                discount.signum() > 0,
                original,
                money(discount),
                money(original.subtract(discount).max(BigDecimal.ZERO)),
                normalizeCurrency(request.currency()),
                applied,
                List.copyOf(warnings));
    }

    private void addCandidate(
            List<Candidate> candidates,
            List<String> warnings,
            Set<String> addedPromotions,
            Promotion promotion,
            UserPromotion wallet,
            PromotionCheckoutRequest request,
            BigDecimal original,
            Instant now,
            boolean selectedByCustomer) {
        if (!addedPromotions.add(promotion.getPublicId())) {
            return;
        }
        try {
            PromotionCampaign campaign = requireRuntimeCampaign(
                    promotion.getCampaignPublicId(), now);
            requirePromotionRuntimeActive(promotion, now);
            JsonNode conditions = read(promotion.getConditionsJson());
            conditionEvaluator.evaluate(conditions, new EvaluationContext(
                    original, request.userPublicId(), request.contextJson()));
            BigDecimal discount = discountCalculator.calculate(
                    read(promotion.getActionsJson()), original, request.contextJson());
            requireCapacity(promotion, wallet, campaign, request.userPublicId(), discount);
            if (discount.signum() > 0) {
                candidates.add(new Candidate(
                        promotion, wallet, campaign, conditions, discount));
            }
        } catch (BusinessException exception) {
            if (selectedByCustomer) {
                throw exception;
            }
            warnings.add(promotion.getName() + ": " + exception.getMessage());
        }
    }

    private List<Candidate> selectBest(
            List<Candidate> candidates, BigDecimal originalAmount) {
        Selection best = Selection.empty();
        for (Candidate candidate : candidates) {
            if (!Boolean.TRUE.equals(candidate.promotion().getStackable())) {
                best = better(best, Selection.of(List.of(candidate)), originalAmount);
            }
        }

        List<Candidate> stackable = candidates.stream()
                .filter(candidate -> Boolean.TRUE.equals(
                        candidate.promotion().getStackable()))
                .sorted(candidateOrder())
                .toList();
        if (stackable.isEmpty()) {
            return best.candidates();
        }
        if (hasCompatibilityRules(stackable)
                && stackable.size() > MAX_COMPATIBILITY_CANDIDATES) {
            throw new BusinessException(
                    "PROMOTION_CONFIGURATION_TOO_COMPLEX",
                    "At most 20 stackable promotions may use compatibility lists",
                    HttpStatus.CONFLICT);
        }
        if (!hasCompatibilityRules(stackable)) {
            best = better(best, Selection.of(stackable), originalAmount);
            return best.candidates();
        }

        SelectionAccumulator accumulator = new SelectionAccumulator(best, originalAmount);
        searchCompatible(stackable, 0, new ArrayList<>(), accumulator);
        return accumulator.best.candidates();
    }

    private void searchCompatible(
            List<Candidate> candidates,
            int index,
            List<Candidate> selected,
            SelectionAccumulator accumulator) {
        if (index == candidates.size()) {
            accumulator.best = better(
                    accumulator.best, Selection.of(List.copyOf(selected)),
                    accumulator.originalAmount);
            return;
        }
        searchCompatible(candidates, index + 1, selected, accumulator);
        Candidate next = candidates.get(index);
        if (selected.stream().allMatch(current -> compatible(current, next))) {
            selected.add(next);
            searchCompatible(candidates, index + 1, selected, accumulator);
            selected.remove(selected.size() - 1);
        }
    }

    private Selection better(
            Selection current, Selection candidate, BigDecimal originalAmount) {
        BigDecimal currentDiscount = current.discount().min(originalAmount);
        BigDecimal candidateDiscount = candidate.discount().min(originalAmount);
        int discountComparison = candidateDiscount.compareTo(currentDiscount);
        if (discountComparison != 0) {
            return discountComparison > 0 ? candidate : current;
        }
        int walletComparison = Integer.compare(
                candidate.walletItems(), current.walletItems());
        if (walletComparison != 0) {
            return walletComparison < 0 ? candidate : current;
        }
        int sizeComparison = Integer.compare(
                candidate.candidates().size(), current.candidates().size());
        if (sizeComparison != 0) {
            return sizeComparison < 0 ? candidate : current;
        }
        return candidate.priorityScore() < current.priorityScore()
                ? candidate : current;
    }

    private List<AppliedPromotionResponse> allocate(
            List<Candidate> selected, BigDecimal originalAmount) {
        List<Candidate> ordered = selected.stream().sorted(candidateOrder()).toList();
        List<AppliedPromotionResponse> result = new ArrayList<>();
        BigDecimal remaining = originalAmount;
        for (Candidate candidate : ordered) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal allocated = candidate.discount().min(remaining);
            if (allocated.signum() <= 0) {
                continue;
            }
            Promotion promotion = candidate.promotion();
            result.add(new AppliedPromotionResponse(
                    promotion.getPublicId(),
                    candidate.wallet() == null ? null : candidate.wallet().getPublicId(),
                    promotion.getCampaignPublicId(), promotion.getPromotionType(),
                    promotion.getCode(), promotion.getName(), money(allocated),
                    promotion.getPriority(), Boolean.TRUE.equals(promotion.getStackable())));
            remaining = money(remaining.subtract(allocated));
        }
        return List.copyOf(result);
    }

    private boolean compatible(Candidate first, Candidate second) {
        if (!Boolean.TRUE.equals(first.promotion().getStackable())
                || !Boolean.TRUE.equals(second.promotion().getStackable())) {
            return false;
        }
        if (first.promotion().getPromotionType() == PromotionType.VOUCHER
                && second.promotion().getPromotionType() == PromotionType.VOUCHER
                && (!first.conditions().path("allowMultipleVoucherPerOrder").asBoolean(false)
                || !second.conditions().path("allowMultipleVoucherPerOrder").asBoolean(false))) {
            return false;
        }
        return allows(first, second) && allows(second, first);
    }

    private boolean allows(Candidate owner, Candidate other) {
        JsonNode blocked = owner.conditions().get("notStackableWith");
        if (matches(blocked, other.promotion())) {
            return false;
        }
        JsonNode allowed = owner.conditions().get("stackableWith");
        return allowed == null || !allowed.isArray() || allowed.isEmpty()
                || matches(allowed, other.promotion());
    }

    private boolean matches(JsonNode values, Promotion promotion) {
        if (values == null || !values.isArray()) {
            return false;
        }
        for (JsonNode value : values) {
            String token = value.asText();
            if (token.equalsIgnoreCase(promotion.getPublicId())
                    || token.equalsIgnoreCase(promotion.getPromotionType().name())
                    || (promotion.getCode() != null
                    && token.equalsIgnoreCase(promotion.getCode()))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCompatibilityRules(List<Candidate> candidates) {
        long voucherCount = candidates.stream()
                .filter(candidate -> candidate.promotion().getPromotionType() == PromotionType.VOUCHER)
                .count();
        return voucherCount > 1 || candidates.stream().anyMatch(candidate ->
                configuredArray(candidate.conditions().get("stackableWith"))
                        || configuredArray(candidate.conditions().get("notStackableWith"))
                        || candidate.conditions().has("allowMultipleVoucherPerOrder"));
    }

    private boolean configuredArray(JsonNode value) {
        return value != null && value.isArray() && !value.isEmpty();
    }

    private void requireCapacity(
            Promotion promotion,
            UserPromotion wallet,
            PromotionCampaign campaign,
            String userPublicId,
            BigDecimal discount) {
        long promotionUsage = redemptionRepository
                .countByPromotionPublicIdAndStatusInAndDeletedAtIsNull(
                        promotion.getPublicId(), CAPACITY_STATUSES);
        if (promotion.getMaxRedemptions() != null
                && promotionUsage >= promotion.getMaxRedemptions()) {
            throw conflict("Promotion redemption capacity is exhausted");
        }
        long userUsage = redemptionRepository
                .countByPromotionPublicIdAndUserPublicIdAndStatusInAndDeletedAtIsNull(
                        promotion.getPublicId(), userPublicId, CAPACITY_STATUSES);
        if (userUsage >= promotion.getMaxRedemptionsPerUser()) {
            throw conflict("Promotion usage limit for this customer is exhausted");
        }
        if (wallet != null) {
            long walletReservations = redemptionRepository
                    .countByUserPromotionPublicIdAndStatusInAndDeletedAtIsNull(
                            wallet.getPublicId(), RESERVED_STATUS);
            if (wallet.getUsageCount() + walletReservations >= wallet.getMaxUsage()) {
                throw conflict("Wallet promotion usage is exhausted");
            }
        }
        long campaignUsage = redemptionRepository.countCampaignRedemptions(
                campaign.getPublicId(), CAPACITY_STATUSES);
        if (campaign.getMaxRedemptions() != null
                && campaignUsage >= campaign.getMaxRedemptions()) {
            throw conflict("Campaign redemption capacity is exhausted");
        }
        long campaignUserUsage = redemptionRepository.countCampaignUserRedemptions(
                campaign.getPublicId(), userPublicId, CAPACITY_STATUSES);
        if (campaignUserUsage >= campaign.getMaxRedemptionsPerUser()) {
            throw conflict("Campaign usage limit for this customer is exhausted");
        }
        BigDecimal availableBudget = campaign.getBudgetRemaining()
                .subtract(campaign.getBudgetReserved());
        if (availableBudget.compareTo(discount) < 0) {
            throw conflict("Campaign budget is exhausted");
        }
    }

    private UserPromotion requireWallet(
            String publicId, String userPublicId, Instant now) {
        UserPromotion wallet = walletRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(
                        "WALLET_PROMOTION_NOT_FOUND", "Wallet promotion was not found",
                        HttpStatus.NOT_FOUND));
        if (!Objects.equals(wallet.getUserPublicId(), userPublicId)) {
            throw new BusinessException(
                    "WALLET_PROMOTION_FORBIDDEN",
                    "Wallet promotion belongs to another customer",
                    HttpStatus.FORBIDDEN);
        }
        if (wallet.getStatus() != UserPromotionStatus.AVAILABLE
                || now.isBefore(wallet.getValidFrom())
                || !now.isBefore(wallet.getValidTo())
                || wallet.getUsageCount() >= wallet.getMaxUsage()) {
            throw conflict("Wallet promotion is not available");
        }
        return wallet;
    }

    private Promotion requirePromotion(String publicId) {
        return promotionRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(
                        "PROMOTION_NOT_FOUND", "Promotion was not found",
                        HttpStatus.NOT_FOUND));
    }

    private PromotionCampaign requireRuntimeCampaign(String publicId, Instant now) {
        PromotionCampaign campaign = campaignRepository
                .findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(
                        "CAMPAIGN_NOT_FOUND", "Promotion campaign was not found",
                        HttpStatus.NOT_FOUND));
        if (campaign.getStatus() != CampaignStatus.ACTIVE
                || campaign.getLegalStatus() != LegalStatus.PASSED
                || Boolean.TRUE.equals(campaign.getKillSwitch())
                || now.isBefore(campaign.getStartAt())
                || !now.isBefore(campaign.getEndAt())) {
            throw conflict("Promotion campaign is not active");
        }
        return campaign;
    }

    private void requirePromotionRuntimeActive(Promotion promotion, Instant now) {
        if (promotion.getStatus() != PromotionStatus.ACTIVE
                || now.isBefore(promotion.getValidFrom())
                || !now.isBefore(promotion.getValidTo())) {
            throw conflict("Promotion is not active");
        }
    }

    private JsonNode read(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw invalid("Promotion JSON configuration is invalid");
        }
    }

    private Comparator<Candidate> candidateOrder() {
        return Comparator
                .comparingInt((Candidate candidate) -> phase(candidate.promotion()))
                .thenComparingInt(candidate -> candidate.promotion().getPriority())
                .thenComparing(candidate -> candidate.promotion().getPublicId());
    }

    private int phase(Promotion promotion) {
        return switch (promotion.getPromotionType()) {
            case AUTO -> 0;
            case VOUCHER -> 1;
            case COUPON -> 2;
        };
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank()
                ? "VND" : currency.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw invalid("originalAmount is required");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException invalid(String message) {
        return new BusinessException(
                "PROMOTION_CHECKOUT_INVALID", message, HttpStatus.BAD_REQUEST);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(
                "PROMOTION_CAPACITY_CONFLICT", message, HttpStatus.CONFLICT);
    }

    private record Candidate(
            Promotion promotion,
            UserPromotion wallet,
            PromotionCampaign campaign,
            JsonNode conditions,
            BigDecimal discount) {
    }

    private record Selection(
            List<Candidate> candidates,
            BigDecimal discount,
            int walletItems,
            int priorityScore) {

        private static Selection empty() {
            return new Selection(List.of(), BigDecimal.ZERO, 0, Integer.MAX_VALUE);
        }

        private static Selection of(List<Candidate> candidates) {
            BigDecimal discount = candidates.stream()
                    .map(Candidate::discount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int walletItems = (int) candidates.stream()
                    .filter(candidate -> candidate.wallet() != null
                            || candidate.promotion().getPromotionType() == PromotionType.COUPON)
                    .count();
            int priority = candidates.stream()
                    .mapToInt(candidate -> candidate.promotion().getPriority())
                    .sum();
            return new Selection(candidates, discount, walletItems, priority);
        }
    }

    private static final class SelectionAccumulator {
        private Selection best;
        private final BigDecimal originalAmount;

        private SelectionAccumulator(Selection best, BigDecimal originalAmount) {
            this.best = best;
            this.originalAmount = originalAmount;
        }
    }
}
