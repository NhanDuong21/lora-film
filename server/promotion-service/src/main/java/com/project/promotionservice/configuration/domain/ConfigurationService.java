package com.project.promotionservice.configuration.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.integration.outbox.PromotionDomainEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

public interface ConfigurationService {
    ConfigurationResponse create(ConfigurationCreateRequest request, String actor);
    ConfigurationResponse update(String publicId, ConfigurationUpdateRequest request, String actor);
    void delete(String publicId, String actor);
    PagedResponse<ConfigurationResponse> search(String keyword, String category,
                                                ConfigurationStatus status, Pageable pageable);
    ConfigurationResponse detail(String publicId);
    int refreshCache();
    String get(String key);
    boolean getBoolean(String key, boolean defaultValue);
    int getInt(String key, int defaultValue);
}

@org.springframework.stereotype.Service
class ConfigurationServiceImpl implements ConfigurationService {
    private static final String TOPIC = "promotion.configuration.lifecycle";
    private final PromotionConfigurationRepository repository;
    private final ConfigurationCacheService cache;
    private final AuditTrailService audit;
    private final PromotionDomainEventService events;
    private final ObjectMapper objectMapper;

    ConfigurationServiceImpl(PromotionConfigurationRepository repository,
                             ConfigurationCacheService cache,
                             AuditTrailService audit,
                             PromotionDomainEventService events,
                             ObjectMapper objectMapper) {
        this.repository = repository;
        this.cache = cache;
        this.audit = audit;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ConfigurationResponse create(ConfigurationCreateRequest request, String actor) {
        String key = request.getConfigKey().trim().toUpperCase();
        if (repository.existsByConfigKey(key)) {
            throw bad("Configuration key already exists");
        }
        validate(request.getValueType(), request.getConfigValue(), request.getMetadataJson());
        PromotionConfiguration entity = new PromotionConfiguration();
        entity.setConfigKey(key);
        entity.setConfigValue(request.getConfigValue().trim());
        entity.setValueType(request.getValueType());
        entity.setCategory(request.getCategory().trim().toUpperCase());
        entity.setDescription(request.getDescription());
        entity.setEditable(request.getEditable());
        entity.setRequiresRestart(request.getRequiresRestart());
        entity.setStatus(request.getStatus() == null ? ConfigurationStatus.ACTIVE : request.getStatus());
        entity.setMetadataJson(request.getMetadataJson());
        entity.setCreatedBy(actor);
        entity.setUpdatedBy(actor);
        PromotionConfiguration saved = repository.save(entity);
        ConfigurationResponse response = toResponse(saved);
        audit.record("PROMOTION_CONFIGURATION", saved.getPublicId(), "CONFIGURATION_CREATED",
                null, response, actor);
        events.enqueue("PROMOTION_CONFIGURATION", saved.getPublicId(), "CONFIGURATION_CREATED",
                TOPIC, response, actor);
        if (Boolean.FALSE.equals(saved.getRequiresRestart())
                && saved.getStatus() == ConfigurationStatus.ACTIVE) cache.put(response);
        return response;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ConfigurationResponse update(String publicId, ConfigurationUpdateRequest request, String actor) {
        PromotionConfiguration entity = repository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Configuration not found"));
        if (entity.getStatus() == ConfigurationStatus.DEPRECATED
                || Boolean.FALSE.equals(entity.getEditable())) {
            throw bad("Configuration is not editable");
        }
        ConfigurationResponse before = toResponse(entity);
        validate(request.getValueType(), request.getConfigValue(), request.getMetadataJson());
        entity.setConfigValue(request.getConfigValue().trim());
        entity.setValueType(request.getValueType());
        entity.setCategory(request.getCategory().trim().toUpperCase());
        entity.setDescription(request.getDescription());
        entity.setEditable(request.getEditable());
        entity.setRequiresRestart(request.getRequiresRestart());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        entity.setMetadataJson(request.getMetadataJson());
        entity.setUpdatedBy(actor);
        PromotionConfiguration saved = repository.save(entity);
        ConfigurationResponse response = toResponse(saved);
        audit.record("PROMOTION_CONFIGURATION", publicId, "CONFIGURATION_UPDATED",
                before, response, actor);
        events.enqueue("PROMOTION_CONFIGURATION", publicId, "CONFIGURATION_UPDATED",
                TOPIC, response, actor);
        if (saved.getStatus() == ConfigurationStatus.ACTIVE
                && Boolean.FALSE.equals(saved.getRequiresRestart())) cache.put(response);
        else cache.evict(saved.getConfigKey());
        return response;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void delete(String publicId, String actor) {
        PromotionConfiguration entity = repository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Configuration not found"));
        ConfigurationResponse before = toResponse(entity);
        entity.setStatus(ConfigurationStatus.DEPRECATED);
        entity.setDeletedAt(java.time.Instant.now());
        entity.setDeletedBy(actor);
        entity.setUpdatedBy(actor);
        repository.save(entity);
        cache.evict(entity.getConfigKey());
        audit.record("PROMOTION_CONFIGURATION", publicId, "CONFIGURATION_DELETED",
                before, toResponse(entity), actor);
        events.enqueue("PROMOTION_CONFIGURATION", publicId, "CONFIGURATION_DELETED",
                TOPIC, toResponse(entity), actor);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PagedResponse<ConfigurationResponse> search(String keyword, String category,
                                                       ConfigurationStatus status, Pageable pageable) {
        Page<PromotionConfiguration> page = repository.findAll((root, query, cb) -> {
            var p = cb.conjunction();
            p.getExpressions().add(cb.isNull(root.get("deletedAt")));
            if (keyword != null && !keyword.isBlank()) {
                p.getExpressions().add(cb.like(cb.lower(root.get("configKey")),
                        "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (category != null && !category.isBlank()) {
                p.getExpressions().add(cb.equal(root.get("category"), category.trim().toUpperCase()));
            }
            if (status != null) p.getExpressions().add(cb.equal(root.get("status"), status));
            return p;
        }, pageable);
        return new PagedResponse<>(page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ConfigurationResponse detail(String publicId) {
        return toResponse(repository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Configuration not found")));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public int refreshCache() {
        cache.clear();
        var active = repository.findByStatusAndDeletedAtIsNull(ConfigurationStatus.ACTIVE);
        int loaded = 0;
        for (PromotionConfiguration configuration : active) {
            if (Boolean.FALSE.equals(configuration.getRequiresRestart())) {
                cache.put(toResponse(configuration));
                loaded++;
            }
        }
        return loaded;
    }

    @Override
    public String get(String key) {
        String cached = cache.get(key);
        if (cached != null) return cached;
        return repository.findByConfigKeyAndDeletedAtIsNull(key.trim().toUpperCase())
                .filter(c -> c.getStatus() == ConfigurationStatus.ACTIVE
                        && Boolean.FALSE.equals(c.getRequiresRestart()))
                .map(c -> {
                    ConfigurationResponse r = toResponse(c);
                    cache.put(r);
                    return r.getConfigValue();
                }).orElse(null);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) return defaultValue;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return defaultValue; }
    }

    private void validate(ConfigurationValueType type, String value, String metadata) {
        if (type == null || value == null || value.isBlank()) throw bad("Configuration value is required");
        try {
            JsonNode metadataNode = metadata == null || metadata.isBlank()
                    ? null : objectMapper.readTree(metadata);
            switch (type) {
                case INTEGER -> Integer.parseInt(value.trim());
                case LONG, DURATION -> Long.parseLong(value.trim());
                case DECIMAL -> new java.math.BigDecimal(value.trim());
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(value.trim()) && !"false".equalsIgnoreCase(value.trim())) {
                        throw new IllegalArgumentException();
                    }
                }
                case JSON -> objectMapper.readTree(value);
                case STRING -> { }
            }
            validateNumericBounds(type, value, metadataNode);
        } catch (Exception ex) {
            throw bad("Configuration value does not match its declared type");
        }
    }

    private void validateNumericBounds(ConfigurationValueType type, String value, JsonNode metadata) {
        if (metadata == null || !metadata.isObject()
                || !(type == ConfigurationValueType.INTEGER
                || type == ConfigurationValueType.LONG
                || type == ConfigurationValueType.DURATION
                || type == ConfigurationValueType.DECIMAL)) {
            return;
        }
        java.math.BigDecimal number = new java.math.BigDecimal(value.trim());
        JsonNode minimum = metadata.get("minimum");
        JsonNode maximum = metadata.get("maximum");
        if (minimum != null && !minimum.isNull()
                && number.compareTo(new java.math.BigDecimal(minimum.asText())) < 0) {
            throw new IllegalArgumentException("minimum exceeded");
        }
        if (maximum != null && !maximum.isNull()
                && number.compareTo(new java.math.BigDecimal(maximum.asText())) > 0) {
            throw new IllegalArgumentException("maximum exceeded");
        }
    }

    private ConfigurationResponse toResponse(PromotionConfiguration e) {
        ConfigurationResponse r = new ConfigurationResponse();
        r.setPublicId(e.getPublicId()); r.setConfigKey(e.getConfigKey()); r.setConfigValue(e.getConfigValue());
        r.setValueType(e.getValueType()); r.setCategory(e.getCategory()); r.setDescription(e.getDescription());
        r.setEditable(e.getEditable()); r.setRequiresRestart(e.getRequiresRestart()); r.setStatus(e.getStatus());
        r.setMetadataJson(e.getMetadataJson()); r.setVersion(e.getVersion());
        r.setCreatedAt(e.getCreatedAt()); r.setCreatedBy(e.getCreatedBy());
        r.setUpdatedAt(e.getUpdatedAt()); r.setUpdatedBy(e.getUpdatedBy());
        return r;
    }

    private static BusinessException bad(String m) {
        return new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, m, HttpStatus.BAD_REQUEST);
    }
    private static BusinessException notFound(String m) {
        return new BusinessException(ErrorCode.NOT_FOUND, m, HttpStatus.NOT_FOUND);
    }
}
