package com.project.promotionservice.partner.service;

import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.integration.outbox.PromotionDomainEventService;
import com.project.promotionservice.partner.dto.request.PartnerCreateRequest;
import com.project.promotionservice.partner.dto.request.PartnerUpdateRequest;
import com.project.promotionservice.partner.dto.response.PartnerResponse;
import com.project.promotionservice.partner.entity.Partner;
import com.project.promotionservice.partner.enums.PartnerStatus;
import com.project.promotionservice.partner.repository.PartnerRepository;
import com.project.promotionservice.partner.repository.PartnerSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PartnerServiceImpl implements PartnerService {
    private static final String TOPIC = "promotion.partner.lifecycle";
    private final PartnerRepository repository;
    private final PartnerCacheService cache;
    private final AuditTrailService audit;
    private final PromotionDomainEventService events;

    public PartnerServiceImpl(PartnerRepository repository, PartnerCacheService cache,
                              AuditTrailService audit, PromotionDomainEventService events) {
        this.repository = repository;
        this.cache = cache;
        this.audit = audit;
        this.events = events;
    }

    @Override
    @Transactional
    public PartnerResponse create(PartnerCreateRequest request, String actor) {
        String code = request.getCode().trim().toUpperCase();
        if (repository.existsByCode(code)) {
            throw bad("Partner code already exists");
        }
        validateContact(request.getEmail(), request.getPhone(), request.getContactPerson());
        validateContract(request.getContractStartAt(), request.getContractEndAt());
        Partner entity = new Partner();
        entity.setCode(code);
        copy(entity, request);
        entity.setStatus(PartnerStatus.ACTIVE);
        entity.setCreatedBy(actor);
        entity.setUpdatedBy(actor);
        Partner saved = repository.save(entity);
        PartnerResponse response = toResponse(saved);
        audit.record("PARTNER", saved.getPublicId(), "PARTNER_CREATED", null, response, actor);
        events.enqueue("PARTNER", saved.getPublicId(), "PARTNER_CREATED", TOPIC, response, actor);
        cache.put(response);
        return response;
    }

    @Override
    @Transactional
    public PartnerResponse update(String publicId, PartnerUpdateRequest request, String actor) {
        Partner entity = require(publicId);
        if (entity.getStatus() == PartnerStatus.TERMINATED) {
            throw bad("Terminated partner cannot be updated");
        }
        validateContact(request.getEmail(), request.getPhone(), request.getContactPerson());
        validateContract(request.getContractStartAt(), request.getContractEndAt());
        PartnerResponse before = toResponse(entity);
        copy(entity, request);
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        entity.setUpdatedBy(actor);
        Partner saved = repository.save(entity);
        PartnerResponse response = toResponse(saved);
        audit.record("PARTNER", publicId, "PARTNER_UPDATED", before, response, actor);
        events.enqueue("PARTNER", publicId, "PARTNER_UPDATED", TOPIC, response, actor);
        cache.put(response);
        return response;
    }

    @Override
    @Transactional
    public void disable(String publicId, String actor) {
        Partner entity = require(publicId);
        if (entity.getStatus() == PartnerStatus.TERMINATED) {
            throw bad("Terminated partner cannot be disabled again");
        }
        PartnerResponse before = toResponse(entity);
        entity.setStatus(PartnerStatus.INACTIVE);
        entity.setDeletedAt(Instant.now());
        entity.setDeletedBy(actor);
        entity.setUpdatedBy(actor);
        repository.save(entity);
        PartnerResponse after = toResponse(entity);
        audit.record("PARTNER", publicId, "PARTNER_DISABLED", before, after, actor);
        events.enqueue("PARTNER", publicId, "PARTNER_DISABLED", TOPIC, after, actor);
        cache.evict(publicId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PartnerResponse> search(String keyword, PartnerStatus status, Pageable pageable) {
        Page<Partner> page = repository.findAll(PartnerSpecifications.partnerSearch(keyword, status), pageable);
        return new PagedResponse<>(page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerResponse detail(String publicId) {
        PartnerResponse response = toResponse(require(publicId));
        cache.put(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireActive(String publicId) {
        Partner entity = repository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Partner not found"));
        if (entity.getStatus() != PartnerStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Partner is not active", HttpStatus.BAD_REQUEST);
        }
        if (entity.getContractEndAt() != null && !Instant.now().isBefore(entity.getContractEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Partner contract has expired", HttpStatus.BAD_REQUEST);
        }
    }

    private Partner require(String publicId) {
        return repository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Partner not found"));
    }

    private void copy(Partner e, PartnerCreateRequest r) {
        e.setName(r.getName().trim());
        e.setPartnerType(r.getPartnerType());
        e.setTaxCode(blankToNull(r.getTaxCode()));
        e.setEmail(blankToNull(r.getEmail()));
        e.setPhone(blankToNull(r.getPhone()));
        e.setContactPerson(blankToNull(r.getContactPerson()));
        e.setAddress(blankToNull(r.getAddress()));
        e.setWebsite(blankToNull(r.getWebsite()));
        e.setContractNumber(blankToNull(r.getContractNumber()));
        e.setContractStartAt(r.getContractStartAt());
        e.setContractEndAt(r.getContractEndAt());
        e.setSettlementCycle(r.getSettlementCycle());
        e.setMetadataJson(blankToNull(r.getMetadataJson()));
    }

    private void copy(Partner e, PartnerUpdateRequest r) {
        e.setName(r.getName().trim());
        e.setPartnerType(r.getPartnerType());
        e.setTaxCode(blankToNull(r.getTaxCode()));
        e.setEmail(blankToNull(r.getEmail()));
        e.setPhone(blankToNull(r.getPhone()));
        e.setContactPerson(blankToNull(r.getContactPerson()));
        e.setAddress(blankToNull(r.getAddress()));
        e.setWebsite(blankToNull(r.getWebsite()));
        e.setContractNumber(blankToNull(r.getContractNumber()));
        e.setContractStartAt(r.getContractStartAt());
        e.setContractEndAt(r.getContractEndAt());
        e.setSettlementCycle(r.getSettlementCycle());
        e.setMetadataJson(blankToNull(r.getMetadataJson()));
    }

    private void validateContact(String email, String phone, String contactPerson) {
        if (blank(email) && blank(phone) && blank(contactPerson)) {
            throw bad("At least one valid partner contact is required");
        }
    }

    private void validateContract(Instant from, Instant to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw bad("Contract end must be after contract start");
        }
    }

    private PartnerResponse toResponse(Partner e) {
        PartnerResponse r = new PartnerResponse();
        r.setPublicId(e.getPublicId()); r.setCode(e.getCode()); r.setName(e.getName());
        r.setPartnerType(e.getPartnerType()); r.setStatus(e.getStatus());
        r.setTaxCode(e.getTaxCode()); r.setEmail(e.getEmail()); r.setPhone(e.getPhone());
        r.setContactPerson(e.getContactPerson()); r.setAddress(e.getAddress());
        r.setWebsite(e.getWebsite()); r.setContractNumber(e.getContractNumber());
        r.setContractStartAt(e.getContractStartAt()); r.setContractEndAt(e.getContractEndAt());
        r.setSettlementCycle(e.getSettlementCycle()); r.setMetadataJson(e.getMetadataJson());
        r.setVersion(e.getVersion()); r.setCreatedAt(e.getCreatedAt()); r.setCreatedBy(e.getCreatedBy());
        r.setUpdatedAt(e.getUpdatedAt()); r.setUpdatedBy(e.getUpdatedBy());
        return r;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String blankToNull(String value) { return blank(value) ? null : value.trim(); }
    private static BusinessException bad(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, message, HttpStatus.BAD_REQUEST);
    }
    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}
