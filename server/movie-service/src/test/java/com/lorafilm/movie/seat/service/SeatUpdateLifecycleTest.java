package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.dto.UpdateSeatRequest;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.seat.service.impl.SeatServiceImpl;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatUpdateLifecycleTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatTypeRepository seatTypeRepository;

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @InjectMocks
    private SeatServiceImpl seatService;

    private Seat seat;
    private Auditorium auditorium;
    private SeatType seatType;

    @BeforeEach
    void setUp() {
        seatType = new SeatType();
        seatType.setId(1L);
        seatType.setPublicId("type-1");
        seatType.setStatus(ActiveStatus.ACTIVE);

        auditorium = new Auditorium();
        auditorium.setId(1L);
        auditorium.setPublicId("aud-1");
        auditorium.setStatus(AuditoriumStatus.DRAFT);

        seat = new Seat();
        seat.setId(1L);
        seat.setPublicId(UUID.randomUUID().toString());
        seat.setAuditorium(auditorium);
        seat.setSeatType(seatType);
        seat.setSeatCode("A1");
        seat.setRowLabel("A");
        seat.setSeatNumber(1);
        seat.setPositionRow(1);
        seat.setPositionColumn(1);
        seat.setStatus(SeatStatus.ACTIVE);
    }

    @Test
    void shouldAllowStructuralUpdateWhenAuditoriumIsDraft() {
        when(seatRepository.findByPublicIdAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(seat));
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(anyString())).thenReturn(Optional.of(auditorium));
        
        SeatType newType = new SeatType();
        newType.setId(2L);
        newType.setPublicId("type-2");
        newType.setStatus(ActiveStatus.ACTIVE);
        when(seatTypeRepository.findByPublicIdAndDeletedAtIsNull("type-2")).thenReturn(Optional.of(newType));
        
        when(seatRepository.existsByAuditoriumIdAndSeatCodeAndIdNotAndDeletedAtIsNull(any(), any(), any())).thenReturn(false);
        when(seatRepository.existsByAuditoriumIdAndPositionRowAndPositionColumnAndIdNotAndDeletedAtIsNull(any(), any(), any(), any())).thenReturn(false);

        UpdateSeatRequest request = new UpdateSeatRequest("type-2", "B", 2, "B2", 2, 2, null, SeatStatus.MAINTENANCE);
        
        var response = seatService.updateSeat(seat.getPublicId(), request);

        assertThat(response.seatCode()).isEqualTo("B2");
        assertThat(response.status()).isEqualTo(SeatStatus.MAINTENANCE);
        verify(seatRepository).findByPublicIdAndDeletedAtIsNull(anyString());
    }

    @Test
    void shouldAllowStatusOnlyUpdateWhenAuditoriumIsActive() {
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        when(seatRepository.findByPublicIdAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(seat));
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(anyString())).thenReturn(Optional.of(auditorium));

        UpdateSeatRequest request = new UpdateSeatRequest("type-1", "A", 1, "A1", 1, 1, null, SeatStatus.MAINTENANCE);
        
        var response = seatService.updateSeat(seat.getPublicId(), request);

        assertThat(response.status()).isEqualTo(SeatStatus.MAINTENANCE);
        assertThat(response.seatCode()).isEqualTo("A1");
        verify(seatRepository, never()).existsByAuditoriumIdAndSeatCodeAndIdNotAndDeletedAtIsNull(any(), any(), any());
    }

    @Test
    void shouldRejectStructuralUpdateWhenAuditoriumIsActive() {
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        when(seatRepository.findByPublicIdAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(seat));
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(anyString())).thenReturn(Optional.of(auditorium));

        UpdateSeatRequest request = new UpdateSeatRequest("type-1", "B", 2, "B2", 2, 2, null, SeatStatus.ACTIVE);
        
        assertThatThrownBy(() -> seatService.updateSeat(seat.getPublicId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUDITORIUM_LAYOUT_NOT_EDITABLE);
    }

    @Test
    void shouldRejectStructuralUpdateWhenShowtimeHistoryExists() {
        when(seatRepository.findByPublicIdAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(seat));
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(anyString()))
                .thenReturn(Optional.of(auditorium));
        when(showtimeRepository.existsByAuditoriumId(auditorium.getId())).thenReturn(true);

        UpdateSeatRequest request = new UpdateSeatRequest(
                "type-1", "B", 2, "B2", 2, 2, null, SeatStatus.ACTIVE);

        assertThatThrownBy(() -> seatService.updateSeat(seat.getPublicId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUDITORIUM_LAYOUT_HAS_SHOWTIME_HISTORY);
    }
    
    @Test
    void shouldThrowNotFoundWhenSeatNotFound() {
        when(seatRepository.findByPublicIdAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());
        
        UpdateSeatRequest request = new UpdateSeatRequest("type-1", "A", 1, "A1", 1, 1, null, SeatStatus.ACTIVE);
        
        assertThatThrownBy(() -> seatService.updateSeat("non-existent", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEAT_NOT_FOUND);
    }
}
