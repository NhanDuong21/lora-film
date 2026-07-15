package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeMapper;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
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
    private ShowtimePricingService showtimePricingService;

    private Clock fixedClock;

    private ShowtimeStatusTransitionServiceImpl transitionService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneId.of("UTC"));
        transitionService = new ShowtimeStatusTransitionServiceImpl(
                showtimeRepository, historyService, currentUserProvider, adminShowtimeMapper, fixedClock, showtimePricingService);
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
        verify(showtimePricingService).validateCompleteness(showtime);
        verify(historyService).recordTransitionHistory(eq(showtime), eq(ShowtimeStatus.DRAFT), eq(ShowtimeStatus.OPEN_FOR_BOOKING), isNull(), eq(1L), eq(fixedClock.instant()));
    }

    @Test
    void transitionStatus_DraftToOpen_FailsIfAlreadyStarted() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        Showtime showtime = new Showtime();
        showtime.setStatus(ShowtimeStatus.DRAFT);
        showtime.setStartTime(Instant.parse("2026-07-10T09:00:00Z")); // past

        when(showtimeRepository.findByPublicIdForUpdate("pub-id")).thenReturn(Optional.of(showtime));

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
}
