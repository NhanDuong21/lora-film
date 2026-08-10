package com.lorafilm.movie.pricing.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.pricing.dto.request.ActivatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.CopyPricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.CreatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.DeactivatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.PriceResolutionPreviewRequest;
import com.lorafilm.movie.pricing.dto.request.UpdatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.response.PricePolicyResponse;
import com.lorafilm.movie.pricing.dto.response.PricePolicyUsageResponse;
import com.lorafilm.movie.pricing.dto.response.PriceResolutionPreviewResponse;

import java.time.LocalDate;

public interface PricePolicyService {
    PageResponse<PricePolicyResponse> search(String cinemaId, String status,
                                             LocalDate effectiveDate, int page, int size);
    PricePolicyResponse create(CreatePricePolicyRequest request);
    PricePolicyResponse get(String publicId);
    PricePolicyResponse update(String publicId, UpdatePricePolicyRequest request);
    PricePolicyResponse activate(String publicId, ActivatePricePolicyRequest request);
    PricePolicyResponse deactivate(String publicId, DeactivatePricePolicyRequest request);
    PricePolicyResponse copy(String publicId, CopyPricePolicyRequest request);
    PricePolicyUsageResponse usage(String publicId, int page, int size);
    PriceResolutionPreviewResponse preview(PriceResolutionPreviewRequest request);
}
