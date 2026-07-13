package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.CinemaMapper;
import com.lorafilm.movie.cinema.dto.CinemaResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaRequest;
import com.lorafilm.movie.cinema.repository.CinemaMediaRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CinemaServiceImplTest {

    @Mock
    private CinemaRepository cinemaRepository;

    @Mock
    private CinemaOperatingHourRepository cinemaOperatingHourRepository;

    @Mock
    private CinemaMediaRepository cinemaMediaRepository;

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private CinemaMapper cinemaMapper;

    @InjectMocks
    private CinemaServiceImpl cinemaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCinema_shouldCreateSuccessfully_whenRequestIsValid() {
        CreateCinemaRequest request = new CreateCinemaRequest();
        request.setName("Lorafilm District 1");
        request.setCity("HCM");
        request.setDistrict("D1");
        request.setAddress("123 Street");
        request.setTimezone("Asia/Ho_Chi_Minh");
        request.setOpenedDate(LocalDate.of(2026, 1, 1));
        request.setClosedDate(LocalDate.of(2026, 12, 31));

        when(cinemaRepository.existsBySlugAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(cinemaRepository.save(any(Cinema.class))).thenAnswer(invocation -> {
            Cinema c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CinemaResponse expectedResponse = new CinemaResponse();
        expectedResponse.setPublicId("uuid-123");
        expectedResponse.setName(request.getName());
        expectedResponse.setSlug("lorafilm-district-1");
        expectedResponse.setStatus(CinemaStatus.DRAFT);
        expectedResponse.setTimezone("Asia/Ho_Chi_Minh");

        when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(expectedResponse);

        CinemaResponse response = cinemaService.createCinema(request);

        assertNotNull(response);
        assertEquals("uuid-123", response.getPublicId());
        assertEquals("Lorafilm District 1", response.getName());
        assertEquals(CinemaStatus.DRAFT, response.getStatus());

        verify(cinemaRepository, times(1)).save(any(Cinema.class));
    }

    @Test
    void createCinema_shouldThrowException_whenTimezoneIsInvalid() {
        CreateCinemaRequest request = new CreateCinemaRequest();
        request.setName("Lorafilm District 1");
        request.setCity("HCM");
        request.setDistrict("D1");
        request.setAddress("123 Street");
        request.setTimezone("Invalid/Timezone");

        BusinessException exception = assertThrows(BusinessException.class, () -> cinemaService.createCinema(request));
        assertEquals(ErrorCode.INVALID_CINEMA_TIMEZONE, exception.getErrorCode());
        verify(cinemaRepository, never()).save(any(Cinema.class));
    }

    @Test
    void createCinema_shouldThrowException_whenClosedDateBeforeOpenedDate() {
        CreateCinemaRequest request = new CreateCinemaRequest();
        request.setName("Lorafilm District 1");
        request.setCity("HCM");
        request.setDistrict("D1");
        request.setAddress("123 Street");
        request.setOpenedDate(LocalDate.of(2026, 12, 31));
        request.setClosedDate(LocalDate.of(2026, 1, 1));

        BusinessException exception = assertThrows(BusinessException.class, () -> cinemaService.createCinema(request));
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(cinemaRepository, never()).save(any(Cinema.class));
    }

    @Test
    void createCinema_shouldThrowException_whenSlugAlreadyExists() {
        CreateCinemaRequest request = new CreateCinemaRequest();
        request.setName("Lorafilm District 1");
        request.setCity("HCM");
        request.setDistrict("D1");
        request.setAddress("123 Street");

        when(cinemaRepository.existsBySlugAndDeletedAtIsNull(anyString())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> cinemaService.createCinema(request));
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(cinemaRepository, never()).save(any(Cinema.class));
    }

    @Test
    void updateCinema_shouldUpdateSuccessfully_whenRequestIsValid() {
        Cinema existingCinema = new Cinema();
        existingCinema.setId(1L);
        existingCinema.setPublicId("uuid-123");
        existingCinema.setName("Old Name");
        existingCinema.setSlug("old-name");
        existingCinema.setStatus(CinemaStatus.DRAFT);

        UpdateCinemaRequest request = new UpdateCinemaRequest();
        request.setName("New Name");
        request.setCity("HCM");
        request.setDistrict("D1");
        request.setAddress("123 Street");
        request.setTimezone("Asia/Ho_Chi_Minh");

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("uuid-123")).thenReturn(Optional.of(existingCinema));
        when(cinemaRepository.existsBySlugAndPublicIdNotAndDeletedAtIsNull(anyString(), anyString())).thenReturn(false);
        when(cinemaRepository.save(any(Cinema.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CinemaResponse expectedResponse = new CinemaResponse();
        expectedResponse.setPublicId("uuid-123");
        expectedResponse.setName(request.getName());
        expectedResponse.setSlug("new-name");

        when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(expectedResponse);

        CinemaResponse response = cinemaService.updateCinema("uuid-123", request);

        assertNotNull(response);
        assertEquals("New Name", response.getName());
        assertEquals("new-name", response.getSlug());
        verify(cinemaRepository, times(1)).save(any(Cinema.class));
    }

    @Test
    void updateCinemaStatus_shouldTransitionDraftToActive_successfully() {
        Cinema existingCinema = new Cinema();
        existingCinema.setId(1L);
        existingCinema.setPublicId("uuid-123");
        existingCinema.setStatus(CinemaStatus.DRAFT);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("uuid-123")).thenReturn(Optional.of(existingCinema));
        when(cinemaRepository.save(any(Cinema.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CinemaResponse expectedResponse = new CinemaResponse();
        expectedResponse.setPublicId("uuid-123");
        expectedResponse.setStatus(CinemaStatus.ACTIVE);
        when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(expectedResponse);

        CinemaResponse response = cinemaService.updateCinemaStatus("uuid-123", CinemaStatus.ACTIVE);

        assertNotNull(response);
        assertEquals(CinemaStatus.ACTIVE, response.getStatus());
        verify(cinemaRepository, times(1)).save(any(Cinema.class));
    }

    @Test
    void updateCinemaStatus_shouldThrowException_whenTransitionIsInvalid() {
        Cinema existingCinema = new Cinema();
        existingCinema.setId(1L);
        existingCinema.setPublicId("uuid-123");
        existingCinema.setStatus(CinemaStatus.DRAFT);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("uuid-123")).thenReturn(Optional.of(existingCinema));

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> cinemaService.updateCinemaStatus("uuid-123", CinemaStatus.MAINTENANCE));
        assertEquals(ErrorCode.INVALID_AUDITORIUM_STATUS_TRANSITION, exception.getErrorCode());
        verify(cinemaRepository, never()).save(any(Cinema.class));
    }

    @Test
    void updateCinemaStatus_shouldThrowException_whenTerminalStateIsViolated() {
        Cinema existingCinema = new Cinema();
        existingCinema.setId(1L);
        existingCinema.setPublicId("uuid-123");
        existingCinema.setStatus(CinemaStatus.PERMANENTLY_CLOSED);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("uuid-123")).thenReturn(Optional.of(existingCinema));

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> cinemaService.updateCinemaStatus("uuid-123", CinemaStatus.ACTIVE));
        assertEquals(ErrorCode.INVALID_AUDITORIUM_STATUS_TRANSITION, exception.getErrorCode());
        verify(cinemaRepository, never()).save(any(Cinema.class));
    }
}
