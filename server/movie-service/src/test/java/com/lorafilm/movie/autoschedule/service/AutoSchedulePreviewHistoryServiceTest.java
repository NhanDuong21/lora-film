package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreviewHistoryQuery;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreviewHistoryItemResponse;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewHistoryRow;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.impl.AutoSchedulePreviewHistoryServiceImpl;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePreviewHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-22T10:00:00Z");

    @Mock
    private ShowtimeSchedulePreviewRepository repository;

    private AutoSchedulePreviewHistoryService service;

    @BeforeEach
    void setUp() {
        service = new AutoSchedulePreviewHistoryServiceImpl(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void overduePreview_isDerivedAsExpired_withoutMutatingPersistence() {
        ShowtimeSchedulePreviewHistoryRow row = row(
                SchedulePreviewStatus.PREVIEWED,
                NOW,
                4,
                null
        );
        when(repository.findHistory(any(), any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(row), invocation.getArgument(1, Pageable.class), 1));

        PageResponse<AutoSchedulePreviewHistoryItemResponse> result =
                service.getHistory(new AutoSchedulePreviewHistoryQuery());

        AutoSchedulePreviewHistoryItemResponse item = result.getData().getFirst();
        assertThat(item.getPersistedStatus()).isEqualTo(SchedulePreviewStatus.PREVIEWED);
        assertThat(item.getDisplayStatus()).isEqualTo(SchedulePreviewStatus.EXPIRED);
        assertThat(item.isEditable()).isFalse();
        assertThat(item.isApplicable()).isFalse();
        assertThat(item.getAppliedShowtimeCount()).isNull();
        verify(repository, never()).save(any());
    }

    @Test
    void activePreview_isEditableAndApplicable_whenCandidatesAreSelected() {
        ShowtimeSchedulePreviewHistoryRow row = row(
                SchedulePreviewStatus.PREVIEWED,
                NOW.plusSeconds(60),
                3,
                null
        );
        when(repository.findHistory(any(), any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(row), invocation.getArgument(1, Pageable.class), 1));

        AutoSchedulePreviewHistoryItemResponse item = service
                .getHistory(new AutoSchedulePreviewHistoryQuery())
                .getData()
                .getFirst();

        assertThat(item.getDisplayStatus()).isEqualTo(SchedulePreviewStatus.PREVIEWED);
        assertThat(item.isEditable()).isTrue();
        assertThat(item.isApplicable()).isTrue();
    }

    @Test
    void appliedAndFailedRows_exposeOnlyDerivedSafeSummaryValues() {
        ShowtimeSchedulePreviewHistoryRow applied = row(
                SchedulePreviewStatus.APPLIED,
                NOW.minusSeconds(60),
                5,
                NOW.minusSeconds(30)
        );
        ShowtimeSchedulePreviewHistoryRow failed = row(
                SchedulePreviewStatus.FAILED,
                NOW.minusSeconds(60),
                0,
                null
        );
        when(repository.findHistory(any(), any())).thenAnswer(invocation ->
                new PageImpl<>(List.of(applied, failed), invocation.getArgument(1, Pageable.class), 2));

        List<AutoSchedulePreviewHistoryItemResponse> items = service
                .getHistory(new AutoSchedulePreviewHistoryQuery())
                .getData();

        assertThat(items.get(0).getAppliedShowtimeCount()).isEqualTo(5);
        assertThat(items.get(0).getFailureReasonSafe()).isNull();
        assertThat(items.get(1).getAppliedShowtimeCount()).isNull();
        assertThat(items.get(1).getFailureReasonSafe())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_GENERATION_FAILED.getMessage());
    }

    @Test
    void invalidRangesStrategyAndSort_areRejectedBeforeQuerying() {
        AutoSchedulePreviewHistoryQuery scheduleRange = new AutoSchedulePreviewHistoryQuery();
        scheduleRange.setScheduleFrom(LocalDate.of(2026, 7, 23));
        scheduleRange.setScheduleTo(LocalDate.of(2026, 7, 22));

        AutoSchedulePreviewHistoryQuery creationRange = new AutoSchedulePreviewHistoryQuery();
        creationRange.setCreatedFrom(NOW);
        creationRange.setCreatedTo(NOW);

        AutoSchedulePreviewHistoryQuery strategy = new AutoSchedulePreviewHistoryQuery();
        strategy.setStrategyVersion("BALANCED_V9");

        AutoSchedulePreviewHistoryQuery sort = new AutoSchedulePreviewHistoryQuery();
        sort.setSort("id,desc");

        AutoSchedulePreviewHistoryQuery direction = new AutoSchedulePreviewHistoryQuery();
        direction.setSort("createdAt,sideways");

        assertValidation(scheduleRange);
        assertValidation(creationRange);
        assertValidation(strategy);
        assertValidation(sort);
        assertValidation(direction);
        verify(repository, never()).findHistory(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BALANCED_V1", "BALANCED_V1_S2", "BALANCED_V1_S3"})
    void allPersistedStrategyVersions_areAccepted(String strategyVersion) {
        AutoSchedulePreviewHistoryQuery query = new AutoSchedulePreviewHistoryQuery();
        query.setStrategyVersion(strategyVersion);
        when(repository.findHistory(any(), any())).thenAnswer(invocation ->
                org.springframework.data.domain.Page.empty(invocation.getArgument(1, Pageable.class)));

        PageResponse<AutoSchedulePreviewHistoryItemResponse> result = service.getHistory(query);

        assertThat(result.getData()).isEmpty();
        verify(repository).findHistory(any(), any());
    }

    private void assertValidation(AutoSchedulePreviewHistoryQuery query) {
        assertThatThrownBy(() -> service.getHistory(query))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private ShowtimeSchedulePreviewHistoryRow row(
            SchedulePreviewStatus status,
            Instant expiresAt,
            int selectedCount,
            Instant appliedAt
    ) {
        return new ShowtimeSchedulePreviewHistoryRow(
                "preview-1",
                7L,
                "cinema-1",
                "LoraFilm Quận 1",
                "Asia/Ho_Chi_Minh",
                LocalDate.of(2026, 7, 23),
                LocalDate.of(2026, 7, 25),
                "BALANCED_V1_S3",
                SchedulePreviewApplyMode.ALL_OR_NOTHING,
                status,
                10,
                8,
                2,
                selectedCount,
                NOW.minusSeconds(3600),
                expiresAt,
                appliedAt
        );
    }
}
