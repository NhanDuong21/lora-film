package com.project.promotionservice.partner.service;

import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.partner.dto.request.PartnerCreateRequest;
import com.project.promotionservice.partner.dto.request.PartnerUpdateRequest;
import com.project.promotionservice.partner.dto.response.PartnerResponse;
import com.project.promotionservice.partner.enums.PartnerStatus;
import org.springframework.data.domain.Pageable;

public interface PartnerService {
    PartnerResponse create(PartnerCreateRequest request, String actor);
    PartnerResponse update(String publicId, PartnerUpdateRequest request, String actor);
    void disable(String publicId, String actor);
    PagedResponse<PartnerResponse> search(String keyword, PartnerStatus status, Pageable pageable);
    PartnerResponse detail(String publicId);
    void requireActive(String publicId);
}
