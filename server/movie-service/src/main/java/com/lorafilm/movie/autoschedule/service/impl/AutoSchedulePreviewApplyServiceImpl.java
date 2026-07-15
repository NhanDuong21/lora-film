package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.ApplyShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.mapper.AutoScheduleApplyResponseMapper;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleApplyRevalidationService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleAuditoriumLockService;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewApplyService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleShowtimeCreationService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AutoSchedulePreviewApplyServiceImpl implements AutoSchedulePreviewApplyService {

    private final CurrentUserProvider currentUserProvider;
    private final ShowtimeSchedulePreviewExpiryService expiryService;
    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final ShowtimeSchedulePreviewItemRepository itemRepository;
    private final AutoScheduleAuditoriumLockService auditoriumLockService;
    private final AutoScheduleApplyRevalidationService revalidationService;
    private final AutoScheduleShowtimeCreationService showtimeCreationService;
    private final AutoScheduleApplyResponseMapper responseMapper;
    private final Clock clock;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    public AutoSchedulePreviewApplyServiceImpl(CurrentUserProvider currentUserProvider,
                                               ShowtimeSchedulePreviewExpiryService expiryService,
                                               ShowtimeSchedulePreviewRepository previewRepository,
                                               ShowtimeSchedulePreviewItemRepository itemRepository,
                                               AutoScheduleAuditoriumLockService auditoriumLockService,
                                               AutoScheduleApplyRevalidationService revalidationService,
                                               AutoScheduleShowtimeCreationService showtimeCreationService,
                                               AutoScheduleApplyResponseMapper responseMapper,
                                               Clock clock,
                                               org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
        this.currentUserProvider = currentUserProvider;
        this.expiryService = expiryService;
        this.previewRepository = previewRepository;
        this.itemRepository = itemRepository;
        this.auditoriumLockService = auditoriumLockService;
        this.revalidationService = revalidationService;
        this.showtimeCreationService = showtimeCreationService;
        this.responseMapper = responseMapper;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public ApplyShowtimeSchedulePreviewResponse applyPreview(String previewPublicId, ApplyShowtimeSchedulePreviewRequest request) {
        Long actorId = currentUserProvider.getCurrentUserId();
        if (actorId == null) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_AVAILABLE, "Current user not available");
        }

        String applyKey = request.getIdempotencyKey().trim();
        Instant now = Instant.now(clock);

        // Normalize expiry in a separate transaction
        if (expiryService.expireIfNecessary(previewPublicId, now)) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
        }

        try {
            return transactionTemplate.execute(status -> doApply(previewPublicId, applyKey, request.getExpectedVersion(), actorId, now));
        } catch (DataIntegrityViolationException e) {
            return handleIdempotencyConflict(applyKey, previewPublicId);
        }
    }

    ApplyShowtimeSchedulePreviewResponse doApply(String previewPublicId, String applyKey, Long expectedVersion, Long actorId, Instant now) {
        // Check if key already exists in the same transaction context (idempotency check before doing work)
        Optional<ShowtimeSchedulePreview> existingByKey = previewRepository.findByApplyIdempotencyKeyDetailed(applyKey);
        if (existingByKey.isPresent()) {
            return handleIdempotencyConflict(applyKey, previewPublicId, existingByKey.get());
        }

        // Lock the preview using pessimistic lock to prevent concurrent modifications
        ShowtimeSchedulePreview preview = previewRepository.findByPublicIdForApply(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));

        // Re-check idempotency key and status AFTER acquiring lock (to handle same-key concurrency)
        if (applyKey.equals(preview.getApplyIdempotencyKey())) {
            if (preview.getStatus() == SchedulePreviewStatus.APPLIED) {
                return responseMapper.toResponse(preview);
            }
            if (preview.getStatus() == SchedulePreviewStatus.APPLYING) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS);
            }
        }
        
        if (preview.getApplyIdempotencyKey() != null && !applyKey.equals(preview.getApplyIdempotencyKey())) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_ALREADY_APPLIED);
        }

        validateStateForApply(preview, expectedVersion, now);

        List<ShowtimeSchedulePreviewItem> selectedItems = itemRepository.findSelectedItemsForApply(preview.getId(), PreviewItemValidationStatus.VALID);
        if (selectedItems.isEmpty() || preview.getSelectedCandidateCount() == 0) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_NO_SELECTED_ITEMS, "No selected items to apply");
        }

        if (selectedItems.size() != preview.getSelectedCandidateCount()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT, "Selected item count mismatch");
        }

        List<Long> auditoriumIds = selectedItems.stream()
                .map(i -> i.getAuditorium().getId())
                .collect(Collectors.toList());

        auditoriumLockService.lockAll(auditoriumIds);

        // Revalidate
        revalidationService.validateAll(preview, selectedItems, now);

        // State changes
        preview.markApplying(applyKey);

        // Create showtimes
        List<Showtime> createdShowtimes = showtimeCreationService.createAll(selectedItems, actorId);

        // Update items
        for (int i = 0; i < selectedItems.size(); i++) {
            ShowtimeSchedulePreviewItem item = selectedItems.get(i);
            Showtime showtime = createdShowtimes.get(i);
            item.setApplyStatus(PreviewItemApplyStatus.CREATED);
            item.setCreatedShowtime(showtime);
            item.setApplyErrorCode(null);
            item.setApplyErrorMessage(null);
        }

        // Unselected / rejected items -> SKIPPED
        List<ShowtimeSchedulePreviewItem> allItems = itemRepository.findDetailedItemsByPreviewId(preview.getId());
        for (ShowtimeSchedulePreviewItem item : allItems) {
            if (item.getApplyStatus() == PreviewItemApplyStatus.PENDING) {
                item.setApplyStatus(PreviewItemApplyStatus.SKIPPED);
            }
        }

        preview.markApplied(actorId, now);
        
        return responseMapper.toResponse(preview);
    }

    private ApplyShowtimeSchedulePreviewResponse handleIdempotencyConflict(String applyKey, String previewPublicId) {
        ShowtimeSchedulePreview preview = previewRepository.findByApplyIdempotencyKeyDetailed(applyKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency key reused but not found?"));
        return handleIdempotencyConflict(applyKey, previewPublicId, preview);
    }

    private ApplyShowtimeSchedulePreviewResponse handleIdempotencyConflict(String applyKey, String previewPublicId, ShowtimeSchedulePreview preview) {
        if (!preview.getPublicId().equals(previewPublicId)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency key was reused with a different preview");
        }

        if (preview.getStatus() == SchedulePreviewStatus.APPLIED) {
            return responseMapper.toResponse(preview);
        } else if (preview.getStatus() == SchedulePreviewStatus.APPLYING) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS);
        } else {
            // It could be FAILED or CANCELLED with that key. If it's failed, maybe the key is stuck.
            // But requirement says: FAILED/CANCELLED/EXPIRED -> không tự replay thành success.
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
        }
    }

    private void validateStateForApply(ShowtimeSchedulePreview preview, Long expectedVersion, Instant now) {
        if (!preview.getVersion().equals(expectedVersion)) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
        }

        if (preview.getApplyMode() != SchedulePreviewApplyMode.ALL_OR_NOTHING) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
        }

        switch (preview.getStatus()) {
            case PREVIEWED:
                if (!now.isBefore(preview.getExpiresAt())) {
                    throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
                }
                break;
            case GENERATING:
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
            case APPLYING:
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS);
            case APPLIED:
                // If it was applied, and we reached here (didn't match idempotency key earlier)
                // it means it was applied with a different key.
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_ALREADY_APPLIED);
            case EXPIRED:
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
            case FAILED:
            case CANCELLED:
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
        }
    }
}
