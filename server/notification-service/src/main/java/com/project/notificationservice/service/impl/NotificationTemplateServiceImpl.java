package com.project.notificationservice.service.impl;

import com.project.notificationservice.dto.request.CreateNotificationTemplateRequest;
import com.project.notificationservice.dto.request.UpdateNotificationTemplateRequest;
import com.project.notificationservice.dto.response.NotificationTemplateResponse;
import com.project.notificationservice.dto.response.NotificationTemplateSummaryResponse;
import com.project.notificationservice.entity.NotificationTemplate;
import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.exception.BusinessException;
import com.project.notificationservice.mapper.NotificationTemplateMapper;
import com.project.notificationservice.repository.NotificationTemplateRepository;
import com.project.notificationservice.service.NotificationTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    public NotificationTemplateServiceImpl(NotificationTemplateRepository repository) {
        this.repository = repository;
    }

    private void validateHtmlFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("HTML file is empty or not provided", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".html")) {
            throw new BusinessException("Only HTML files are allowed", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        try {
            byte[] bytes = file.getBytes();
            java.nio.charset.CharsetDecoder decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
            decoder.decode(buf);
        } catch (Exception e) {
            throw new BusinessException("HTML file is not valid UTF-8 encoded", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
    }

    private String readHtmlContent(org.springframework.web.multipart.MultipartFile file) {
        try {
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("HTML file is not valid UTF-8 encoded", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional
    public NotificationTemplateResponse createTemplate(CreateNotificationTemplateRequest request) {
        String code = request.getTemplateCode().trim().toUpperCase();
        if (repository.existsByTemplateCode(code)) {
            throw new BusinessException(
                    "Notification template code already exists",
                    "NOTIFICATION_TEMPLATE_CODE_ALREADY_EXISTS",
                    HttpStatus.CONFLICT
            );
        }

        validateHtmlFile(request.getHtmlFile());
        String htmlContent = readHtmlContent(request.getHtmlFile());

        NotificationTemplate template = new NotificationTemplate(
                code,
                request.getTitle().trim(),
                htmlContent.trim(),
                request.getChannelType(),
                request.getIsActive()
        );

        NotificationTemplate saved = repository.save(template);
        return NotificationTemplateMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationTemplateSummaryResponse> getTemplateList(int page, int size, String code,
                                                                    NotificationChannel channelType, Boolean isActive, String sort) {
        // Enforce page constraints
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        } else if (size > 50) {
            size = 50;
        }

        // Enforce sort field whitelist and parsing
        Sort parsedSort = parseSort(sort);

        Pageable pageable = PageRequest.of(page, size, parsedSort);

        Specification<NotificationTemplate> spec = (root, query, cb) -> cb.conjunction();

        if (code != null && !code.trim().isEmpty()) {
            String cleanCode = code.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("templateCode")), cleanCode));
        }

        if (channelType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("channelType"), channelType));
        }

        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        Page<NotificationTemplate> templates = repository.findAll(spec, pageable);
        return templates.map(NotificationTemplateMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplateDetail(Integer templateId) {
        if (templateId == null || templateId <= 0) {
            throw new BusinessException(
                    "Notification template not found",
                    "NOTIFICATION_TEMPLATE_NOT_FOUND",
                    HttpStatus.NOT_FOUND
            );
        }
        NotificationTemplate template = repository.findById(templateId)
                .orElseThrow(() -> new BusinessException(
                        "Notification template not found",
                        "NOTIFICATION_TEMPLATE_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));
        return NotificationTemplateMapper.toResponse(template);
    }

    @Override
    @Transactional
    public NotificationTemplateResponse updateTemplate(Integer templateId, UpdateNotificationTemplateRequest request) {
        if (templateId == null || templateId <= 0) {
            throw new BusinessException(
                    "Notification template not found",
                    "NOTIFICATION_TEMPLATE_NOT_FOUND",
                    HttpStatus.NOT_FOUND
            );
        }

        NotificationTemplate template = repository.findById(templateId)
                .orElseThrow(() -> new BusinessException(
                        "Notification template not found",
                        "NOTIFICATION_TEMPLATE_NOT_FOUND",
                        HttpStatus.NOT_FOUND
                ));

        // Enforce templateCode immutability (only if provided and not empty)
        if (request.getTemplateCode() != null && !request.getTemplateCode().trim().isEmpty()) {
            String newCode = request.getTemplateCode().trim().toUpperCase();
            if (!template.getTemplateCode().equalsIgnoreCase(newCode)) {
                throw new BusinessException(
                        "Template code is immutable and cannot be changed",
                        "VALIDATION_ERROR",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        // Manual version pre-check
        if (!template.getVersion().equals(request.getVersion())) {
            throw new BusinessException(
                    "Optimistic lock conflict occurred. Stale version provided.",
                    "NOTIFICATION_OPTIMISTIC_LOCK_CONFLICT",
                    HttpStatus.CONFLICT
            );
        }

        // Partial updates
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            String title = request.getTitle().trim();
            if (title.length() > 255) {
                throw new BusinessException("Title must not exceed 255 characters", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
            template.setTitle(title);
        }

        if (request.getChannelType() != null) {
            template.setChannelType(request.getChannelType());
        }

        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }

        if (request.getHtmlFile() != null && !request.getHtmlFile().isEmpty()) {
            validateHtmlFile(request.getHtmlFile());
            String htmlContent = readHtmlContent(request.getHtmlFile());
            template.setContent(htmlContent.trim());
        }

        NotificationTemplate saved = repository.saveAndFlush(template);
        return NotificationTemplateMapper.toResponse(saved);
    }

    private Sort parseSort(String sortParam) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sortParam != null && !sortParam.trim().isEmpty()) {
            String[] parts = sortParam.split(",");
            String property = parts[0].trim();
            // Validate property against whitelist
            if ("createdAt".equals(property) || "updatedAt".equals(property) || "templateCode".equals(property)) {
                Sort.Direction direction = Sort.Direction.ASC;
                if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                    direction = Sort.Direction.DESC;
                }
                orders.add(new Sort.Order(direction, property));
            }
        }
        if (orders.isEmpty()) {
            // Default sort
            orders.add(new Sort.Order(Sort.Direction.DESC, "createdAt"));
        }
        return Sort.by(orders);
    }
}
