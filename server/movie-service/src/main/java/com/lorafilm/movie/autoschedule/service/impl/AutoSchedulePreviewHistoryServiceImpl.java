package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreviewHistoryQuery;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreviewHistoryItemResponse;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewHistoryRow;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewHistoryService;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AutoSchedulePreviewHistoryServiceImpl implements AutoSchedulePreviewHistoryService {

    private static final Set<String> STRATEGY_VERSIONS = Set.of(
            AutoScheduleStrategyVersions.LEGACY_BALANCED_V1,
            AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S2,
            AutoScheduleStrategyVersions.CURRENT
    );

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt",
            "scheduleFrom",
            "scheduleTo",
            "status",
            "cinemaName",
            "totalCandidateCount",
            "selectedCandidateCount",
            "appliedAt"
    );

    private final ShowtimeSchedulePreviewRepository repository;
    private final Clock clock;

    public AutoSchedulePreviewHistoryServiceImpl(
            ShowtimeSchedulePreviewRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AutoSchedulePreviewHistoryItemResponse> getHistory(
            AutoSchedulePreviewHistoryQuery query
    ) {
        Sort sort = validateAndResolveSort(query);
        PageRequest pageable = PageRequest.of(query.getPage(), query.getSize(), sort);
        Instant now = Instant.now(clock);
        Page<ShowtimeSchedulePreviewHistoryRow> page = repository.findHistory(query, pageable);
        List<AutoSchedulePreviewHistoryItemResponse> content = page.getContent().stream()
                .map(row -> toResponse(row, now))
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    private Sort validateAndResolveSort(AutoSchedulePreviewHistoryQuery query) {
        if (query.getScheduleFrom() != null
                && query.getScheduleTo() != null
                && query.getScheduleFrom().isAfter(query.getScheduleTo())) {
            throw validation("scheduleFrom must not be after scheduleTo");
        }
        if (query.getCreatedFrom() != null
                && query.getCreatedTo() != null
                && !query.getCreatedFrom().isBefore(query.getCreatedTo())) {
            throw validation("createdFrom must be before createdTo");
        }
        if (hasText(query.getStrategyVersion())
                && !STRATEGY_VERSIONS.contains(query.getStrategyVersion().trim())) {
            throw validation("Unsupported strategyVersion: " + query.getStrategyVersion().trim());
        }

        String rawSort = hasText(query.getSort()) ? query.getSort().trim() : "createdAt,desc";
        String[] parts = rawSort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])) {
            throw validation("Unsupported history sort: " + rawSort);
        }

        String directionValue = parts[1].toLowerCase(Locale.ROOT);
        if (!directionValue.equals("asc") && !directionValue.equals("desc")) {
            throw validation("Unsupported history sort direction: " + parts[1]);
        }

        Sort.Direction direction = directionValue.equals("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }

    private AutoSchedulePreviewHistoryItemResponse toResponse(
            ShowtimeSchedulePreviewHistoryRow row,
            Instant now
    ) {
        SchedulePreviewStatus displayStatus = row.persistedStatus();
        if (row.persistedStatus() == SchedulePreviewStatus.PREVIEWED
                && !now.isBefore(row.expiresAt())) {
            displayStatus = SchedulePreviewStatus.EXPIRED;
        }

        boolean editable = row.persistedStatus() == SchedulePreviewStatus.PREVIEWED
                && now.isBefore(row.expiresAt());
        boolean applicable = editable
                && row.applyMode() == SchedulePreviewApplyMode.ALL_OR_NOTHING
                && row.selectedCandidateCount() != null
                && row.selectedCandidateCount() > 0;

        AutoSchedulePreviewHistoryItemResponse response = new AutoSchedulePreviewHistoryItemResponse();
        response.setPreviewPublicId(row.previewPublicId());
        response.setVersion(row.version());
        response.setCinemaPublicId(row.cinemaPublicId());
        response.setCinemaName(row.cinemaName());
        response.setTimezoneSnapshot(row.timezoneSnapshot());
        response.setScheduleFrom(row.scheduleFrom());
        response.setScheduleTo(row.scheduleTo());
        response.setStrategyVersion(row.strategyVersion());
        response.setApplyMode(row.applyMode());
        response.setPersistedStatus(row.persistedStatus());
        response.setDisplayStatus(displayStatus);
        response.setEditable(editable);
        response.setApplicable(applicable);
        response.setTotalCandidateCount(row.totalCandidateCount());
        response.setValidCandidateCount(row.validCandidateCount());
        response.setRejectedCandidateCount(row.rejectedCandidateCount());
        response.setSelectedCandidateCount(row.selectedCandidateCount());
        response.setAppliedShowtimeCount(row.persistedStatus() == SchedulePreviewStatus.APPLIED
                ? row.selectedCandidateCount()
                : null);
        response.setCreatedAt(row.createdAt());
        response.setExpiresAt(row.expiresAt());
        response.setAppliedAt(row.appliedAt());
        response.setFailureReasonSafe(row.persistedStatus() == SchedulePreviewStatus.FAILED
                ? ErrorCode.AUTO_SCHEDULE_GENERATION_FAILED.getMessage()
                : null);
        return response;
    }

    private BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
