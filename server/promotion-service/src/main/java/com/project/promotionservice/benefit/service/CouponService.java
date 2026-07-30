package com.project.promotionservice.benefit.service;

import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponCreateRequest;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponGenerateRequest;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponUpdateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponImportResult;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.common.response.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

public interface CouponService {

    CouponResponse create(CouponCreateRequest request, String actor);

    List<CouponResponse> generate(CouponGenerateRequest request, String actor);

    CouponImportResult importCsv(MultipartFile file, String actor);

    byte[] exportCsv(String keyword, String campaignPublicId, CouponStatus status);

    CouponResponse update(String publicId, CouponUpdateRequest request, String actor);

    void disable(String publicId, String actor);

    PagedResponse<CouponResponse> search(
            String keyword, String campaignPublicId, CouponStatus status, Instant validAt, Pageable pageable);

    CouponResponse get(String publicId);
}
