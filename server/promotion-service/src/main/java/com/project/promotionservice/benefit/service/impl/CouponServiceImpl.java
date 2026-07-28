package com.project.promotionservice.benefit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponCreateRequest;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponGenerateRequest;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponUpdateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponImportResult;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponResponse;
import com.project.promotionservice.benefit.entity.Coupon;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponType;
import com.project.promotionservice.benefit.enums.BenefitEnums.DistributionType;
import com.project.promotionservice.benefit.exception.BenefitErrorCode;
import com.project.promotionservice.benefit.mapper.BenefitMapper;
import com.project.promotionservice.benefit.repository.CouponRepository;
import com.project.promotionservice.benefit.service.BenefitEventService;
import com.project.promotionservice.benefit.service.BenefitPolicyValidator;
import com.project.promotionservice.benefit.service.CouponService;
import com.project.promotionservice.benefit.specification.BenefitSpecifications;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final BenefitMapper mapper;
    private final BenefitEventService eventService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final BenefitPolicyValidator policyValidator;

    public CouponServiceImpl(CouponRepository couponRepository,
                             PromotionCampaignRepository campaignRepository,
                             BenefitMapper mapper,
                             BenefitEventService eventService,
                             ObjectMapper objectMapper,
                             Validator validator,
                             BenefitPolicyValidator policyValidator) {
        this.couponRepository = couponRepository;
        this.campaignRepository = campaignRepository;
        this.mapper = mapper;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.policyValidator = policyValidator;
    }

    @Override
    @Transactional
    @Auditable(action = "CREATE", entityType = "COUPON")
    @CacheEvict(cacheNames = "promotions", allEntries = true)
    public CouponResponse create(CouponCreateRequest request, String actor) {
        validatePolicy(request);
        requireCampaign(request.getCampaignPublicId());
        String code = mapper.normalizeCode(request.getCode());
        requireUniqueCode(code, null);
        Coupon saved = couponRepository.save(mapper.toCoupon(request, actor));
        CouponResponse response = mapper.toCouponResponse(saved);
        eventService.record("COUPON", saved.getPublicId(), "COUPON_CREATED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "GENERATE", entityType = "COUPON")
    @CacheEvict(cacheNames = "promotions", allEntries = true)
    public List<CouponResponse> generate(CouponGenerateRequest request, String actor) {
        policyValidator.validateCoupon(
                request.getCouponType(), request.getDistributionType(), request.getReusable(),
                request.getMaxRedemptions(), request.getMaxRedemptionsPerUser(),
                request.getConditionsJson(), request.getActionsJson());
        requireCampaign(request.getCampaignPublicId());
        String prefix = sanitizePrefix(request.getPrefix());
        List<Coupon> coupons = new ArrayList<>(request.getQuantity());
        Set<String> generatedCodes = new HashSet<>();
        while (coupons.size() < request.getQuantity()) {
            String code = prefix + "-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase(Locale.ROOT);
            if (generatedCodes.add(code) && !couponRepository.existsByCodeIgnoreCase(code)) {
                coupons.add(mapper.toGeneratedCoupon(request, code, actor));
            }
        }
        List<Coupon> saved = couponRepository.saveAll(coupons);
        List<CouponResponse> responses = saved.stream().map(mapper::toCouponResponse).toList();
        for (CouponResponse response : responses) {
            eventService.record("COUPON", response.getPublicId(), "COUPON_CREATED", response, actor);
        }
        return responses;
    }

    @Override
    @Transactional
    @Auditable(action = "IMPORT", entityType = "COUPON")
    @CacheEvict(cacheNames = "promotions", allEntries = true)
    public CouponImportResult importCsv(MultipartFile file, String actor) {
        if (file == null || file.isEmpty()) {
            throw badRequest(BenefitErrorCode.IMPORT_INVALID, "CSV file must not be empty");
        }

        CouponImportResult result = new CouponImportResult();
        List<Coupon> validCoupons = new ArrayList<>();
        Set<String> fileCodes = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(file.getBytes()), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw badRequest(BenefitErrorCode.IMPORT_INVALID, "CSV header is missing");
            }
            Map<String, Integer> headers = headerIndexes(parseCsvLine(stripBom(headerLine)));
            requireImportHeaders(headers);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                result.setTotalRows(result.getTotalRows() + 1);
                try {
                    List<String> row = parseCsvLine(line);
                    CouponCreateRequest request = importRequest(headers, row);
                    requireCampaign(request.getCampaignPublicId());
                    String code = mapper.normalizeCode(request.getCode());
                    if (!fileCodes.add(code) || couponRepository.existsByCodeIgnoreCase(code)) {
                        throw badRequest(BenefitErrorCode.COUPON_DUPLICATE, "Coupon code already exists: " + code);
                    }
                    validCoupons.add(mapper.toCoupon(request, actor));
                } catch (RuntimeException exception) {
                    result.getErrors().add("Row " + rowNumber + ": " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw badRequest(BenefitErrorCode.IMPORT_INVALID, "Unable to read CSV file");
        }

        List<Coupon> saved = couponRepository.saveAll(validCoupons);
        List<CouponResponse> responses = saved.stream().map(mapper::toCouponResponse).toList();
        result.setCoupons(responses);
        result.setImportedRows(responses.size());
        result.setRejectedRows(result.getTotalRows() - result.getImportedRows());
        for (CouponResponse response : responses) {
            eventService.record("COUPON", response.getPublicId(), "COUPON_CREATED", response, actor);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(String keyword, String campaignPublicId, CouponStatus status) {
        List<Coupon> coupons = couponRepository.findAll(
                BenefitSpecifications.coupons(keyword, campaignPublicId, status, null),
                Sort.by(Sort.Direction.ASC, "code"));
        StringBuilder csv = new StringBuilder();
        csv.append("campaign_public_id,code,name,description,coupon_type,status,distribution_type,")
                .append("stackable,transferable,reusable,auto_apply,priority,max_redemptions,")
                .append("redemption_count,max_redemptions_per_user,valid_from,valid_to,")
                .append("conditions_json,actions_json,metadata_json\r\n");
        for (Coupon coupon : coupons) {
            csv.append(csv(coupon.getCampaignPublicId())).append(',')
                    .append(csv(coupon.getCode())).append(',')
                    .append(csv(coupon.getName())).append(',')
                    .append(csv(coupon.getDescription())).append(',')
                    .append(csv(coupon.getCouponType())).append(',')
                    .append(csv(coupon.getStatus())).append(',')
                    .append(csv(coupon.getDistributionType())).append(',')
                    .append(csv(coupon.getStackable())).append(',')
                    .append(csv(coupon.getTransferable())).append(',')
                    .append(csv(coupon.getReusable())).append(',')
                    .append(csv(coupon.getAutoApply())).append(',')
                    .append(csv(coupon.getPriority())).append(',')
                    .append(csv(coupon.getMaxRedemptions())).append(',')
                    .append(csv(coupon.getRedemptionCount())).append(',')
                    .append(csv(coupon.getMaxRedemptionsPerUser())).append(',')
                    .append(csv(coupon.getValidFrom())).append(',')
                    .append(csv(coupon.getValidTo())).append(',')
                    .append(csv(coupon.getConditionsJson())).append(',')
                    .append(csv(coupon.getActionsJson())).append(',')
                    .append(csv(coupon.getMetadataJson())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE", entityType = "COUPON")
    @CacheEvict(cacheNames = "promotions", allEntries = true)
    public CouponResponse update(String publicId, CouponUpdateRequest request, String actor) {
        Coupon coupon = find(publicId);
        if (coupon.getRedemptionCount() > 0
                && (request.getConditionsJson() != null || request.getActionsJson() != null)) {
            throw badRequest(
                    BenefitErrorCode.BENEFIT_CONFIGURATION_INVALID,
                    "Coupon conditions and actions are immutable after the first redemption");
        }
        if (request.getName() != null) coupon.setName(request.getName());
        if (request.getDescription() != null) coupon.setDescription(request.getDescription());
        if (request.getCouponType() != null) coupon.setCouponType(request.getCouponType());
        if (request.getStatus() != null) coupon.setStatus(request.getStatus());
        if (request.getDistributionType() != null) coupon.setDistributionType(request.getDistributionType());
        if (request.getStackable() != null) coupon.setStackable(request.getStackable());
        if (request.getTransferable() != null) coupon.setTransferable(request.getTransferable());
        if (request.getReusable() != null) coupon.setReusable(request.getReusable());
        if (request.getAutoApply() != null) coupon.setAutoApply(request.getAutoApply());
        if (request.getPriority() != null) coupon.setPriority(request.getPriority());
        if (request.getMaxRedemptions() != null) {
            if (request.getMaxRedemptions() < coupon.getRedemptionCount()) {
                throw badRequest(ErrorCode.INVALID_REQUEST_PARAMETER,
                        "maxRedemptions cannot be lower than redemptionCount");
            }
            coupon.setMaxRedemptions(request.getMaxRedemptions());
        }
        if (request.getMaxRedemptionsPerUser() != null) {
            coupon.setMaxRedemptionsPerUser(request.getMaxRedemptionsPerUser());
        }
        Instant validFrom = request.getValidFrom() == null ? coupon.getValidFrom() : request.getValidFrom();
        Instant validTo = request.getValidTo() == null ? coupon.getValidTo() : request.getValidTo();
        requirePeriod(validFrom, validTo);
        coupon.setValidFrom(validFrom);
        coupon.setValidTo(validTo);
        if (request.getConditionsJson() != null) coupon.setConditionsJson(mapper.toJson(request.getConditionsJson()));
        if (request.getActionsJson() != null) coupon.setActionsJson(mapper.toJson(request.getActionsJson()));
        if (request.getMetadataJson() != null) coupon.setMetadataJson(mapper.toNullableJson(request.getMetadataJson()));
        policyValidator.validateCoupon(
                coupon.getCouponType(), coupon.getDistributionType(), coupon.getReusable(),
                coupon.getMaxRedemptions(), coupon.getMaxRedemptionsPerUser(),
                mapper.toNode(coupon.getConditionsJson()), mapper.toNode(coupon.getActionsJson()));
        coupon.setUpdatedBy(actor);

        CouponResponse response = mapper.toCouponResponse(couponRepository.save(coupon));
        eventService.record("COUPON", publicId, "COUPON_UPDATED", response, actor);
        return response;
    }

    @Override
    @Transactional
    @Auditable(action = "DISABLE", entityType = "COUPON")
    @CacheEvict(cacheNames = "promotions", allEntries = true)
    public void disable(String publicId, String actor) {
        Coupon coupon = find(publicId);
        coupon.setStatus(CouponStatus.DISABLED);
        coupon.setUpdatedBy(actor);
        CouponResponse response = mapper.toCouponResponse(couponRepository.save(coupon));
        eventService.record("COUPON", publicId, "COUPON_UPDATED", response, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CouponResponse> search(
            String keyword, String campaignPublicId, CouponStatus status, Instant validAt, Pageable pageable) {
        Page<Coupon> result = couponRepository.findAll(
                BenefitSpecifications.coupons(keyword, campaignPublicId, status, validAt), pageable);
        return page(result.map(mapper::toCouponResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "promotions", key = "'coupon:' + #publicId")
    public CouponResponse get(String publicId) {
        return mapper.toCouponResponse(find(publicId));
    }

    private Coupon find(String publicId) {
        return couponRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(
                        BenefitErrorCode.COUPON_NOT_FOUND, "Coupon not found", HttpStatus.NOT_FOUND));
    }

    private PromotionCampaign requireCampaign(String publicId) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Promotion campaign not found", HttpStatus.NOT_FOUND));
        return campaign;
    }

    private void requireUniqueCode(String code, String currentPublicId) {
        if (currentPublicId == null && couponRepository.existsByCodeIgnoreCase(code)) {
            throw badRequest(BenefitErrorCode.COUPON_DUPLICATE, "Coupon code already exists");
        }
        if (currentPublicId != null) {
            couponRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(code).ifPresent(existing -> {
                if (!existing.getPublicId().equals(currentPublicId)) {
                    throw badRequest(BenefitErrorCode.COUPON_DUPLICATE, "Coupon code already exists");
                }
            });
        }
    }

    private void requirePeriod(Instant from, Instant to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw badRequest(ErrorCode.INVALID_REQUEST_PARAMETER, "validTo must be after validFrom");
        }
    }

    private String sanitizePrefix(String input) {
        String value = input == null ? "CPN" : input.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (value.isBlank()) value = "CPN";
        return value.length() > 50 ? value.substring(0, 50) : value;
    }

    private CouponCreateRequest importRequest(Map<String, Integer> headers, List<String> row) {
        CouponCreateRequest request = new CouponCreateRequest();
        request.setCampaignPublicId(value(headers, row, "campaignpublicid"));
        request.setCode(value(headers, row, "code"));
        request.setName(value(headers, row, "name"));
        request.setDescription(nullableValue(headers, row, "description"));
        request.setCouponType(CouponType.valueOf(value(headers, row, "coupontype").toUpperCase(Locale.ROOT)));
        String status = nullableValue(headers, row, "status");
        request.setStatus(status == null ? CouponStatus.ACTIVE : CouponStatus.valueOf(status.toUpperCase(Locale.ROOT)));
        request.setDistributionType(DistributionType.valueOf(
                value(headers, row, "distributiontype").toUpperCase(Locale.ROOT)));
        request.setStackable(booleanValue(headers, row, "stackable", false));
        request.setTransferable(booleanValue(headers, row, "transferable", false));
        request.setReusable(booleanValue(headers, row, "reusable", false));
        request.setAutoApply(booleanValue(headers, row, "autoapply", false));
        request.setPriority(integerValue(headers, row, "priority", 100));
        request.setMaxRedemptions(nullableIntegerValue(headers, row, "maxredemptions"));
        request.setMaxRedemptionsPerUser(integerValue(headers, row, "maxredemptionsperuser", 1));
        request.setValidFrom(Instant.parse(value(headers, row, "validfrom")));
        request.setValidTo(Instant.parse(value(headers, row, "validto")));
        request.setConditionsJson(jsonValue(headers, row, "conditionsjson", true));
        request.setActionsJson(jsonValue(headers, row, "actionsjson", true));
        request.setMetadataJson(jsonValue(headers, row, "metadatajson", false));

        if (request.getCampaignPublicId().isBlank() || request.getCode().isBlank() || request.getName().isBlank()) {
            throw badRequest(BenefitErrorCode.IMPORT_INVALID, "Required value is blank");
        }
        requirePeriod(request.getValidFrom(), request.getValidTo());
        if (request.getMaxRedemptions() != null && request.getMaxRedemptions() < 1) {
            throw badRequest(BenefitErrorCode.IMPORT_INVALID, "maxRedemptions must be positive");
        }
        Set<ConstraintViolation<CouponCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            ConstraintViolation<CouponCreateRequest> violation = violations.iterator().next();
            throw badRequest(
                    BenefitErrorCode.IMPORT_INVALID,
                    violation.getPropertyPath() + " " + violation.getMessage());
        }
        validatePolicy(request);
        return request;
    }

    private void validatePolicy(CouponCreateRequest request) {
        policyValidator.validateCoupon(
                request.getCouponType(), request.getDistributionType(), request.getReusable(),
                request.getMaxRedemptions(), request.getMaxRedemptionsPerUser(),
                request.getConditionsJson(), request.getActionsJson());
    }

    private void requireImportHeaders(Map<String, Integer> headers) {
        List<String> required = List.of(
                "campaignpublicid", "code", "name", "coupontype", "distributiontype",
                "validfrom", "validto", "conditionsjson", "actionsjson");
        for (String header : required) {
            if (!headers.containsKey(header)) {
                throw badRequest(BenefitErrorCode.IMPORT_INVALID, "Missing CSV column: " + header);
            }
        }
    }

    private Map<String, Integer> headerIndexes(List<String> values) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < values.size(); i++) {
            result.put(normalizeHeader(values.get(i)), i);
        }
        return result;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace("_", "");
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw badRequest(BenefitErrorCode.IMPORT_INVALID, "Unclosed quoted field");
        }
        values.add(current.toString().trim());
        return values;
    }

    private String value(Map<String, Integer> headers, List<String> row, String name) {
        String result = nullableValue(headers, row, name);
        if (result == null) {
            throw badRequest(BenefitErrorCode.IMPORT_INVALID, "Missing value for " + name);
        }
        return result;
    }

    private String nullableValue(Map<String, Integer> headers, List<String> row, String name) {
        Integer index = headers.get(name);
        if (index == null || index >= row.size()) return null;
        String result = row.get(index).trim();
        return result.isEmpty() ? null : result;
    }

    private boolean booleanValue(Map<String, Integer> headers, List<String> row, String name, boolean fallback) {
        String value = nullableValue(headers, row, name);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private int integerValue(Map<String, Integer> headers, List<String> row, String name, int fallback) {
        Integer value = nullableIntegerValue(headers, row, name);
        return value == null ? fallback : value;
    }

    private Integer nullableIntegerValue(Map<String, Integer> headers, List<String> row, String name) {
        String value = nullableValue(headers, row, name);
        return value == null ? null : new BigDecimal(value).intValueExact();
    }

    private JsonNode jsonValue(Map<String, Integer> headers, List<String> row, String name, boolean required) {
        String value = nullableValue(headers, row, name);
        if (value == null) {
            if (required) return JsonNodeFactory.instance.objectNode();
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw badRequest(BenefitErrorCode.IMPORT_INVALID, "Invalid JSON in " + name);
        }
    }

    private String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
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
