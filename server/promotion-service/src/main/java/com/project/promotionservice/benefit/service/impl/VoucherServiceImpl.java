package com.project.promotionservice.benefit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherBatchIssueRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherExtendRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherIssueRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherUpdateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.VoucherResponse;
import com.project.promotionservice.benefit.entity.Voucher;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.benefit.exception.BenefitErrorCode;
import com.project.promotionservice.benefit.mapper.BenefitMapper;
import com.project.promotionservice.benefit.repository.VoucherRepository;
import com.project.promotionservice.benefit.service.BenefitEventService;
import com.project.promotionservice.benefit.service.BenefitPolicyValidator;
import com.project.promotionservice.benefit.service.VoucherService;
import com.project.promotionservice.benefit.specification.BenefitSpecifications;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignType;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.service.CampaignConfigurationPolicy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final BenefitMapper mapper;
    private final BenefitEventService eventService;
    private final BenefitPolicyValidator policyValidator;
    private final CampaignConfigurationPolicy configurationPolicy;

    public VoucherServiceImpl(VoucherRepository voucherRepository,
                              PromotionCampaignRepository campaignRepository,
                              BenefitMapper mapper,
                              BenefitEventService eventService,
                              BenefitPolicyValidator policyValidator,
                              CampaignConfigurationPolicy configurationPolicy) {
        this.voucherRepository = voucherRepository;
        this.campaignRepository = campaignRepository;
        this.mapper = mapper;
        this.eventService = eventService;
        this.policyValidator = policyValidator;
        this.configurationPolicy = configurationPolicy;
    }

    @Override
    @Transactional
    @Auditable(action = "ISSUE", entityType = "VOUCHER")
    @CacheEvict(cacheNames = "vouchers", allEntries = true)
    public VoucherResponse issue(VoucherIssueRequest request, String actor) {
        Voucher saved = issueOne(request, actor);
        VoucherResponse response = currentResponse(saved);
        eventService.record("VOUCHER", saved.getPublicId(), "VOUCHER_ISSUED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "BATCH_ISSUE", entityType = "VOUCHER")
    @CacheEvict(cacheNames = "vouchers", allEntries = true)
    public List<VoucherResponse> batchIssue(VoucherBatchIssueRequest request, String actor) {
        List<VoucherResponse> responses = new ArrayList<>(request.getVouchers().size());
        for (VoucherIssueRequest item : request.getVouchers()) {
            Voucher saved = issueOne(item, actor);
            VoucherResponse response = currentResponse(saved);
            responses.add(response);
            eventService.record("VOUCHER", saved.getPublicId(), "VOUCHER_ISSUED", response, actor);
        }
        return responses;
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE", entityType = "VOUCHER")
    @CacheEvict(cacheNames = "vouchers", allEntries = true)
    public VoucherResponse update(String publicId, VoucherUpdateRequest request, String actor) {
        Voucher voucher = find(publicId);
        PromotionCampaign campaign = requireEditableLinkedCampaign(voucher.getCampaignPublicId());
        if (voucher.getStatus() == VoucherStatus.USED || voucher.getStatus() == VoucherStatus.REVOKED) {
            throw badRequest(BenefitErrorCode.VOUCHER_INVALID, "Used or revoked voucher cannot be updated");
        }
        if (voucher.getUsageCount() > 0
                && (request.getConditionsJson() != null || request.getActionsJson() != null)) {
            throw badRequest(
                    BenefitErrorCode.BENEFIT_CONFIGURATION_INVALID,
                    "Voucher conditions and actions are immutable after the first redemption");
        }
        if (request.getName() != null) voucher.setName(request.getName());
        if (request.getDescription() != null) voucher.setDescription(request.getDescription());
        if (request.getVoucherType() != null) voucher.setVoucherType(request.getVoucherType());
        if (request.getStatus() != null) {
            if (request.getStatus() != VoucherStatus.ACTIVE && request.getStatus() != VoucherStatus.ISSUED) {
                throw badRequest(BenefitErrorCode.VOUCHER_INVALID,
                        "Use the revoke endpoint or redemption flow for terminal statuses");
            }
            voucher.setStatus(request.getStatus());
        }
        if (request.getIssueReason() != null) voucher.setIssueReason(request.getIssueReason());
        if (request.getTransferable() != null) voucher.setTransferable(request.getTransferable());
        if (request.getStackable() != null) voucher.setStackable(request.getStackable());
        if (request.getReusable() != null) voucher.setReusable(request.getReusable());
        if (request.getMaxUsage() != null) {
            if (request.getMaxUsage() < voucher.getUsageCount()) {
                throw badRequest(ErrorCode.INVALID_REQUEST_PARAMETER,
                        "maxUsage cannot be lower than usageCount");
            }
            voucher.setMaxUsage(request.getMaxUsage());
        }
        if (request.getFaceValue() != null) voucher.setFaceValue(request.getFaceValue());
        if (request.getMinimumOrderAmount() != null) {
            voucher.setMinimumOrderAmount(request.getMinimumOrderAmount());
        }
        if (request.getConditionsJson() != null) voucher.setConditionsJson(mapper.toJson(request.getConditionsJson()));
        if (request.getActionsJson() != null) voucher.setActionsJson(mapper.toJson(request.getActionsJson()));
        if (request.getMetadataJson() != null) voucher.setMetadataJson(mapper.toNullableJson(request.getMetadataJson()));
        policyValidator.validateVoucher(
                voucher.getVoucherType(), voucher.getReusable(), voucher.getMaxUsage(),
                voucher.getFaceValue(), mapper.toNode(voucher.getConditionsJson()),
                mapper.toNode(voucher.getActionsJson()));
        voucher.setUpdatedBy(actor);
        VoucherResponse response = currentResponse(voucherRepository.save(voucher));
        if (campaign != null) {
            markCampaignChanged(campaign, actor);
        }
        eventService.record("VOUCHER", publicId, "VOUCHER_UPDATED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "REVOKE", entityType = "VOUCHER")
    @CacheEvict(cacheNames = "vouchers", allEntries = true)
    public VoucherResponse revoke(String publicId, String reason, String actor) {
        Voucher voucher = find(publicId);
        PromotionCampaign campaign = requireLinkedCampaign(voucher.getCampaignPublicId());
        if (campaign != null && campaign.getStatus() == CampaignStatus.DRAFT) {
            configurationPolicy.requireEditable(campaign);
        }
        if (voucher.getStatus() == VoucherStatus.USED) {
            throw badRequest(BenefitErrorCode.VOUCHER_ALREADY_USED, "Used voucher cannot be revoked");
        }
        if (voucher.getStatus() != VoucherStatus.REVOKED) {
            voucher.setStatus(VoucherStatus.REVOKED);
            voucher.setIssueReason(appendReason(voucher.getIssueReason(), "Revoked: " + reason));
            voucher.setUpdatedBy(actor);
            voucherRepository.save(voucher);
            if (campaign != null && campaign.getStatus() == CampaignStatus.DRAFT) {
                markCampaignChanged(campaign, actor);
            }
        }
        VoucherResponse response = currentResponse(voucher);
        eventService.record("VOUCHER", publicId, "VOUCHER_REVOKED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "EXTEND", entityType = "VOUCHER")
    @CacheEvict(cacheNames = "vouchers", allEntries = true)
    public VoucherResponse extend(String publicId, VoucherExtendRequest request, String actor) {
        Voucher voucher = find(publicId);
        if (voucher.getSource() == VoucherSource.BIRTHDAY) {
            throw badRequest(BenefitErrorCode.VOUCHER_INVALID, "Birthday voucher cannot be extended");
        }
        if (voucher.getStatus() == VoucherStatus.USED || voucher.getStatus() == VoucherStatus.REVOKED) {
            throw badRequest(BenefitErrorCode.VOUCHER_INVALID, "Used or revoked voucher cannot be extended");
        }
        if (!request.getValidTo().isAfter(voucher.getValidTo())) {
            throw badRequest(ErrorCode.INVALID_REQUEST_PARAMETER, "New validTo must be after current validTo");
        }
        PromotionCampaign campaign = requireLinkedCampaign(voucher.getCampaignPublicId());
        if (campaign != null && campaign.getStatus() == CampaignStatus.DRAFT) {
            configurationPolicy.requireEditable(campaign);
        }
        if (campaign != null && request.getValidTo().isAfter(campaign.getEndAt())) {
            throw badRequest(
                    BenefitErrorCode.VOUCHER_INVALID,
                    "A campaign voucher cannot be extended beyond the campaign end time");
        }
        voucher.setValidTo(request.getValidTo());
        if (voucher.getValidFrom().isAfter(Instant.now())) {
            voucher.setStatus(VoucherStatus.ISSUED);
        } else {
            voucher.setStatus(VoucherStatus.ACTIVE);
        }
        JsonNode existing = mapper.toNode(voucher.getMetadataJson());
        ObjectNode metadata = existing != null && existing.isObject()
                ? (ObjectNode) existing.deepCopy()
                : com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        metadata.put("lastExtensionReason", request.getReason());
        metadata.put("lastExtendedAt", Instant.now().toString());
        voucher.setMetadataJson(mapper.toJson(metadata));
        voucher.setUpdatedBy(actor);
        VoucherResponse response = currentResponse(voucherRepository.save(voucher));
        if (campaign != null && campaign.getStatus() == CampaignStatus.DRAFT) {
            markCampaignChanged(campaign, actor);
        }
        eventService.record("VOUCHER", publicId, "VOUCHER_EXTENDED", response, actor);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<VoucherResponse> search(
            String keyword, String ownerPublicId, String campaignPublicId,
            VoucherStatus status, VoucherSource source, Pageable pageable) {
        Page<Voucher> result = voucherRepository.findAll(
                BenefitSpecifications.vouchers(keyword, ownerPublicId, campaignPublicId, status, source), pageable);
        return page(result.map(this::currentResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "vouchers", key = "'voucher:' + #publicId")
    public VoucherResponse get(String publicId) {
        return currentResponse(find(publicId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "vouchers",
            key = "'wallet:' + #ownerPublicId + ':' + #status + ':' + #pageable.pageNumber + ':'"
                    + " + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public PagedResponse<VoucherResponse> wallet(
            String ownerPublicId, VoucherStatus status, Pageable pageable) {
        Page<Voucher> result = voucherRepository.findAll(
                BenefitSpecifications.vouchers(null, ownerPublicId, null, status, null), pageable);
        return page(result.map(this::currentResponse));
    }

    private Voucher issueOne(VoucherIssueRequest request, String actor) {
        policyValidator.validateVoucher(
                request.getVoucherType(), request.getReusable(), request.getMaxUsage(),
                request.getFaceValue(), request.getConditionsJson(), request.getActionsJson());
        PromotionCampaign campaign = null;
        if (request.getCampaignPublicId() != null && !request.getCampaignPublicId().isBlank()) {
            campaign = campaignRepository.findByPublicId(request.getCampaignPublicId())
                    .filter(value -> value.getDeletedAt() == null)
                    .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Promotion campaign not found", HttpStatus.NOT_FOUND));
            if (campaign.getCampaignType() != CampaignType.VOUCHER) {
                throw badRequest(
                        BenefitErrorCode.BENEFIT_CONFIGURATION_INVALID,
                        "Vouchers can only be attached to a VOUCHER campaign");
            }
            configurationPolicy.requireEditable(campaign);
        }
        if (request.getSource() == VoucherSource.BIRTHDAY
                && request.getValidTo().isAfter(Instant.now().plus(120, ChronoUnit.DAYS))) {
            throw badRequest(BenefitErrorCode.VOUCHER_INVALID,
                    "Birthday voucher validity cannot exceed 120 days from issuance");
        }
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            code = generateCode();
        } else {
            code = mapper.normalizeCode(code);
        }
        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw badRequest(BenefitErrorCode.VOUCHER_DUPLICATE, "Voucher code already exists");
        }
        Voucher voucher = mapper.toVoucher(request, code, actor);
        Voucher saved = voucherRepository.save(voucher);
        if (campaign != null) {
            markCampaignChanged(campaign, actor);
        }
        return saved;
    }

    private String generateCode() {
        String code;
        do {
            code = "VCH-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 16).toUpperCase(Locale.ROOT);
        } while (voucherRepository.existsByCodeIgnoreCase(code));
        return code;
    }

    private Voucher find(String publicId) {
        return voucherRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(
                        BenefitErrorCode.VOUCHER_NOT_FOUND, "Voucher not found", HttpStatus.NOT_FOUND));
    }

    private PromotionCampaign requireEditableLinkedCampaign(String campaignPublicId) {
        PromotionCampaign campaign = requireLinkedCampaign(campaignPublicId);
        if (campaign != null) {
            configurationPolicy.requireEditable(campaign);
        }
        return campaign;
    }

    private PromotionCampaign requireLinkedCampaign(String campaignPublicId) {
        if (campaignPublicId == null || campaignPublicId.isBlank()) {
            return null;
        }
        return campaignRepository.findByPublicId(campaignPublicId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Promotion campaign not found", HttpStatus.NOT_FOUND));
    }

    private void markCampaignChanged(PromotionCampaign campaign, String actor) {
        configurationPolicy.markConfigurationChanged(campaign, actor);
        campaignRepository.save(campaign);
    }

    private VoucherResponse currentResponse(Voucher voucher) {
        VoucherResponse response = mapper.toVoucherResponse(voucher);
        Instant now = Instant.now();
        if (voucher.getValidTo().isBefore(now)
                && voucher.getStatus() != VoucherStatus.USED
                && voucher.getStatus() != VoucherStatus.REVOKED
                && voucher.getStatus() != VoucherStatus.CANCELLED) {
            response.setStatus(VoucherStatus.EXPIRED);
        } else if (!voucher.getValidFrom().isAfter(now) && voucher.getStatus() == VoucherStatus.ISSUED) {
            response.setStatus(VoucherStatus.ACTIVE);
        }
        return response;
    }

    private String appendReason(String existing, String addition) {
        return existing == null || existing.isBlank() ? addition : existing + " | " + addition;
    }

    private <T> PagedResponse<T> page(Page<T> result) {
        return new PagedResponse<>(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }
}
