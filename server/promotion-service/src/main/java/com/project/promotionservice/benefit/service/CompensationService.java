package com.project.promotionservice.benefit.service;

import com.project.promotionservice.benefit.dto.request.CompensationRequests.CompensationIssueRequest;
import com.project.promotionservice.benefit.dto.request.CompensationRequests.CompensationUpdateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CompensationResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationType;
import com.project.promotionservice.common.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface CompensationService {

    CompensationResponse issue(CompensationIssueRequest request, String actor);

    CompensationResponse update(String publicId, CompensationUpdateRequest request, String actor);

    PagedResponse<CompensationResponse> search(
            String userPublicId, CompensationType type, CompensationStatus status,
            Instant from, Instant to, Pageable pageable);

    CompensationResponse get(String publicId);
}
