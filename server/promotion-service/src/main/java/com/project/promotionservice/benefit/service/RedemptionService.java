package com.project.promotionservice.benefit.service;

import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitValidationRequest;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.ReservedRedemptionRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.RedemptionResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.ValidationResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.common.response.PagedResponse;

import java.time.Instant;

public interface RedemptionService {

    ValidationResponse validateCoupon(BenefitValidationRequest request);

    ValidationResponse validateVoucher(BenefitValidationRequest request);

    RedemptionResponse confirmReservedCoupon(ReservedRedemptionRequest request, String actor);

    RedemptionResponse confirmReservedVoucher(ReservedRedemptionRequest request, String actor);

    PagedResponse<RedemptionResponse> history(
            RedemptionType type, String userPublicId, String bookingPublicId,
            RedemptionStatus status, Instant from, Instant to, int page, int size);
}
