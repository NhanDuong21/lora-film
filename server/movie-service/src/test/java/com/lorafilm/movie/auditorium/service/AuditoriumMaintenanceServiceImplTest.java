package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.domain.enums.MaintenanceType;
import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.ExtendMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceImpactResponse;
import com.lorafilm.movie.auditorium.dto.ResolveMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.impl.AuditoriumMaintenanceServiceImpl;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriumMaintenanceServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-09T05:00:00Z");

    @Mock
    private AuditoriumMaintenanceWindowRepository maintenanceRepository;
    @Mock
    private AuditoriumRepository auditoriumRepository;
    @Mock
    private AuditoriumMaintenanceImpactService impactService;
    @Mock
    private ShowtimeStatusTransitionService showtimeTransitionService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private AuditoriumMaintenanceServiceImpl service;
    private Auditorium auditorium;

    @BeforeEach
    void setUp() {
        service = new AuditoriumMaintenanceServiceImpl(
                maintenanceRepository,
                auditoriumRepository,
                impactService,
                showtimeTransitionService,
                currentUserProvider,
                Clock.fixed(NOW, ZoneOffset.UTC));
        auditorium = new Auditorium();
        auditorium.setId(11L);
        auditorium.setPublicId("room-01");
        auditorium.setName("Phòng 01");
    }

    @Test
    void plannedMaintenanceIsBlockedUntilAffectedShowtimesAreHandled() {
        CreateMaintenanceWindowRequest request = new CreateMaintenanceWindowRequest(
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                "Bảo trì định kỳ",
                MaintenanceType.PLANNED);
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate("room-01"))
                .thenReturn(Optional.of(auditorium));
        when(maintenanceRepository.findFirstOverlap(eq(11L), eq(ActionStatus.ACTIVE), any(), any()))
                .thenReturn(Optional.empty());
        when(impactService.preview(eq("room-01"), any())).thenReturn(impactWithOpenShowtime());

        assertThatThrownBy(() -> service.createWindow("room-01", request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.PLANNED_MAINTENANCE_HAS_AFFECTED_SHOWTIMES);
        verify(maintenanceRepository, never()).saveAndFlush(any());
    }

    @Test
    void emergencyStartsImmediatelyAndClosesAffectedOpenShowtimes() {
        CreateMaintenanceWindowRequest request = new CreateMaintenanceWindowRequest(
                NOW.plusSeconds(600),
                NOW.plusSeconds(7200),
                "Máy chiếu mất hình",
                MaintenanceType.EMERGENCY);
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate("room-01"))
                .thenReturn(Optional.of(auditorium));
        when(maintenanceRepository.findFirstOverlap(eq(11L), eq(ActionStatus.ACTIVE), any(), any()))
                .thenReturn(Optional.empty());
        when(impactService.preview(eq("room-01"), any())).thenReturn(impactWithOpenShowtime());
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(maintenanceRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            AuditoriumMaintenanceWindow window = invocation.getArgument(0);
            window.setId(7L);
            return window;
        });

        var response = service.createWindow("room-01", request);

        assertThat(response.maintenanceType()).isEqualTo(MaintenanceType.EMERGENCY);
        assertThat(response.startTime()).isEqualTo(NOW);
        ArgumentCaptor<UpdateShowtimeStatusRequest> statusRequest =
                ArgumentCaptor.forClass(UpdateShowtimeStatusRequest.class);
        verify(showtimeTransitionService).transitionStatus(eq("showtime-01"), statusRequest.capture());
        assertThat(statusRequest.getValue().getStatus()).isEqualTo(ShowtimeStatus.CANCELLED);
        assertThat(statusRequest.getValue().getReason()).contains("sự cố phòng chiếu #7");
    }

    @Test
    void startedMaintenanceCannotBeCancelledAndMustBeResolved() {
        AuditoriumMaintenanceWindow window = activeWindow();
        when(maintenanceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(window));

        assertThatThrownBy(() -> service.cancelWindow(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.MAINTENANCE_WINDOW_CANNOT_BE_CANCELLED_AFTER_START);
    }

    @Test
    void resolvingMaintenanceKeepsHistoryAndRecordsActualEndTime() {
        AuditoriumMaintenanceWindow window = activeWindow();
        when(maintenanceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(window));
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);

        var response = service.resolveWindow(
                7L,
                new ResolveMaintenanceWindowRequest(true, "Đã thay bóng đèn và chạy thử ổn định"));

        assertThat(response.status()).isEqualTo(ActionStatus.RESOLVED);
        assertThat(response.actualEndTime()).isEqualTo(NOW);
        assertThat(response.resolvedBy()).isEqualTo(99L);
        assertThat(response.resolutionNote()).isEqualTo("Đã thay bóng đèn và chạy thử ổn định");
    }

    @Test
    void extendingEmergencyClosureChecksImpactAndClosesNewlyAffectedOpenShowtimes() {
        AuditoriumMaintenanceWindow window = activeWindow();
        when(maintenanceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(window));
        when(maintenanceRepository.findFirstOverlapExcluding(
                eq(11L), eq(7L), eq(ActionStatus.ACTIVE), any(), any()))
                .thenReturn(Optional.empty());
        when(impactService.preview(eq("room-01"), any())).thenReturn(impactWithOpenShowtime());
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);

        var response = service.extendWindow(
                7L,
                new ExtendMaintenanceWindowRequest(
                        NOW.plusSeconds(7200),
                        "Cần thêm thời gian chạy thử thiết bị"));

        assertThat(response.endTime()).isEqualTo(NOW.plusSeconds(7200));
        assertThat(response.extensionNote()).isEqualTo("Cần thêm thời gian chạy thử thiết bị");
        verify(showtimeTransitionService).transitionStatus(eq("showtime-01"), any());
    }

    private AuditoriumMaintenanceWindow activeWindow() {
        AuditoriumMaintenanceWindow window = new AuditoriumMaintenanceWindow();
        window.setId(7L);
        window.setAuditorium(auditorium);
        window.setStartTime(NOW.minusSeconds(1800));
        window.setEndTime(NOW.plusSeconds(3600));
        window.setReason("Máy chiếu mất hình");
        window.setMaintenanceType(MaintenanceType.EMERGENCY);
        window.setStatus(ActionStatus.ACTIVE);
        return window;
    }

    private MaintenanceImpactResponse impactWithOpenShowtime() {
        return new MaintenanceImpactResponse(
                "room-01",
                "Phòng 01",
                NOW,
                NOW.plusSeconds(7200),
                1,
                1,
                0,
                2,
                true,
                List.of(new MaintenanceImpactResponse.AffectedShowtime(
                        "showtime-01",
                        "Phim A",
                        NOW.plusSeconds(600),
                        NOW.plusSeconds(4200),
                        ShowtimeStatus.OPEN_FOR_BOOKING,
                        2,
                        true)));
    }
}
