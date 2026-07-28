package com.project.promotionservice.benefit.service.impl;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.benefit.dto.request.CompensationRequests.CompensationIssueRequest;
import com.project.promotionservice.benefit.dto.request.CompensationRequests.CompensationUpdateRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherExtendRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherIssueRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CompensationResponse;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.VoucherResponse;
import com.project.promotionservice.benefit.entity.CompensationApprovalHistory;
import com.project.promotionservice.benefit.entity.CompensationVoucher;
import com.project.promotionservice.benefit.entity.Voucher;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationType;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherType;
import com.project.promotionservice.benefit.exception.BenefitErrorCode;
import com.project.promotionservice.benefit.mapper.BenefitMapper;
import com.project.promotionservice.benefit.repository.CompensationApprovalHistoryRepository;
import com.project.promotionservice.benefit.repository.CompensationVoucherRepository;
import com.project.promotionservice.benefit.repository.VoucherRepository;
import com.project.promotionservice.benefit.service.BenefitEventService;
import com.project.promotionservice.benefit.service.CompensationService;
import com.project.promotionservice.benefit.service.VoucherService;
import com.project.promotionservice.benefit.specification.BenefitSpecifications;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.response.PagedResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CompensationServiceImpl implements CompensationService {

    private static final String TARGET_TYPE = "COMPENSATION";

    private final CompensationVoucherRepository compensationRepository;
    private final CompensationApprovalHistoryRepository approvalHistoryRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService;
    private final BenefitMapper mapper;
    private final BenefitEventService eventService;

    public CompensationServiceImpl(
            CompensationVoucherRepository compensationRepository,
            CompensationApprovalHistoryRepository approvalHistoryRepository,
            VoucherRepository voucherRepository,
            VoucherService voucherService,
            BenefitMapper mapper,
            BenefitEventService eventService) {
        this.compensationRepository = compensationRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.voucherRepository = voucherRepository;
        this.voucherService = voucherService;
        this.mapper = mapper;
        this.eventService = eventService;
    }

    @Override
    @Transactional
    @Auditable(action = "ISSUE", entityType = "COMPENSATION_VOUCHER")
    @CacheEvict(cacheNames = "vouchers", allEntries = true)
    public CompensationResponse issue(CompensationIssueRequest request, String actor) {
        VoucherIssueRequest voucherRequest = compensationVoucherRequest(request);
        VoucherResponse issuedVoucher = voucherService.issue(voucherRequest, actor);

        CompensationVoucher compensation = new CompensationVoucher();
        compensation.setVoucherPublicId(issuedVoucher.getPublicId());
        compensation.setReservationPublicId(request.getReservationPublicId());
        compensation.setBookingPublicId(request.getBookingPublicId());
        compensation.setOrderPublicId(request.getOrderPublicId());
        compensation.setUserPublicId(request.getUserPublicId());
        compensation.setCompensationType(request.getCompensationType());
        compensation.setReason(request.getReason());
        compensation.setAmount(request.getAmount());
        compensation.setStatus(CompensationStatus.ISSUED);
        compensation.setIssuedAt(Instant.now());
        compensation.setExpiredAt(request.getExpiredAt());
        compensation.setMetadataJson(mapper.toNullableJson(request.getMetadataJson()));
        compensation.setCreatedBy(actor);
        compensation.setUpdatedBy(actor);
        CompensationVoucher saved = compensationRepository.save(compensation);

        saveApproval(saved, "APPROVE", "PENDING", "ISSUED",
                actor, "Approved and issued by authorized administrator");

        CompensationResponse response = response(saved);
        eventService.record(
                "COMPENSATION", saved.getPublicId(),
                "COMPENSATION_VOUCHER_ISSUED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE", entityType = "COMPENSATION_VOUCHER")
    @CacheEvict(cacheNames = "vouchers", allEntries = true)
    public CompensationResponse update(String publicId, CompensationUpdateRequest request, String actor) {
        CompensationVoucher compensation = find(publicId);
        if (compensation.getStatus() == CompensationStatus.CANCELLED) {
            throw badRequest(BenefitErrorCode.COMPENSATION_IMMUTABLE,
                    "Cancelled compensation cannot be changed");
        }
        if (request.getReason() != null) {
            compensation.setReason(request.getReason());
        }
        if (request.getExpiredAt() != null) {
            if (!request.getExpiredAt().isAfter(compensation.getExpiredAt())) {
                throw badRequest(ErrorCode.INVALID_REQUEST_PARAMETER,
                        "Compensation expiry can only be extended");
            }
            VoucherExtendRequest extendRequest = new VoucherExtendRequest();
            extendRequest.setValidTo(request.getExpiredAt());
            extendRequest.setReason("Compensation validity extended");
            voucherService.extend(compensation.getVoucherPublicId(), extendRequest, actor);
            compensation.setExpiredAt(request.getExpiredAt());
        }
        if (request.getMetadataJson() != null) {
            compensation.setMetadataJson(mapper.toNullableJson(request.getMetadataJson()));
        }
        if (request.getStatus() != null && request.getStatus() != compensation.getStatus()) {
            if (request.getStatus() != CompensationStatus.CANCELLED) {
                throw badRequest(BenefitErrorCode.COMPENSATION_IMMUTABLE,
                        "An issued compensation can only transition to CANCELLED");
            }
            voucherService.revoke(
                    compensation.getVoucherPublicId(),
                    "Compensation cancelled: " + compensation.getReason(),
                    actor);
            compensation.setStatus(CompensationStatus.CANCELLED);
            saveApproval(compensation, "REJECT", "ISSUED", "CANCELLED",
                    actor, "Compensation cancelled");
        }
        compensation.setUpdatedBy(actor);
        CompensationVoucher saved = compensationRepository.save(compensation);
        CompensationResponse response = response(saved);
        eventService.record(
                "COMPENSATION", saved.getPublicId(),
                "COMPENSATION_VOUCHER_UPDATED", response, actor);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CompensationResponse> search(
            String userPublicId, CompensationType type, CompensationStatus status,
            Instant from, Instant to, Pageable pageable) {
        Page<CompensationVoucher> page = compensationRepository.findAll(
                BenefitSpecifications.compensations(userPublicId, type, status, from, to), pageable);
        Page<CompensationResponse> mapped = page.map(this::response);
        return new PagedResponse<>(
                mapped.getContent(), mapped.getNumber(), mapped.getSize(),
                mapped.getTotalElements(), mapped.getTotalPages(), mapped.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public CompensationResponse get(String publicId) {
        return response(find(publicId));
    }

    private VoucherIssueRequest compensationVoucherRequest(CompensationIssueRequest request) {
        VoucherIssueRequest voucher = new VoucherIssueRequest();
        voucher.setOwnerPublicId(request.getUserPublicId());
        voucher.setCode(request.getVoucherCode());
        voucher.setName(request.getVoucherName() == null || request.getVoucherName().isBlank()
                ? "Compensation - " + request.getCompensationType()
                : request.getVoucherName());
        voucher.setDescription(request.getReason());
        voucher.setVoucherType(VoucherType.COMPENSATION);
        voucher.setSource(VoucherSource.COMPENSATION);
        voucher.setIssueReason(request.getReason());
        voucher.setValidFrom(Instant.now());
        voucher.setValidTo(request.getExpiredAt());
        voucher.setTransferable(false);
        voucher.setStackable(false);
        voucher.setReusable(false);
        voucher.setMaxUsage(1);
        voucher.setFaceValue(request.getAmount());
        voucher.setMinimumOrderAmount(java.math.BigDecimal.ZERO);
        voucher.setConditionsJson(JsonNodeFactory.instance.objectNode());
        ObjectNode actions = JsonNodeFactory.instance.objectNode();
        actions.put("discountType", "FIXED_AMOUNT");
        actions.put("discountValue", request.getAmount());
        voucher.setActionsJson(actions);
        voucher.setMetadataJson(request.getMetadataJson());
        return voucher;
    }

    private void saveApproval(CompensationVoucher compensation, String action,
                              String oldStatus, String newStatus, String actor, String comment) {
        CompensationApprovalHistory history = new CompensationApprovalHistory();
        history.setTargetType(TARGET_TYPE);
        history.setTargetPublicId(compensation.getPublicId());
        history.setAction(action);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setApproverPublicId(actor);
        history.setComment(comment);
        history.setApprovedAt(Instant.now());
        history.setCreatedBy(actor);
        history.setUpdatedBy(actor);
        approvalHistoryRepository.save(history);
    }

    private CompensationResponse response(CompensationVoucher compensation) {
        Voucher voucher = voucherRepository.findByPublicIdAndDeletedAtIsNull(compensation.getVoucherPublicId())
                .orElse(null);
        CompensationResponse response = mapper.toCompensationResponse(compensation, voucher);
        List<com.project.promotionservice.benefit.dto.response.BenefitResponses.ApprovalRecordResponse> history =
                approvalHistoryRepository
                        .findByTargetTypeAndTargetPublicIdAndDeletedAtIsNullOrderByApprovedAtAsc(
                                TARGET_TYPE, compensation.getPublicId())
                        .stream().map(mapper::toApprovalResponse).toList();
        response.setApprovalHistory(history);
        return response;
    }

    private CompensationVoucher find(String publicId) {
        return compensationRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(
                        BenefitErrorCode.COMPENSATION_NOT_FOUND,
                        "Compensation voucher not found",
                        HttpStatus.NOT_FOUND));
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }
}
