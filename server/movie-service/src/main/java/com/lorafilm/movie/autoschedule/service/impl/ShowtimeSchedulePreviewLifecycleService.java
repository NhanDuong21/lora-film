package com.lorafilm.movie.autoschedule.service.impl;

import java.time.Clock;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewPersistenceMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShowtimeSchedulePreviewLifecycleService {

    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final ShowtimeSchedulePreviewItemRepository itemRepository;
    private final ShowtimeSchedulePreviewPersistenceMapper persistenceMapper;
    private final Clock clock;

    public ShowtimeSchedulePreviewLifecycleService(ShowtimeSchedulePreviewRepository previewRepository,
                                                   ShowtimeSchedulePreviewItemRepository itemRepository,
                                                   ShowtimeSchedulePreviewPersistenceMapper persistenceMapper,
                                                   Clock clock) {
        this.previewRepository = previewRepository;
        this.itemRepository = itemRepository;
        this.persistenceMapper = persistenceMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShowtimeSchedulePreview createGeneratingPreview(NormalizedGeneratePreviewRequest request,
                                                           Cinema cinema,
                                                           String fingerprint,
                                                           Long adminUserId) {
        Instant now = Instant.now(clock);
        
        ShowtimeSchedulePreview preview = ShowtimeSchedulePreview.createGenerating(
                cinema,
                request.getScheduleFrom(),
                request.getScheduleTo(),
                request.getSlotGranularityMinutes(),
                request.getPreviewTtlMinutes(),
                request.getIdempotencyKey(),
                fingerprint,
                adminUserId,
                now
        );

        return previewRepository.saveAndFlush(preview);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPreviewFailed(Long previewId, String failureReason) {
        previewRepository.findById(previewId).ifPresent(preview -> {
            String sanitizedReason = failureReason != null && failureReason.length() > MAX_FAILURE_REASON_LENGTH ?
                    failureReason.substring(0, MAX_FAILURE_REASON_LENGTH - 3) + "..." : failureReason;
            preview.markFailed(sanitizedReason);
            previewRepository.saveAndFlush(preview);
        });
    }

    @Transactional
    public void persistGeneratedItemsAndMarkPreviewed(ShowtimeSchedulePreview preview, List<ShowtimeCandidate> candidates) {
        int total = candidates.size();
        int valid = 0;
        int rejected = 0;
        int selected = 0;

        List<ShowtimeSchedulePreviewItem> items = candidates.stream()
                .map(c -> persistenceMapper.toEntity(c, preview))
                .collect(Collectors.toList());

        for (ShowtimeSchedulePreviewItem item : items) {
            if (item.getValidationStatus() == PreviewItemValidationStatus.VALID) {
                valid++;
            } else {
                rejected++;
            }
            if (item.getSelected()) {
                selected++;
            }
        }

        itemRepository.saveAll(items); // Batch save depends on Hibernate configuration

        preview.setStatus(SchedulePreviewStatus.PREVIEWED);
        preview.setTotalCandidateCount(total);
        preview.setValidCandidateCount(valid);
        preview.setRejectedCandidateCount(rejected);
        preview.setSelectedCandidateCount(selected);

        previewRepository.save(preview);
    }
}
