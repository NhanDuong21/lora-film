package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;
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
    private final Clock clock;

    public ShowtimeSchedulePreviewServiceImpl(
            ShowtimeSchedulePreviewRepository previewRepository,
            ShowtimeSchedulePreviewItemRepository itemRepository,
            ShowtimeSchedulePreviewMapper mapper,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.previewRepository = previewRepository;
        this.itemRepository = itemRepository;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ShowtimeSchedulePreviewResponse getPreview(String previewPublicId) {
        log.info("Auto schedule preview retrieved. publicId={}", previewPublicId);
        ShowtimeSchedulePreview preview = previewRepository.findByPublicId(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));

        normalizeExpiry(preview, Instant.now(clock));

        List<ShowtimeSchedulePreviewItem> items = itemRepository.findDetailedItemsByPreviewId(preview.getId());
        return mapper.toResponse(preview, items);
    }

    @Override
    @Transactional
    public ShowtimeSchedulePreviewResponse updateSelections(String previewPublicId, UpdatePreviewItemSelectionsRequest request) {
        log.info("Auto schedule selection update requested. previewPublicId={}", previewPublicId);
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_AVAILABLE);
        }

        ShowtimeSchedulePreview preview = previewRepository.findByPublicId(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));

        Instant now = Instant.now(clock);
        normalizeExpiry(preview, now);

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
            previewRepository.flush();
            log.info("Auto schedule selection updated. previewPublicId={}, changedItemCount={}, newSelectedCount={}", previewPublicId, changedItemCount, newSelectedCount);
        }

        List<ShowtimeSchedulePreviewItem> detailedItems = itemRepository.findDetailedItemsByPreviewId(preview.getId());
        return mapper.toResponse(preview, detailedItems);
    }

    private void normalizeExpiry(ShowtimeSchedulePreview preview, Instant now) {
        if (preview.getStatus() == SchedulePreviewStatus.PREVIEWED && !now.isBefore(preview.getExpiresAt())) {
            log.info("Auto schedule preview expired during access. publicId={}", preview.getPublicId());
            preview.markExpired();
        }
    }
}
