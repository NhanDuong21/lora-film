package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.promotion.dto.request.RuleCreateRequest;
import com.project.promotionservice.promotion.dto.request.RuleUpdateRequest;
import com.project.promotionservice.promotion.dto.request.RuleCloneRequest;
import com.project.promotionservice.promotion.dto.response.RuleResponse;
import org.springframework.data.domain.Pageable;

public interface RuleService {

    RuleResponse createRule(RuleCreateRequest request, String creator);

    RuleResponse updateRule(String publicId, RuleUpdateRequest request, String updater);

    void deleteRule(String publicId, String deleter);

    RuleResponse getRule(String publicId);

    PagedResponse<RuleResponse> searchRules(String campaignPublicId, String code, Boolean enabled, Pageable pageable);

    RuleResponse cloneRule(String publicId, RuleCloneRequest request, String creator);

    boolean validateRuleJson(String conditionsJson, String actionsJson);

    double previewDiscount(String conditionsJson, String actionsJson, String contextJson);
}
