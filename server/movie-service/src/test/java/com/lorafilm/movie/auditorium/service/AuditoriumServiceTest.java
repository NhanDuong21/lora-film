package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.impl.AuditoriumServiceImpl;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
}
