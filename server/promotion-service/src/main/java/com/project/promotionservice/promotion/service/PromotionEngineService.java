package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.promotion.service.PromotionConditionEvaluator.EvaluationContext;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.promotion.dto.request.PromotionCheckoutRequest;
import com.project.promotionservice.promotion.dto.response.AppliedPromotionResponse;
import com.project.promotionservice.promotion.dto.response.PromotionCheckoutResponse;
import com.project.promotionservice.promotion.dto.response.PromotionEligibilityResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PromotionEngineService {

    private static final Set<PromotionRedemptionStatus> CAPACITY_STATUSES =
            EnumSet.of(PromotionRedemptionStatus.RESERVED,
                    PromotionRedemptionStatus.CONFIRMED);
    private static final Set<PromotionRedemptionStatus> RESERVED_STATUS =
            EnumSet.of(PromotionRedemptionStatus.RESERVED);

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

        List<String> selectedPromotionIds = request.selectedPromotionPublicIds() == null
                ? List.of() : request.selectedPromotionPublicIds();
        List<String> selectedWalletIds = request.selectedUserPromotionPublicIds() == null
                ? List.of() : request.selectedUserPromotionPublicIds();
        requireSingleManualSelection(
                selectedPromotionIds, selectedWalletIds, request.couponCode());
        for (String promotionPublicId : selectedPromotionIds) {
            Promotion promotion = requirePromotion(promotionPublicId);
            if (promotion.getPromotionType() != PromotionType.AUTO) {
                throw invalid("Only system promotions can be selected by promotionPublicId");
            }
            addCandidate(candidates, warnings, addedPromotions,
                    promotion, null, request, original, now, true);
        }
        // AUTO promotions always compete with a customer's manual choice. The
        // selection policy evaluates every valid option and keeps the best price.
        for (Promotion promotion : promotionRepository.findRuntimeCandidates(
                PromotionType.AUTO, PromotionStatus.ACTIVE, now)) {
            addCandidate(candidates, warnings, addedPromotions,
                    promotion, null, request, original, now, false);
        }

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
            UserPromotion couponGrant = walletRepository
                    .findFirstByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNullOrderByIdDesc(
                            request.userPublicId(), coupon.getPublicId())
                    .orElseThrow(() -> new BusinessException(
                            "COUPON_NOT_ISSUED",
                            "Coupon was not issued to this customer",
                            HttpStatus.FORBIDDEN));
            requireWallet(couponGrant.getPublicId(), request.userPublicId(), now);
            addCandidate(candidates, warnings, addedPromotions,
                    coupon, couponGrant, request, original, now, true);
        }

        List<PromotionEligibilityResponse> evaluations = evaluateRequestedPromotions(
                request, original, now);

        List<Candidate> selected = selectBest(candidates, original);
        requireSelectionCapacity(selected, request.userPublicId(), original);
        List<AppliedPromotionResponse> applied = allocate(selected, original);
        BigDecimal discount = applied.stream()
                .map(AppliedPromotionResponse::discountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Candidate manualCandidate = candidates.stream()
                .filter(candidate -> candidate.promotion().getPromotionType()
                        != PromotionType.AUTO)
                .findFirst()
                .orElse(null);
        boolean manualSelectionReplaced = manualCandidate != null
                && selected.stream().noneMatch(candidate -> candidate == manualCandidate);
        BigDecimal additionalSavings = manualSelectionReplaced
                ? money(discount.subtract(manualCandidate.discount()).max(BigDecimal.ZERO))
                : BigDecimal.ZERO.setScale(2);
        if (manualSelectionReplaced) {
            String retainedBenefit = manualCandidate.promotion().getPromotionType()
                    == PromotionType.COUPON
                    ? "Mã ưu đãi chưa được sử dụng."
                    : "Voucher vẫn còn trong ví.";
            warnings.add(retainedBenefit + " Hệ thống đã tự áp dụng ưu đãi tốt hơn, "
                    + "giúp bạn tiết kiệm thêm " + additionalSavings.toPlainString() + " "
                    + normalizeCurrency(request.currency()));
        }
        return new PromotionCheckoutResponse(
                discount.signum() > 0,
                original,
                money(discount),
                money(original.subtract(discount).max(BigDecimal.ZERO)),
                normalizeCurrency(request.currency()),
                applied,
                evaluations,
                List.copyOf(warnings),
                manualSelectionReplaced,
                additionalSavings);
    }

    private List<PromotionEligibilityResponse> evaluateRequestedPromotions(
            PromotionCheckoutRequest request, BigDecimal original, Instant now) {
        List<PromotionEligibilityResponse> evaluations = new ArrayList<>();
        Set<String> evaluatedKeys = new HashSet<>();
        List<String> promotionIds = request.evaluationPromotionPublicIds() == null
                ? List.of() : request.evaluationPromotionPublicIds();
        for (String promotionPublicId : promotionIds) {
            if (!evaluatedKeys.add("promotion:" + promotionPublicId)) {
                continue;
            }
            Promotion promotion;
            try {
                promotion = requirePromotion(promotionPublicId);
            } catch (BusinessException exception) {
                evaluations.add(unavailableEvaluation(
                        promotionPublicId, null, null, exception));
                continue;
            }
            evaluations.add(evaluateForCheckout(
                    promotion, null, request, original, now));
        }

        List<String> walletIds = request.evaluationUserPromotionPublicIds() == null
                ? List.of() : request.evaluationUserPromotionPublicIds();
        for (String walletPublicId : walletIds) {
            if (!evaluatedKeys.add("wallet:" + walletPublicId)) {
                continue;
            }
            UserPromotion wallet;
            try {
                wallet = requireWallet(walletPublicId, request.userPublicId(), now);
            } catch (BusinessException exception) {
                evaluations.add(unavailableEvaluation(
                        null, walletPublicId, null, exception));
                continue;
            }
            Promotion promotion;
            try {
                promotion = requirePromotion(wallet.getPromotionPublicId());
            } catch (BusinessException exception) {
                evaluations.add(unavailableEvaluation(
                        wallet.getPromotionPublicId(), walletPublicId, null, exception));
                continue;
            }
            evaluations.add(evaluateForCheckout(
                    promotion, wallet, request, original, now));
        }
        return List.copyOf(evaluations);
    }

    private PromotionEligibilityResponse evaluateForCheckout(
            Promotion promotion,
            UserPromotion wallet,
            PromotionCheckoutRequest request,
            BigDecimal original,
            Instant now) {
        try {
            Candidate candidate = buildCandidate(
                    promotion, wallet, request, original, now);
            return new PromotionEligibilityResponse(
                    promotion.getPublicId(),
                    wallet == null ? null : wallet.getPublicId(),
                    promotion.getPromotionType(),
                    true,
                    money(candidate.discount()),
                    "ELIGIBLE",
                    "Có thể sử dụng cho đơn hiện tại");
        } catch (BusinessException exception) {
            return unavailableEvaluation(
                    promotion.getPublicId(),
                    wallet == null ? null : wallet.getPublicId(),
                    promotion.getPromotionType(),
                    exception);
        }
    }

    private PromotionEligibilityResponse unavailableEvaluation(
            String promotionPublicId,
            String walletPublicId,
            PromotionType promotionType,
            BusinessException exception) {
        String reasonCode = eligibilityReasonCode(exception);
        return new PromotionEligibilityResponse(
                promotionPublicId,
                walletPublicId,
                promotionType,
                false,
                BigDecimal.ZERO.setScale(2),
                reasonCode,
                eligibilityReason(reasonCode));
    }

    private String eligibilityReasonCode(BusinessException exception) {
        String message = String.valueOf(exception.getMessage()).toLowerCase(Locale.ROOT);
        if (message.contains("minimum order")) return "MINIMUM_ORDER_NOT_MET";
        if (message.contains("movieid") || message.contains("moviepublicid")) {
            return "MOVIE_NOT_APPLICABLE";
        }
        if (message.contains("cinemaid") || message.contains("cinemapublicid")) {
            return "CINEMA_NOT_APPLICABLE";
        }
        if (message.contains("showtimeid") || message.contains("showtimepublicid")
                || message.contains("day of week")
                || message.contains("business date")) return "SHOWTIME_NOT_APPLICABLE";
        if (message.contains("paymentmethod")) return "PAYMENT_METHOD_NOT_APPLICABLE";
        if (message.contains("channel")) return "CHANNEL_NOT_APPLICABLE";
        if (message.contains("format") || message.contains("room type")) {
            return "ROOM_FORMAT_NOT_APPLICABLE";
        }
        if (message.contains("seattype")) return "SEAT_TYPE_NOT_APPLICABLE";
        if (message.contains("order type")) return "ORDER_TYPE_NOT_APPLICABLE";
        if (message.contains("membership tier")) return "MEMBER_TIER_NOT_ELIGIBLE";
        if (message.contains("verified customer")) return "CUSTOMER_VERIFICATION_REQUIRED";
        if (message.contains("customer is not eligible")) return "CUSTOMER_NOT_ELIGIBLE";
        if (message.contains("wallet promotion")) return "WALLET_NOT_AVAILABLE";
        if (message.contains("campaign is not active")) return "CAMPAIGN_NOT_ACTIVE";
        if (message.contains("promotion is not active")) return "PROMOTION_NOT_ACTIVE";
        if (message.contains("capacity") || message.contains("usage limit")
                || message.contains("usage is exhausted")) return "USAGE_LIMIT_REACHED";
        if (message.contains("budget is exhausted")) return "CAMPAIGN_BUDGET_EXHAUSTED";
        if (exception.getErrorCode().contains("CONFIGURATION")
                || exception.getErrorCode().contains("ACTION_INVALID")) {
            return "PROMOTION_CONFIGURATION_INVALID";
        }
        if (exception.getErrorCode().contains("NOT_FOUND")) return "PROMOTION_NOT_FOUND";
        return "PROMOTION_CONDITION_NOT_MET";
    }

    private String eligibilityReason(String reasonCode) {
        return switch (reasonCode) {
            case "MINIMUM_ORDER_NOT_MET" -> "Chưa đủ giá trị đơn hàng tối thiểu";
            case "MOVIE_NOT_APPLICABLE" -> "Không áp dụng cho phim hiện tại";
            case "CINEMA_NOT_APPLICABLE" -> "Không áp dụng cho rạp hiện tại";
            case "SHOWTIME_NOT_APPLICABLE" -> "Không áp dụng cho suất chiếu hiện tại";
            case "PAYMENT_METHOD_NOT_APPLICABLE" -> "Không áp dụng với phương thức thanh toán hiện tại";
            case "CHANNEL_NOT_APPLICABLE" -> "Không áp dụng trên kênh đặt vé hiện tại";
            case "ROOM_FORMAT_NOT_APPLICABLE" -> "Không áp dụng cho định dạng phòng chiếu hiện tại";
            case "SEAT_TYPE_NOT_APPLICABLE" -> "Không áp dụng cho loại ghế đã chọn";
            case "ORDER_TYPE_NOT_APPLICABLE" -> "Không áp dụng cho loại đơn hàng này";
            case "MEMBER_TIER_NOT_ELIGIBLE" -> "Hạng thành viên chưa đáp ứng điều kiện";
            case "CUSTOMER_VERIFICATION_REQUIRED" -> "Tài khoản cần được xác thực";
            case "CUSTOMER_NOT_ELIGIBLE" -> "Khách hàng không thuộc nhóm được áp dụng";
            case "WALLET_NOT_AVAILABLE" -> "Voucher trong ví đã hết hạn hoặc hết lượt sử dụng";
            case "CAMPAIGN_NOT_ACTIVE" -> "Chiến dịch của voucher hiện không hoạt động";
            case "PROMOTION_NOT_ACTIVE" -> "Voucher chưa có hiệu lực hoặc đã hết hạn";
            case "USAGE_LIMIT_REACHED" -> "Voucher đã hết lượt sử dụng";
            case "CAMPAIGN_BUDGET_EXHAUSTED" -> "Ngân sách chiến dịch đã hết";
            case "PROMOTION_CONFIGURATION_INVALID" -> "Voucher đang được cấu hình lại";
            case "PROMOTION_NOT_FOUND" -> "Voucher không còn tồn tại";
            default -> "Không đáp ứng điều kiện áp dụng của voucher";
        };
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
            candidates.add(buildCandidate(
                    promotion, wallet, request, original, now));
        } catch (BusinessException exception) {
            if (selectedByCustomer) {
                throw exception;
            }
            warnings.add(promotion.getName() + ": " + exception.getMessage());
        }
    }

    private Candidate buildCandidate(
            Promotion promotion,
            UserPromotion wallet,
            PromotionCheckoutRequest request,
            BigDecimal original,
            Instant now) {
        PromotionCampaign campaign = requireRuntimeCampaign(
                promotion.getCampaignPublicId(), now);
        requirePromotionRuntimeActive(promotion, now);
        JsonNode conditions = read(promotion.getConditionsJson());
        conditionEvaluator.evaluate(conditions, new EvaluationContext(
                original, request.userPublicId(), request.contextJson()));
        JsonNode actions = read(promotion.getActionsJson());
        BigDecimal discount = discountCalculator.calculate(
                actions, original, request.contextJson());
        Candidate candidate = new Candidate(
                promotion, wallet, campaign, conditions, actions,
                request.contextJson(), discount);
        requireCapacity(promotion, wallet, campaign, request.userPublicId(), discount);
        if (discount.signum() <= 0) {
            throw invalid("Promotion discount must be greater than zero");
        }
        return candidate;
    }

    private List<Candidate> selectBest(
            List<Candidate> candidates, BigDecimal originalAmount) {
        List<Candidate> automaticCandidates = candidates.stream()
                .filter(candidate -> candidate.promotion().getPromotionType()
                        == PromotionType.AUTO)
                .toList();
        Candidate manualCandidate = candidates.stream()
                .filter(candidate -> candidate.promotion().getPromotionType()
                        != PromotionType.AUTO)
                .findFirst()
                .orElse(null);

        Selection best = manualCandidate == null
                ? Selection.empty()
                : selectionOf(List.of(manualCandidate), originalAmount);
        for (Candidate automaticCandidate : automaticCandidates) {
            best = better(best, selectionOf(
                    List.of(automaticCandidate), originalAmount));
            if (canStack(manualCandidate, automaticCandidate)) {
                best = better(best, selectionOf(
                        List.of(automaticCandidate, manualCandidate),
                        originalAmount));
            }
        }
        return best.candidates();
    }

    private boolean canStack(Candidate manual, Candidate automatic) {
        if (manual == null
                || manual.promotion().getPromotionType() == PromotionType.AUTO
                || automatic.promotion().getPromotionType() != PromotionType.AUTO) {
            return false;
        }
        if (!Boolean.TRUE.equals(manual.promotion().getStackable())
                || !Boolean.TRUE.equals(automatic.promotion().getStackable())
                || !Boolean.TRUE.equals(manual.campaign().getStackable())
                || !Boolean.TRUE.equals(automatic.campaign().getStackable())) {
            return false;
        }
        boolean differentCampaign = !Objects.equals(
                manual.campaign().getPublicId(), automatic.campaign().getPublicId());
        return !differentCampaign
                || (!Boolean.TRUE.equals(manual.campaign().getExclusiveCampaign())
                && !Boolean.TRUE.equals(
                automatic.campaign().getExclusiveCampaign()));
    }

    private Selection better(
            Selection current, Selection candidate) {
        int discountComparison = candidate.discount().compareTo(current.discount());
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
        int campaignPriorityComparison = Integer.compare(
                candidate.campaignPriorityScore(), current.campaignPriorityScore());
        if (campaignPriorityComparison != 0) {
            return campaignPriorityComparison < 0 ? candidate : current;
        }
        int promotionPriorityComparison = Integer.compare(
                candidate.promotionPriorityScore(), current.promotionPriorityScore());
        if (promotionPriorityComparison != 0) {
            return promotionPriorityComparison < 0 ? candidate : current;
        }
        return selectionKey(candidate).compareTo(selectionKey(current)) < 0
                ? candidate : current;
    }

    private String selectionKey(Selection selection) {
        return selection.candidates().stream()
                .map(candidate -> candidate.promotion().getPublicId())
                .sorted()
                .collect(java.util.stream.Collectors.joining("|"));
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
            BigDecimal allocated = discountFor(candidate, remaining).min(remaining);
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

    private Selection selectionOf(
            List<Candidate> candidates, BigDecimal originalAmount) {
        if (candidates == null || candidates.isEmpty()) {
            return Selection.empty();
        }
        BigDecimal remaining = originalAmount;
        BigDecimal discount = BigDecimal.ZERO;
        for (Candidate candidate : candidates.stream().sorted(candidateOrder()).toList()) {
            BigDecimal applied = discountFor(candidate, remaining).min(remaining);
            discount = discount.add(applied);
            remaining = remaining.subtract(applied);
            if (remaining.signum() <= 0) {
                break;
            }
        }
        if (remaining.signum() <= 0 && candidates.stream().anyMatch(candidate ->
                configuredArray(candidate.conditions().get("paymentMethods")))) {
            return Selection.empty();
        }
        discount = money(discount);
        int walletItems = (int) candidates.stream()
                .filter(candidate -> candidate.wallet() != null
                        || candidate.promotion().getPromotionType() == PromotionType.COUPON)
                .count();
        int campaignPriority = candidates.stream()
                .mapToInt(candidate -> candidate.campaign().getPriority()).sum();
        int promotionPriority = candidates.stream()
                .mapToInt(candidate -> candidate.promotion().getPriority()).sum();
        return new Selection(
                List.copyOf(candidates), discount, walletItems,
                campaignPriority, promotionPriority);
    }

    private BigDecimal discountFor(Candidate candidate, BigDecimal remaining) {
        JsonNode action = candidate.actions();
        if (action != null && action.isArray() && !action.isEmpty()) {
            action = action.get(0);
        }
        String type = action == null ? ""
                : action.path("discountType").asText(
                action.path("type").asText(action.path("actionType").asText("")));
        if ("PERCENTAGE".equalsIgnoreCase(type)
                || "PERCENT".equalsIgnoreCase(type)) {
            return discountCalculator.calculate(
                    candidate.actions(), remaining, candidate.context());
        }
        return candidate.discount().min(remaining);
    }

    private boolean configuredArray(JsonNode value) {
        return value != null && value.isArray() && !value.isEmpty();
    }

    private void requireSingleManualSelection(
            List<String> promotionIds,
            List<String> walletIds,
            String couponCode) {
        long selectionCount = promotionIds.stream().distinct().count()
                + walletIds.stream().distinct().count()
                + (couponCode == null || couponCode.isBlank() ? 0 : 1);
        if (selectionCount > 1) {
            throw invalid("Only one voucher or coupon can be selected per booking");
        }
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
        if (userUsage >= customerPromotionLimit(promotion, campaign)) {
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
        BigDecimal availableBudget = campaign.getBudgetRemaining()
                .subtract(campaign.getBudgetReserved());
        if (availableBudget.compareTo(discount) < 0) {
            throw conflict("Campaign budget is exhausted");
        }
    }

    private void requireSelectionCapacity(
            List<Candidate> selected, String userPublicId,
            BigDecimal originalAmount) {
        Map<String, List<Candidate>> byCampaign = new LinkedHashMap<>();
        Map<String, Integer> byPromotion = new LinkedHashMap<>();
        Map<String, Integer> byWallet = new LinkedHashMap<>();
        for (Candidate candidate : selected) {
            byCampaign.computeIfAbsent(
                    candidate.campaign().getPublicId(), ignored -> new ArrayList<>())
                    .add(candidate);
            byPromotion.merge(candidate.promotion().getPublicId(), 1, Integer::sum);
            if (candidate.wallet() != null) {
                byWallet.merge(candidate.wallet().getPublicId(), 1, Integer::sum);
            }
        }
        for (Candidate candidate : selected) {
            Promotion promotion = candidate.promotion();
            long usage = redemptionRepository
                    .countByPromotionPublicIdAndStatusInAndDeletedAtIsNull(
                            promotion.getPublicId(), CAPACITY_STATUSES);
            if (promotion.getMaxRedemptions() != null
                    && usage + byPromotion.get(promotion.getPublicId())
                    > promotion.getMaxRedemptions()) {
                throw conflict("Selected promotions exceed promotion capacity");
            }
            long userUsage = redemptionRepository
                    .countByPromotionPublicIdAndUserPublicIdAndStatusInAndDeletedAtIsNull(
                            promotion.getPublicId(), userPublicId, CAPACITY_STATUSES);
            if (userUsage + byPromotion.get(promotion.getPublicId())
                    > customerPromotionLimit(promotion, candidate.campaign())) {
                throw conflict("Selected promotions exceed customer promotion capacity");
            }
            if (candidate.wallet() != null) {
                UserPromotion wallet = candidate.wallet();
                long reservations = redemptionRepository
                        .countByUserPromotionPublicIdAndStatusInAndDeletedAtIsNull(
                                wallet.getPublicId(), RESERVED_STATUS);
                if (wallet.getUsageCount() + reservations
                        + byWallet.get(wallet.getPublicId()) > wallet.getMaxUsage()) {
                    throw conflict("Selected promotions exceed wallet capacity");
                }
            }
        }
        Map<String, BigDecimal> allocatedByCampaign = new LinkedHashMap<>();
        BigDecimal remaining = originalAmount;
        for (Candidate candidate : selected.stream().sorted(candidateOrder()).toList()) {
            BigDecimal allocated = discountFor(candidate, remaining).min(remaining);
            allocatedByCampaign.merge(
                    candidate.campaign().getPublicId(), allocated, BigDecimal::add);
            remaining = money(remaining.subtract(allocated));
        }
        for (List<Candidate> campaignItems : byCampaign.values()) {
            PromotionCampaign campaign = campaignItems.get(0).campaign();
            long usage = redemptionRepository.countCampaignRedemptions(
                    campaign.getPublicId(), CAPACITY_STATUSES);
            if (campaign.getMaxRedemptions() != null
                    && usage + 1 > campaign.getMaxRedemptions()) {
                throw conflict("Selected promotions exceed campaign capacity");
            }
            BigDecimal totalDiscount = allocatedByCampaign.get(campaign.getPublicId());
            BigDecimal availableBudget = campaign.getBudgetRemaining()
                    .subtract(campaign.getBudgetReserved());
            if (availableBudget.compareTo(totalDiscount) < 0) {
                throw conflict("Selected promotions exceed campaign budget");
            }
        }
    }

    private int customerPromotionLimit(
            Promotion promotion, PromotionCampaign campaign) {
        int promotionLimit = promotion.getMaxRedemptionsPerUser() == null
                ? Integer.MAX_VALUE : promotion.getMaxRedemptionsPerUser();
        int campaignCeiling = campaign.getMaxRedemptionsPerUser() == null
                ? Integer.MAX_VALUE : campaign.getMaxRedemptionsPerUser();
        return Math.min(promotionLimit, campaignCeiling);
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
                .thenComparingInt(candidate -> candidate.campaign().getPriority())
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
            JsonNode actions,
            JsonNode context,
            BigDecimal discount) {
    }

    private record Selection(
            List<Candidate> candidates,
            BigDecimal discount,
            int walletItems,
            int campaignPriorityScore,
            int promotionPriorityScore) {

        private static Selection empty() {
            return new Selection(
                    List.of(), BigDecimal.ZERO, 0,
                    Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
    }

}
