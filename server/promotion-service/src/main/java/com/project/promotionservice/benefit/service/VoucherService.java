package com.project.promotionservice.benefit.service;

import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherBatchIssueRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherExtendRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherIssueRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherUpdateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.VoucherResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.common.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VoucherService {

    VoucherResponse issue(VoucherIssueRequest request, String actor);

    List<VoucherResponse> batchIssue(VoucherBatchIssueRequest request, String actor);

    VoucherResponse update(String publicId, VoucherUpdateRequest request, String actor);

    VoucherResponse revoke(String publicId, String reason, String actor);

    VoucherResponse extend(String publicId, VoucherExtendRequest request, String actor);

    PagedResponse<VoucherResponse> search(
            String keyword, String ownerPublicId, String campaignPublicId,
            VoucherStatus status, VoucherSource source, Pageable pageable);

    VoucherResponse get(String publicId);

    PagedResponse<VoucherResponse> wallet(String ownerPublicId, VoucherStatus status, Pageable pageable);
}
