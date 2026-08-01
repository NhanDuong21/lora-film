package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.request.CouponRedeemRequest;
import com.project.promotionservice.promotion.dto.response.PromotionResponse;
import com.project.promotionservice.promotion.dto.response.WalletPromotionResponse;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import com.project.promotionservice.promotion.service.PromotionCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;
import static com.project.promotionservice.common.web.ControllerPageSupport.pageable;

@RestController
@Validated
@RequestMapping("/api")
public class CustomerPromotionController {

    private static final Set<String> PROMOTION_SORT_FIELDS = Set.of(
            "priority", "validTo", "createdAt", "name");
    private static final Set<String> WALLET_SORT_FIELDS = Set.of(
            "claimedAt", "validTo", "status", "createdAt");

    private final PromotionCatalogService service;

    public CustomerPromotionController(PromotionCatalogService service) {
        this.service = service;
    }

    @GetMapping("/promotions/public")
    public ResponseEntity<ApiResponse<PagedResponse<PromotionResponse>>> publicPromotions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "priority,asc") @Size(max = 60) String sort) {
        Pageable pageable = pageable(
                page, size, sort, PROMOTION_SORT_FIELDS, "priority");
        return ResponseEntity.ok(ApiResponse.success(service.publicPromotions(pageable)));
    }

    @PostMapping("/promotions/{id}/claim")
    public ResponseEntity<ApiResponse<WalletPromotionResponse>> claim(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        String user = currentUser(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Voucher added to wallet",
                        service.claim(publicId, user, user)));
    }

    @PostMapping("/promotions/coupons/redeem")
    public ResponseEntity<ApiResponse<WalletPromotionResponse>> redeemCoupon(
            @Valid @RequestBody CouponRedeemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String user = currentUser(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon added to wallet",
                        service.redeemCoupon(request.code(), user, user)));
    }

    @GetMapping("/customers/me/promotions")
    public ResponseEntity<ApiResponse<PagedResponse<WalletPromotionResponse>>> wallet(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UserPromotionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "validTo,asc") @Size(max = 60) String sort) {
        String user = currentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(service.wallet(
                user, status,
                pageable(page, size, sort, WALLET_SORT_FIELDS, "validTo"))));
    }

    private String currentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Authentication is required",
                    HttpStatus.UNAUTHORIZED);
        }
        String actor = principal.getId() == null
                ? principal.getUsername() : principal.getId().toString();
        if (actor == null || actor.isBlank()) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Authenticated customer has no identifier",
                    HttpStatus.UNAUTHORIZED);
        }
        return actor.length() <= 36 ? actor : actor.substring(0, 36);
    }
}
