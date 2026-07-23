package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
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
import com.lorafilm.movie.autoschedule.repository.SelectionItemSnapshot;
import com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService;
import com.lorafilm.movie.autoschedule.validation.OccupancyInterval;
import com.lorafilm.movie.autoschedule.validation.OccupancyOverlapValidator;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemSpecification;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class ShowtimeSchedulePreviewServiceImpl implements ShowtimeSchedulePreviewService {

    private static final Logger log = LoggerFactory.getLogger(ShowtimeSchedulePreviewServiceImpl.class);
    private static final int MAX_SELECTION_UPDATES = 10_000;

    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final ShowtimeSchedulePreviewItemRepository itemRepository;
    private final ShowtimeSchedulePreviewMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final ShowtimeSchedulePreviewExpiryService expiryService;
    private final Clock clock;

    public ShowtimeSchedulePreviewServiceImpl(
            ShowtimeSchedulePreviewRepository previewRepository,
            ShowtimeSchedulePreviewItemRepository itemRepository,
            ShowtimeSchedulePreviewMapper mapper,
            CurrentUserProvider currentUserProvider,
            ShowtimeSchedulePreviewExpiryService expiryService,
            Clock clock) {
        this.previewRepository = previewRepository;
        this.itemRepository = itemRepository;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
        this.expiryService = expiryService;
        this.clock = clock;
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShowtimeSchedulePreviewSummaryResponse updateSelections(String previewPublicId, UpdatePreviewItemSelectionsRequest request) {
        log.info("Auto schedule selection update requested. previewPublicId={}", previewPublicId);
        Set<String> uniqueItemIds = validateRequestAndCollectIds(request);

        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_AVAILABLE);
        }

        Instant now = Instant.now(clock);
        expiryService.expireIfNecessary(previewPublicId, now);

        ShowtimeSchedulePreview preview = previewRepository.findByPublicIdWithCinema(previewPublicId)
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

        List<SelectionItemSnapshot> requestTargets =
                itemRepository.findSelectionSnapshotsByPublicIdIn(uniqueItemIds);
        if (requestTargets.size() != uniqueItemIds.size()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_ITEM_NOT_FOUND);
        }

        Map<String, SelectionItemSnapshot> targetByPublicId = new HashMap<>();
        for (SelectionItemSnapshot item : requestTargets) {
            if (!Objects.equals(item.previewId(), preview.getId())) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_ITEM_NOT_BELONG_TO_PREVIEW);
            }
            targetByPublicId.put(item.itemPublicId(), item);
        }

        List<SelectionItemSnapshot> currentlySelected =
                itemRepository.findSelectedSelectionSnapshots(preview.getId());
        Map<String, SelectionItemSnapshot> finalSelected = new LinkedHashMap<>();
        for (SelectionItemSnapshot item : currentlySelected) {
            finalSelected.put(item.itemPublicId(), item);
        }

        List<SelectionItemSnapshot> additions = new ArrayList<>();
        for (UpdatePreviewItemSelectionRequest itemReq : request.getItems()) {
            SelectionItemSnapshot item = targetByPublicId.get(itemReq.getItemPublicId());
            if (Boolean.TRUE.equals(itemReq.getSelected())) {
                validateRequestedSelection(item);
                finalSelected.put(item.itemPublicId(), item);
                if (!Boolean.TRUE.equals(item.selected())) {
                    additions.add(item);
                }
            } else {
                finalSelected.remove(item.itemPublicId());
            }
        }

        validateMalformedLegacyRemediation(finalSelected.values(), additions);
        validateFinalOccupancy(finalSelected.values());

        List<Long> changedToSelected = new ArrayList<>();
        List<Long> changedToDeselected = new ArrayList<>();
        for (UpdatePreviewItemSelectionRequest itemReq : request.getItems()) {
            SelectionItemSnapshot item = targetByPublicId.get(itemReq.getItemPublicId());
            if (!Objects.equals(item.selected(), itemReq.getSelected())) {
                if (Boolean.TRUE.equals(itemReq.getSelected())) {
                    changedToSelected.add(item.itemId());
                } else {
                    changedToDeselected.add(item.itemId());
                }
            }
        }

        int changedItemCount = changedToSelected.size() + changedToDeselected.size();
        if (changedItemCount == 0) {
            return mapper.toSummaryResponse(preview);
        }

        int finalSelectedValidCount = Math.toIntExact(finalSelected.values().stream()
                .filter(item -> item.validationStatus() == PreviewItemValidationStatus.VALID)
                .count());

        int previewUpdates = previewRepository.compareAndSetSelectionVersion(
                preview.getId(),
                SchedulePreviewStatus.PREVIEWED,
                request.getExpectedVersion(),
                finalSelectedValidCount,
                now);
        if (previewUpdates != 1) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
        }

        updateSelectionRows(preview.getId(), changedToSelected, false, true, now, currentUserId);
        updateSelectionRows(preview.getId(), changedToDeselected, true, false, now, currentUserId);

        ShowtimeSchedulePreview updatedPreview = previewRepository.findByPublicIdWithCinema(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT));
        log.info("Auto schedule selection updated. previewPublicId={}, changedItemCount={}, newSelectedCount={}",
                previewPublicId, changedItemCount, finalSelectedValidCount);
        return mapper.toSummaryResponse(updatedPreview);
    }

    private Set<String> validateRequestAndCollectIds(UpdatePreviewItemSelectionsRequest request) {
        if (request == null
                || request.getItems() == null
                || request.getItems().isEmpty()
                || request.getItems().size() > MAX_SELECTION_UPDATES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Set<String> uniqueItemIds = new LinkedHashSet<>();
        for (UpdatePreviewItemSelectionRequest item : request.getItems()) {
            if (item == null || !uniqueItemIds.add(item.getItemPublicId())) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_DUPLICATE_ITEM_SELECTION);
            }
        }
        return uniqueItemIds;
    }

    private void validateRequestedSelection(SelectionItemSnapshot item) {
        if (item.validationStatus() == PreviewItemValidationStatus.REJECTED) {
            log.warn("Auto schedule selection rejected. Item {} is REJECTED but requested as selected.",
                    item.itemPublicId());
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_REJECTED_ITEM_CANNOT_BE_SELECTED);
        }
        if (item.validationStatus() != PreviewItemValidationStatus.VALID
                || item.applyStatus() != PreviewItemApplyStatus.PENDING
                || !hasWellFormedInterval(item)) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_ITEM_SELECTION);
        }
    }

    private void validateMalformedLegacyRemediation(Collection<SelectionItemSnapshot> finalSelected,
                                                     List<SelectionItemSnapshot> additions) {
        if (additions.isEmpty()) {
            return;
        }
        for (SelectionItemSnapshot retained : finalSelected) {
            if (hasWellFormedInterval(retained)) {
                continue;
            }
            if (retained.auditoriumId() == null
                    || additions.stream().anyMatch(addition ->
                    Objects.equals(addition.auditoriumId(), retained.auditoriumId()))) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_ITEM_SELECTION);
            }
        }
    }

    private void validateFinalOccupancy(Collection<SelectionItemSnapshot> finalSelected) {
        List<OccupancyInterval> intervals = finalSelected.stream()
                .filter(this::hasWellFormedInterval)
                .map(item -> new OccupancyInterval(
                        item.auditoriumId(),
                        item.startTime(),
                        item.occupancyEndTime(),
                        item.itemPublicId()))
                .toList();
        if (OccupancyOverlapValidator.findConflict(intervals).isPresent()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_SELECTION_OVERLAP);
        }
    }

    private boolean hasWellFormedInterval(SelectionItemSnapshot item) {
        return item.itemPublicId() != null
                && !item.itemPublicId().isBlank()
                && item.auditoriumId() != null
                && item.startTime() != null
                && item.endTime() != null
                && item.occupancyEndTime() != null
                && item.startTime().isBefore(item.endTime())
                && !item.endTime().isAfter(item.occupancyEndTime());
    }

    private void updateSelectionRows(Long previewId,
                                     List<Long> itemIds,
                                     boolean expectedSelected,
                                     boolean selected,
                                     Instant selectedAt,
                                     Long selectedBy) {
        if (itemIds.isEmpty()) {
            return;
        }
        int affectedRows = itemRepository.updateSelectionState(
                previewId, itemIds, expectedSelected, selected, selectedAt, selectedBy);
        if (affectedRows != itemIds.size()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT);
        }
    }

}
