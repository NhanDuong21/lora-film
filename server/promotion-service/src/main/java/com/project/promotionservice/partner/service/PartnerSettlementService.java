package com.project.promotionservice.partner.service;

import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.partner.dto.request.SettlementCreateRequest;
import com.project.promotionservice.partner.dto.request.SettlementUpdateRequest;
import com.project.promotionservice.partner.dto.response.SettlementResponse;
import com.project.promotionservice.partner.enums.SettlementStatus;
import org.springframework.data.domain.Pageable;

public interface PartnerSettlementService {
    SettlementResponse create(SettlementCreateRequest request, String actor);
    SettlementResponse update(String publicId, SettlementUpdateRequest request, String actor);
    void disable(String publicId, String actor);
    PagedResponse<SettlementResponse> search(String partnerPublicId, String campaignPublicId,
                                             SettlementStatus status, Pageable pageable);
    SettlementResponse detail(String publicId);
}
