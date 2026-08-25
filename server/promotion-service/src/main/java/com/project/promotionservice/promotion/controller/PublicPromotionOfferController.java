package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.presentation.storage.PromotionAssetStorage;
import com.project.promotionservice.promotion.dto.response.PublicPromotionOfferResponse;
import com.project.promotionservice.promotion.enums.PromotionPlacement;
import com.project.promotionservice.promotion.service.CampaignPresentationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@Validated
@RequestMapping("/api/promotions")
public class PublicPromotionOfferController {

    private final CampaignPresentationService service;

    public PublicPromotionOfferController(CampaignPresentationService service) {
        this.service = service;
    }

    @GetMapping("/offers")
    public ResponseEntity<ApiResponse<PagedResponse<PublicPromotionOfferResponse>>> offers(
            @RequestParam(defaultValue = "HOME") PromotionPlacement placement,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                service.publicOffers(placement, PageRequest.of(page, size))));
    }

    @GetMapping("/assets/{storageKey}")
    public ResponseEntity<Resource> asset(
            @PathVariable
            @Pattern(regexp = "[A-Za-z0-9_-]+\\.(jpg|png|webp)")
            String storageKey) {
        PromotionAssetStorage.LoadedAsset asset = service.loadAsset(storageKey);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(asset.contentType());
        } catch (IllegalArgumentException ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
                .contentType(mediaType)
                .body(asset.resource());
    }
}
