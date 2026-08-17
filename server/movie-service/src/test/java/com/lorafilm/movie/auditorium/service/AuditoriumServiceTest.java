package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumWithLayoutRequest;
import com.lorafilm.movie.auditorium.dto.CloneAuditoriumRequest;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.impl.AuditoriumServiceImpl;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.BulkSeatItemRequest;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditoriumServiceTest {

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private CinemaRepository cinemaRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private SeatService seatService;

    @InjectMocks
    private AuditoriumServiceImpl auditoriumService;

    private Cinema activeCinema;
    private CreateAuditoriumRequest createRequest;

    @BeforeEach
    void setUp() {
        activeCinema = new Cinema();
        activeCinema.setId(1L);
        activeCinema.setPublicId("cinema-pub-id");
        activeCinema.setName("LoraFilm Cinema");
        activeCinema.setStatus(CinemaStatus.ACTIVE);

        createRequest = new CreateAuditoriumRequest(
                "Screen 1", ScreenType.STANDARD, SoundType.STANDARD, 100, 15
        );
    }

    @Test
    void createAuditorium_Success() {
        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-pub-id"))
                .thenReturn(Optional.of(activeCinema));
        when(auditoriumRepository.existsByCinemaIdAndNameIgnoreCaseAndDeletedAtIsNull(1L, "Screen 1"))
                .thenReturn(false);
        when(auditoriumRepository.save(any(Auditorium.class))).thenAnswer(i -> {
            Auditorium a = i.getArgument(0);
            a.setId(100L);
            return a;
        });

        AuditoriumResponse response = auditoriumService.createAuditorium("cinema-pub-id", createRequest);

        assertNotNull(response);
        assertEquals("Screen 1", response.name());
        assertEquals(AuditoriumStatus.DRAFT, response.status());
        verify(auditoriumRepository, times(1)).save(any(Auditorium.class));
    }

    @Test
    void createAuditoriumWithLayout_UsesSingleSetupCommand() {
        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-pub-id"))
                .thenReturn(Optional.of(activeCinema));
        when(auditoriumRepository.existsByCinemaIdAndNameIgnoreCaseAndDeletedAtIsNull(1L, "Screen 1"))
                .thenReturn(false);
        when(auditoriumRepository.save(any(Auditorium.class))).thenAnswer(invocation -> {
            Auditorium auditorium = invocation.getArgument(0);
            auditorium.setId(100L);
            return auditorium;
        });
        BulkCreateSeatsRequest layout = new BulkCreateSeatsRequest(List.of(
                new BulkSeatItemRequest(
                        "standard-type", "A", 1, "A1", 1, 1,
                        null, SeatStatus.ACTIVE)), 100);

        AuditoriumResponse response = auditoriumService.createAuditoriumWithLayout(
                "cinema-pub-id",
                new CreateAuditoriumWithLayoutRequest(createRequest, layout));

        assertEquals(AuditoriumStatus.DRAFT, response.status());
        verify(seatService).bulkCreateSeats(response.publicId(), layout);
    }

    @Test
    void createAuditoriumWithLayout_RejectsCapacityMismatchBeforeWriting() {
        BulkCreateSeatsRequest layout = new BulkCreateSeatsRequest(List.of(
                new BulkSeatItemRequest(
                        "standard-type", "A", 1, "A1", 1, 1,
                        null, SeatStatus.ACTIVE)), 1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> auditoriumService.createAuditoriumWithLayout(
                        "cinema-pub-id",
                        new CreateAuditoriumWithLayoutRequest(createRequest, layout)));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verifyNoInteractions(cinemaRepository, seatService);
    }

    @Test
    void cloneAuditoriumLayout_resetsOperationalSeatStatusInNewDraft() {
        Auditorium source = new Auditorium();
        source.setId(20L);
        source.setPublicId("source-room");
        source.setCinema(activeCinema);

        Auditorium target = new Auditorium();
        target.setId(21L);
        target.setPublicId("target-room");
        target.setCinema(activeCinema);
        target.setName("Screen 2");
        target.setScreenType(ScreenType.STANDARD);
        target.setSoundType(SoundType.STANDARD);
        target.setCleaningBufferMinutes(15);
        target.setStatus(AuditoriumStatus.DRAFT);

        SeatType type = new SeatType();
        Seat damagedSourceSeat = new Seat();
        damagedSourceSeat.setPublicId("source-seat");
        damagedSourceSeat.setAuditorium(source);
        damagedSourceSeat.setSeatType(type);
        damagedSourceSeat.setRowLabel("A");
        damagedSourceSeat.setSeatNumber(1);
        damagedSourceSeat.setSeatCode("A1");
        damagedSourceSeat.setPositionRow(1);
        damagedSourceSeat.setPositionColumn(1);
        damagedSourceSeat.setStatus(SeatStatus.MAINTENANCE);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-pub-id"))
                .thenReturn(Optional.of(activeCinema));
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate("target-room"))
                .thenReturn(Optional.of(target));
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNull("source-room"))
                .thenReturn(Optional.of(source));
        when(seatRepository.findByAuditoriumIdAndDeletedAtIsNull(21L)).thenReturn(List.of());
        when(seatRepository.findByAuditoriumIdAndDeletedAtIsNull(20L))
                .thenReturn(List.of(damagedSourceSeat));
        when(seatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        auditoriumService.cloneAuditoriumLayout(
                "cinema-pub-id", "target-room", new CloneAuditoriumRequest("source-room"));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<Seat>> seatCaptor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(seatCaptor.capture());
        List<Seat> seats = seatCaptor.getValue();
        assertEquals(SeatStatus.ACTIVE, seats.getFirst().getStatus());
        assertNotEquals("source-seat", seats.getFirst().getPublicId());
    }

    @Test
    void createAuditorium_CinemaNotFound_ThrowsException() {
        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("invalid-id"))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> auditoriumService.createAuditorium("invalid-id", createRequest));
        assertEquals(ErrorCode.CINEMA_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createAuditorium_DuplicateName_ThrowsException() {
        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-pub-id"))
                .thenReturn(Optional.of(activeCinema));
        when(auditoriumRepository.existsByCinemaIdAndNameIgnoreCaseAndDeletedAtIsNull(1L, "Screen 1"))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> auditoriumService.createAuditorium("cinema-pub-id", createRequest));
        assertEquals(ErrorCode.AUDITORIUM_NAME_DUPLICATED, ex.getErrorCode());
    }

    @Test
    void updateAuditorium_ValidRequest_Success() {
        Auditorium auditorium = new Auditorium();
        auditorium.setId(10L);
        auditorium.setCinema(activeCinema);
        auditorium.setName("Old Name");
        auditorium.setStatus(AuditoriumStatus.DRAFT);
        
        com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest updateReq = 
            new com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest("New Name", ScreenType.IMAX, SoundType.DOLBY_ATMOS, 150, 20, AuditoriumStatus.ACTIVE);

        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate("pub-10"))
                .thenReturn(Optional.of(auditorium));
        when(seatRepository.countByAuditoriumIdAndDeletedAtIsNull(10L)).thenReturn(100L);
        when(seatRepository.countSellableLayoutSeatsByAuditoriumId(10L)).thenReturn(100L);

        AuditoriumResponse response = auditoriumService.updateAuditorium("pub-10", updateReq);
        assertEquals("New Name", response.name());
        assertEquals(AuditoriumStatus.ACTIVE, response.status());
        assertEquals(150, response.capacity());
    }

    @Test
    void updateAuditorium_ActivationRequiresSellableLayoutSeat() {
        Auditorium auditorium = new Auditorium();
        auditorium.setId(10L);
        auditorium.setCinema(activeCinema);
        auditorium.setName("Old Name");
        auditorium.setStatus(AuditoriumStatus.DRAFT);
        var updateRequest = new com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest(
                "New Name", ScreenType.STANDARD, SoundType.STANDARD,
                100, 15, AuditoriumStatus.ACTIVE);
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate("pub-10"))
                .thenReturn(Optional.of(auditorium));
        when(seatRepository.countByAuditoriumIdAndDeletedAtIsNull(10L)).thenReturn(100L);
        when(seatRepository.countSellableLayoutSeatsByAuditoriumId(10L)).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> auditoriumService.updateAuditorium("pub-10", updateRequest));

        assertEquals(ErrorCode.AUDITORIUM_LAYOUT_REQUIRED, exception.getErrorCode());
    }

    @Test
    void updateAuditorium_InvalidStatusTransition_ThrowsException() {
        Auditorium auditorium = new Auditorium();
        auditorium.setId(10L);
        auditorium.setCinema(activeCinema);
        auditorium.setStatus(AuditoriumStatus.DRAFT);
        
        com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest updateReq = 
            new com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest("New Name", ScreenType.IMAX, SoundType.DOLBY_ATMOS, 150, 20, AuditoriumStatus.MAINTENANCE);

        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate("pub-10"))
                .thenReturn(Optional.of(auditorium));

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> auditoriumService.updateAuditorium("pub-10", updateReq));
        assertEquals(ErrorCode.INVALID_AUDITORIUM_STATUS_TRANSITION, ex.getErrorCode());
    }

    @Test
    void updateAuditorium_CapacitySmallerThanActiveSeats_ThrowsException() {
        Auditorium auditorium = new Auditorium();
        auditorium.setId(10L);
        auditorium.setCinema(activeCinema);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        
        com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest updateReq = 
            new com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest("New Name", ScreenType.IMAX, SoundType.DOLBY_ATMOS, 50, 20, AuditoriumStatus.ACTIVE);

        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate("pub-10"))
                .thenReturn(Optional.of(auditorium));
        when(seatRepository.countByAuditoriumIdAndDeletedAtIsNull(10L)).thenReturn(100L);

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> auditoriumService.updateAuditorium("pub-10", updateReq));
        assertEquals(ErrorCode.AUDITORIUM_CAPACITY_BELOW_CURRENT_SEAT_COUNT, ex.getErrorCode());
    }
}
