package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeMapper;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import com.lorafilm.movie.showtime.service.ShowtimeRefundOutboxService;
import com.lorafilm.movie.showtime.validation.ShowtimeOpeningPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeStatusTransitionServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private ShowtimeStatusHistoryService historyService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AdminShowtimeMapper adminShowtimeMapper;

    @Mock
    private ShowtimeRefundOutboxService refundOutboxService;
    @Mock
    private ShowtimeOpeningPolicy openingPolicy;
    @Mock
    private ShowtimeSchedulePreviewRepository schedulePreviewRepository;

    private Clock fixedClock;

    private ShowtimeStatusTransitionServiceImpl transitionService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneId.of("UTC"));
        transitionService = new ShowtimeStatusTransitionServiceImpl(
                showtimeRepository, historyService, currentUserProvider,
                adminShowtimeMapper, fixedClock, refundOutboxService, openingPolicy,
                schedulePreviewRepository);
    }

    @Test
    void transitionStatus_DraftToOpen_Success() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        Showtime showtime = new Showtime();
        showtime.setStatus(ShowtimeStatus.DRAFT);
        showtime.setStartTime(Instant.parse("2026-07-10T12:00:00Z")); // future
        showtime.setEndTime(Instant.parse("2026-07-10T14:00:00Z"));

        when(showtimeRepository.findByPublicIdForUpdate("pub-id")).thenReturn(Optional.of(showtime));
        when(showtimeRepository.saveAndFlush(any(Showtime.class))).thenAnswer(i -> i.getArgument(0));
        when(adminShowtimeMapper.toAdminResponse(any(Showtime.class))).thenReturn(new AdminShowtimeResponse());

        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);

        transitionService.transitionStatus("pub-id", request);

        assertEquals(ShowtimeStatus.OPEN_FOR_BOOKING, showtime.getStatus());
        assertEquals(Instant.parse("2026-07-10T10:00:00Z"), showtime.getBookingOpenTime());
        verify(openingPolicy).validateCanOpen(showtime, fixedClock.instant());
        verify(historyService).recordTransitionHistory(eq(showtime), eq(ShowtimeStatus.DRAFT), eq(ShowtimeStatus.OPEN_FOR_BOOKING), isNull(), eq(1L), eq(fixedClock.instant()));
    }

    @Test
    void transitionStatus_DraftToOpen_FailsIfAlreadyStarted() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        Showtime showtime = new Showtime();
        showtime.setStatus(ShowtimeStatus.DRAFT);
        showtime.setStartTime(Instant.parse("2026-07-10T09:00:00Z")); // past

        when(showtimeRepository.findByPublicIdForUpdate("pub-id")).thenReturn(Optional.of(showtime));
        doThrow(new BusinessException(ErrorCode.SHOWTIME_CANNOT_OPEN_AFTER_START))
                .when(openingPolicy).validateCanOpen(showtime, fixedClock.instant());

        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);

        BusinessException ex = assertThrows(BusinessException.class, () -> transitionService.transitionStatus("pub-id", request));
        assertEquals(ErrorCode.SHOWTIME_CANNOT_OPEN_AFTER_START, ex.getErrorCode());
    }

    @Test
    void transitionStatus_DraftToClosed_FailsInvalidTransition() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        Showtime showtime = new Showtime();
        showtime.setStatus(ShowtimeStatus.DRAFT);

        when(showtimeRepository.findByPublicIdForUpdate("pub-id")).thenReturn(Optional.of(showtime));

        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.CLOSED);

        BusinessException ex = assertThrows(BusinessException.class, () -> transitionService.transitionStatus("pub-id", request));
        assertEquals(ErrorCode.INVALID_SHOWTIME_STATUS_TRANSITION, ex.getErrorCode());
    }

    @Test
    void transitionStatus_ToCancelled_RequiresReason() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        Showtime showtime = new Showtime();
        showtime.setStatus(ShowtimeStatus.DRAFT);

        when(showtimeRepository.findByPublicIdForUpdate("pub-id")).thenReturn(Optional.of(showtime));

        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.CANCELLED);
        request.setReason("   ");

        BusinessException ex = assertThrows(BusinessException.class, () -> transitionService.transitionStatus("pub-id", request));
        assertEquals(ErrorCode.SHOWTIME_CANCELLATION_REASON_REQUIRED, ex.getErrorCode());
    }

    @Test
    void transitionStatus_ToCancelled_SuccessWithReason() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        Showtime showtime = new Showtime();
        showtime.setPublicId("pub-id");
        showtime.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        showtime.setBookingOpenTime(Instant.parse("2026-07-10T09:00:00Z"));

        when(showtimeRepository.findByPublicIdForUpdate("pub-id")).thenReturn(Optional.of(showtime));
        when(showtimeRepository.saveAndFlush(any(Showtime.class))).thenAnswer(i -> i.getArgument(0));

        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.CANCELLED);
        request.setReason("Technical issue");

        transitionService.transitionStatus("pub-id", request);

        assertEquals(ShowtimeStatus.CANCELLED, showtime.getStatus());
        assertEquals("Technical issue", showtime.getCancellationReason());
        assertEquals(Instant.parse("2026-07-10T10:00:00Z"), showtime.getBookingCloseTime());
        verify(refundOutboxService).enqueueCancellation("pub-id", "Technical issue");
    }

    @Test
    void transitionStatus_ClosedToFinished_SuccessIfEnded() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        Showtime showtime = new Showtime();
        showtime.setStatus(ShowtimeStatus.CLOSED);
        showtime.setEndTime(Instant.parse("2026-07-10T09:00:00Z")); // past

        when(showtimeRepository.findByPublicIdForUpdate("pub-id")).thenReturn(Optional.of(showtime));
        when(showtimeRepository.saveAndFlush(any(Showtime.class))).thenAnswer(i -> i.getArgument(0));

        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.FINISHED);

        transitionService.transitionStatus("pub-id", request);

        assertEquals(ShowtimeStatus.FINISHED, showtime.getStatus());
    }

    @Test
    void transitionStatus_ClosedToFinished_FailsIfNotEnded() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        Showtime showtime = new Showtime();
        showtime.setStatus(ShowtimeStatus.CLOSED);
        showtime.setEndTime(Instant.parse("2026-07-10T12:00:00Z")); // future

        when(showtimeRepository.findByPublicIdForUpdate("pub-id")).thenReturn(Optional.of(showtime));

        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.FINISHED);

        BusinessException ex = assertThrows(BusinessException.class, () -> transitionService.transitionStatus("pub-id", request));
        assertEquals(ErrorCode.SHOWTIME_CANNOT_FINISH_BEFORE_END, ex.getErrorCode());
    }

    @Test
    void previewBatchStatus_GroupsBlockedItemsAndTreatsAlreadyOpenAsNoOp() {
        Showtime eligible = showtime("eligible", ShowtimeStatus.DRAFT, "2026-07-10T12:00:00Z");
        Showtime missingPrice = showtime("missing", ShowtimeStatus.DRAFT, "2026-07-10T13:00:00Z");
        Showtime alreadyOpen = showtime("open", ShowtimeStatus.OPEN_FOR_BOOKING, "2026-07-10T14:00:00Z");
        when(showtimeRepository.findAllByBatchIdAndDeletedAtIsNullOrderByIdAsc("batch-1"))
                .thenReturn(List.of(eligible, missingPrice, alreadyOpen));
        doAnswer(invocation -> {
            if (invocation.getArgument(0) == missingPrice) {
                throw new BusinessException(ErrorCode.SHOWTIME_PRICE_MISSING, "missing");
            }
            return null;
        }).when(openingPolicy).validateCanOpen(any(Showtime.class), eq(fixedClock.instant()));
        when(currentUserProvider.getCurrentUserId()).thenReturn(7L);

        var summary = transitionService.previewBatchStatus(
                "batch-1", ShowtimeStatus.OPEN_FOR_BOOKING);

        assertEquals(3, summary.getTotalCount());
        assertEquals(1, summary.getEligibleCount());
        assertEquals(1, summary.getAlreadyTargetCount());
        assertEquals(1, summary.getSkippedCount());
        assertFalse(summary.isActionAllowed());
        assertTrue(summary.isAtomic());
        assertEquals("SHOWTIME_PRICE_MISSING", summary.getReasonGroups().get(0).getReasonCode());
        assertEquals("Showtime price config is missing", summary.getReasonGroups().get(0).getReason());
        assertEquals(List.of("missing"), summary.getReasonGroups().get(0).getSampleShowtimePublicIds());
        assertEquals(1, summary.getBlockedShowtimes().size());
        assertEquals("missing", summary.getBlockedShowtimes().get(0).getShowtimePublicId());
        assertEquals("SHOWTIME_PRICE_MISSING", summary.getBlockedShowtimes().get(0).getReasonCode());
        assertEquals("Showtime price config is missing", summary.getBlockedShowtimes().get(0).getReason());
        assertEquals(7L, summary.getActorId());
    }

    @Test
    void previewBatchStatus_GroupsEveryBlockedShowtimeByCanonicalReason() {
        Showtime firstMissing = showtime("missing-1", ShowtimeStatus.DRAFT, "2026-07-10T12:00:00Z");
        Showtime secondMissing = showtime("missing-2", ShowtimeStatus.DRAFT, "2026-07-10T13:00:00Z");
        Showtime invalidStatus = showtime("closed", ShowtimeStatus.CLOSED, "2026-07-10T14:00:00Z");
        when(showtimeRepository.findAllByBatchIdAndDeletedAtIsNullOrderByIdAsc("batch-1"))
                .thenReturn(List.of(firstMissing, secondMissing, invalidStatus));
        doThrow(new BusinessException(ErrorCode.PRICING_INCOMPLETE, "diagnostic details"))
                .when(openingPolicy).validateCanOpen(any(Showtime.class), eq(fixedClock.instant()));

        var summary = transitionService.previewBatchStatus(
                "batch-1", ShowtimeStatus.OPEN_FOR_BOOKING);

        assertEquals(3, summary.getSkippedCount());
        assertFalse(summary.isActionAllowed());
        assertEquals(2, summary.getReasonGroups().size());
        assertEquals("PRICING_INCOMPLETE", summary.getReasonGroups().get(0).getReasonCode());
        assertEquals(2, summary.getReasonGroups().get(0).getCount());
        assertEquals(
                List.of("missing-1", "missing-2"),
                summary.getReasonGroups().get(0).getSampleShowtimePublicIds());
        assertEquals("INVALID_SHOWTIME_STATUS_TRANSITION", summary.getReasonGroups().get(1).getReasonCode());
        assertEquals(3, summary.getBlockedShowtimes().size());
        assertEquals(
                List.of("missing-1", "missing-2", "closed"),
                summary.getBlockedShowtimes().stream()
                        .map(blocked -> blocked.getShowtimePublicId())
                        .toList());
        assertTrue(summary.getBlockedShowtimes().stream()
                .allMatch(blocked -> blocked.getReasonCode() != null && blocked.getReason() != null));
    }

    @Test
    void transitionBatchStatus_BlockedItemPreventsPartialOpen() {
        Showtime eligible = showtime("eligible", ShowtimeStatus.DRAFT, "2026-07-10T12:00:00Z");
        Showtime started = showtime("started", ShowtimeStatus.DRAFT, "2026-07-10T09:00:00Z");
        when(showtimeRepository.findAllByBatchIdForUpdate("batch-1"))
                .thenReturn(List.of(eligible, started));
        doAnswer(invocation -> {
            if (invocation.getArgument(0) == started) {
                throw new BusinessException(ErrorCode.SHOWTIME_CANNOT_OPEN_AFTER_START);
            }
            return null;
        }).when(openingPolicy).validateCanOpen(any(Showtime.class), eq(fixedClock.instant()));
        when(currentUserProvider.getCurrentUserId()).thenReturn(9L);
        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);

        var summary = transitionService.transitionBatchStatus("batch-1", request);

        assertFalse(summary.isActionAllowed());
        assertEquals(0, summary.getAffectedCount());
        assertEquals(ShowtimeStatus.DRAFT, eligible.getStatus());
        verify(showtimeRepository, never()).saveAndFlush(any());
        verify(historyService, never()).recordTransitionHistory(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void transitionBatchStatus_UsesSharedActorAndTimeForEligibleItems() {
        Showtime first = showtime("first", ShowtimeStatus.DRAFT, "2026-07-10T12:00:00Z");
        Showtime second = showtime("second", ShowtimeStatus.DRAFT, "2026-07-10T13:00:00Z");
        Showtime alreadyOpen = showtime("open", ShowtimeStatus.OPEN_FOR_BOOKING, "2026-07-10T14:00:00Z");
        when(showtimeRepository.findAllByBatchIdForUpdate("batch-1"))
                .thenReturn(List.of(first, second, alreadyOpen));
        when(currentUserProvider.getCurrentUserId()).thenReturn(11L);
        when(showtimeRepository.saveAndFlush(any(Showtime.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);

        var summary = transitionService.transitionBatchStatus("batch-1", request);

        assertTrue(summary.isActionAllowed());
        assertEquals(2, summary.getAffectedCount());
        assertEquals(1, summary.getAlreadyTargetCount());
        assertEquals(ShowtimeStatus.OPEN_FOR_BOOKING, first.getStatus());
        assertEquals(ShowtimeStatus.OPEN_FOR_BOOKING, second.getStatus());
        assertEquals(fixedClock.instant(), first.getBookingOpenTime());
        assertEquals(fixedClock.instant(), second.getBookingOpenTime());
        assertEquals(11L, summary.getActorId());
        assertEquals(fixedClock.instant(), summary.getActionAt());
        verify(historyService, times(2)).recordTransitionHistory(
                any(), eq(ShowtimeStatus.DRAFT), eq(ShowtimeStatus.OPEN_FOR_BOOKING),
                isNull(), eq(11L), eq(fixedClock.instant()));
    }

    @Test
    void transitionBatchStatus_CancelsAllAutomaticDraftsWithoutRefundWork() {
        ShowtimeSchedulePreview sourcePreview = mock(ShowtimeSchedulePreview.class);
        when(sourcePreview.getStatus()).thenReturn(SchedulePreviewStatus.APPLIED);
        when(schedulePreviewRepository.findByPublicIdForApply("batch-1"))
                .thenReturn(Optional.of(sourcePreview));
        Showtime first = showtime("first", ShowtimeStatus.DRAFT, "2026-07-10T12:00:00Z");
        Showtime second = showtime("second", ShowtimeStatus.DRAFT, "2026-07-10T14:00:00Z");
        when(showtimeRepository.findAllByBatchIdForUpdate("batch-1"))
                .thenReturn(List.of(first, second));
        when(showtimeRepository.saveAndFlush(any(Showtime.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProvider.getCurrentUserId()).thenReturn(15L);
        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.CANCELLED);
        request.setReason("Replace automatic schedule");

        var summary = transitionService.transitionBatchStatus("batch-1", request);

        assertTrue(summary.isActionAllowed());
        assertEquals(2, summary.getAffectedCount());
        assertEquals(ShowtimeStatus.CANCELLED, first.getStatus());
        assertEquals(ShowtimeStatus.CANCELLED, second.getStatus());
        verify(refundOutboxService, never()).enqueueCancellation(anyString(), anyString());
        verify(sourcePreview).markCancelled();
        verify(schedulePreviewRepository).saveAndFlush(sourcePreview);
    }

    @Test
    void previewBatchStatus_AllowsReplacementOnlyForAppliedAutomaticDraftBatch() {
        ShowtimeSchedulePreview sourcePreview = mock(ShowtimeSchedulePreview.class);
        when(sourcePreview.getStatus()).thenReturn(SchedulePreviewStatus.APPLIED);
        when(schedulePreviewRepository.findByPublicId("batch-1"))
                .thenReturn(Optional.of(sourcePreview));
        when(showtimeRepository.findAllByBatchIdAndDeletedAtIsNullOrderByIdAsc("batch-1"))
                .thenReturn(List.of(
                        showtime("first", ShowtimeStatus.DRAFT, "2026-07-10T12:00:00Z"),
                        showtime("second", ShowtimeStatus.DRAFT, "2026-07-10T14:00:00Z")));

        var summary = transitionService.previewBatchStatus("batch-1", ShowtimeStatus.CANCELLED);

        assertTrue(summary.isActionAllowed());
        assertTrue(summary.isAtomic());
        assertEquals(2, summary.getEligibleCount());
        assertEquals(0, summary.getSkippedCount());
    }

    @Test
    void transitionBatchStatus_CancellationBlocksWholeBatchWhenAnyShowtimeWasOpened() {
        ShowtimeSchedulePreview sourcePreview = mock(ShowtimeSchedulePreview.class);
        when(sourcePreview.getStatus()).thenReturn(SchedulePreviewStatus.APPLIED);
        when(schedulePreviewRepository.findByPublicIdForApply("batch-1"))
                .thenReturn(Optional.of(sourcePreview));
        Showtime draft = showtime("draft", ShowtimeStatus.DRAFT, "2026-07-10T12:00:00Z");
        Showtime opened = showtime("opened", ShowtimeStatus.OPEN_FOR_BOOKING, "2026-07-10T14:00:00Z");
        when(showtimeRepository.findAllByBatchIdForUpdate("batch-1"))
                .thenReturn(List.of(draft, opened));
        when(currentUserProvider.getCurrentUserId()).thenReturn(15L);
        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.CANCELLED);
        request.setReason("Replace automatic schedule");

        var summary = transitionService.transitionBatchStatus("batch-1", request);

        assertFalse(summary.isActionAllowed());
        assertEquals(0, summary.getAffectedCount());
        assertEquals(1, summary.getEligibleCount());
        assertEquals(1, summary.getSkippedCount());
        assertEquals("SHOWTIME_BATCH_REPLACEMENT_REQUIRES_AUTO_DRAFT",
                summary.getReasonGroups().get(0).getReasonCode());
        assertEquals(ShowtimeStatus.DRAFT, draft.getStatus());
        assertEquals(ShowtimeStatus.OPEN_FOR_BOOKING, opened.getStatus());
        verify(showtimeRepository, never()).saveAndFlush(any());
        verify(sourcePreview, never()).markCancelled();
        verify(schedulePreviewRepository, never()).saveAndFlush(any());
    }

    private Showtime showtime(String publicId, ShowtimeStatus status, String startTime) {
        Showtime showtime = new Showtime();
        showtime.setPublicId(publicId);
        showtime.setStatus(status);
        showtime.setStartTime(Instant.parse(startTime));
        showtime.setEndTime(Instant.parse(startTime).plusSeconds(3600));
        showtime.setSource(ShowtimeSource.AUTO);
        return showtime;
    }
}
