package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewPageResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemSpecification;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShowtimeSchedulePreviewServiceImpl implements ShowtimeSchedulePreviewService {

    private static final Logger log = LoggerFactory.getLogger(ShowtimeSchedulePreviewServiceImpl.class);

    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final ShowtimeSchedulePreviewItemRepository itemRepository;
    private final ShowtimeSchedulePreviewMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final ShowtimeSchedulePreviewExpiryService expiryService;
    private final Clock clock;
    private final EntityManager entityManager;

    public ShowtimeSchedulePreviewServiceImpl(
            ShowtimeSchedulePreviewRepository previewRepository,
            ShowtimeSchedulePreviewItemRepository itemRepository,
            ShowtimeSchedulePreviewMapper mapper,
            CurrentUserProvider currentUserProvider,
            ShowtimeSchedulePreviewExpiryService expiryService,
            Clock clock,
            EntityManager entityManager) {
        this.previewRepository = previewRepository;
        this.itemRepository = itemRepository;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
        this.expiryService = expiryService;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeSchedulePreviewPageResponse getPreview(String previewPublicId, ShowtimeSchedulePreviewItemQuery query) {
        log.info("Auto schedule preview retrieved. publicId={}", previewPublicId);
        
        Instant now = Instant.now(clock);
        expiryService.expireIfNecessary(previewPublicId, now);

        ShowtimeSchedulePreview preview = previewRepository.findByPublicId(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));

        Specification<ShowtimeSchedulePreviewItem> spec = ShowtimeSchedulePreviewItemSpecification.filterBy(
                preview.getId(),
                query,
                preview.getTimezoneSnapshot()
        );

        Sort sort = parseSort(query.getSort());
        PageRequest pageRequest = PageRequest.of(query.getPage(), query.getSize(), sort);

        Page<ShowtimeSchedulePreviewItem> itemsPage = itemRepository.findAll(spec, pageRequest);

        return mapper.toPageResponse(preview, itemsPage);
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "rankingPosition", "id");
        }

        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        List<String> allowedProperties = Arrays.asList("rankingPosition", "startTime", "endTime", "score", "createdAt");
        if (!allowedProperties.contains(property)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported sort property: " + property);
        }

        if ("startTime".equals(property)) {
            return Sort.by(
                    new Sort.Order(direction, "startTime"),
                    new Sort.Order(Sort.Direction.ASC, "rankingPosition"),
                    new Sort.Order(Sort.Direction.ASC, "id")
            );
        }

        return Sort.by(
                new Sort.Order(direction, property),
                new Sort.Order(Sort.Direction.ASC, "id")
        );
    }

    @Override
    @Transactional
    public ShowtimeSchedulePreviewSummaryResponse updateSelections(String previewPublicId, UpdatePreviewItemSelectionsRequest request) {
        log.info("Auto schedule selection update requested. previewPublicId={}", previewPublicId);
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_AVAILABLE);
        }

        Instant now = Instant.now(clock);
        expiryService.expireIfNecessary(previewPublicId, now);

        ShowtimeSchedulePreview preview = previewRepository.findByPublicId(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));

        if (preview.getStatus() == SchedulePreviewStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
        }
        if (preview.getStatus() != SchedulePreviewStatus.PREVIEWED) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_EDITABLE);
        }

        if (!Objects.equals(preview.getVersion(), request.getExpectedVersion())) {
            log.warn("Optimistic version conflict. publicId={}, current={}, expected={}", previewPublicId, preview.getVersion(), request.getExpectedVersion());
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
        }

        Set<String> uniqueItemIds = new HashSet<>();
        for (UpdatePreviewItemSelectionRequest itemReq : request.getItems()) {
            if (!uniqueItemIds.add(itemReq.getItemPublicId())) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_DUPLICATE_ITEM_SELECTION);
            }
        }

        List<ShowtimeSchedulePreviewItem> existingItems = itemRepository.findAllByPublicIdIn(uniqueItemIds);
        
        if (existingItems.size() != uniqueItemIds.size()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_ITEM_NOT_FOUND);
        }

        Map<String, ShowtimeSchedulePreviewItem> itemMap = new HashMap<>();
        for (ShowtimeSchedulePreviewItem item : existingItems) {
            if (!Objects.equals(item.getPreview().getId(), preview.getId())) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_ITEM_NOT_BELONG_TO_PREVIEW);
            }
            itemMap.put(item.getPublicId(), item);
        }

        int changedItemCount = 0;
        for (UpdatePreviewItemSelectionRequest itemReq : request.getItems()) {
            ShowtimeSchedulePreviewItem item = itemMap.get(itemReq.getItemPublicId());
            
            if (item.getValidationStatus() == PreviewItemValidationStatus.REJECTED && Boolean.TRUE.equals(itemReq.getSelected())) {
                log.warn("Auto schedule selection rejected. Item {} is REJECTED but requested as selected.", item.getPublicId());
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_REJECTED_ITEM_CANNOT_BE_SELECTED);
            }

            if (!Objects.equals(item.getSelected(), itemReq.getSelected())) {
                item.setSelected(itemReq.getSelected());
                item.setSelectedAt(now);
                item.setSelectedBy(currentUserId);
                changedItemCount++;
            }
        }

        if (changedItemCount > 0) {
            long newSelectedCount = itemRepository.countByPreviewIdAndSelectedTrueAndValidationStatus(
                    preview.getId(), PreviewItemValidationStatus.VALID);
            preview.setSelectedCandidateCount(Math.toIntExact(newSelectedCount));
            
            // Explicitly force a flush to capture optimistic lock exceptions if any, before reloading detailed items.
            // Also, we must increment version so a swap (A=true->false, B=false->true) triggers a version bump
            entityManager.lock(preview, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            previewRepository.saveAndFlush(preview);
            entityManager.refresh(preview);
            log.info("Auto schedule selection updated. previewPublicId={}, changedItemCount={}, newSelectedCount={}", previewPublicId, changedItemCount, newSelectedCount);
        }

        return mapper.toSummaryResponse(preview);
    }

}
