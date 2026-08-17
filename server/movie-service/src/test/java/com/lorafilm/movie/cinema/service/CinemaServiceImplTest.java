package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaMedia;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.domain.enums.CinemaMediaType;
import com.lorafilm.movie.cinema.dto.*;
import com.lorafilm.movie.cinema.repository.CinemaMediaRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    private CinemaClosurePeriodRepository cinemaClosurePeriodRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private CinemaMapper cinemaMapper;

    @InjectMocks
    private CinemaServiceImpl cinemaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lenient().when(cinemaRepository.findByPublicIdForScheduling(anyString()))
                .thenAnswer(invocation -> cinemaRepository.findByPublicIdAndDeletedAtIsNull(
                        invocation.getArgument(0)));
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
        stubBasicInformation(existingCinema);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("uuid-123")).thenReturn(Optional.of(existingCinema));
        stubOperatingHours();
        stubActiveRoomWithLayout(existingCinema);
        when(cinemaRepository.save(any(Cinema.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CinemaResponse expectedResponse = new CinemaResponse();
        expectedResponse.setPublicId("uuid-123");
        expectedResponse.setStatus(CinemaStatus.ACTIVE);
        when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(expectedResponse);

        CinemaResponse response = cinemaService.updateCinemaStatus("uuid-123", CinemaStatus.ACTIVE);

        assertEquals(CinemaStatus.ACTIVE, response.getStatus());
        verify(cinemaRepository, times(1)).save(any(Cinema.class));
    }

    @Test
    void updateCinemaStatus_shouldThrowException_whenActivatingWithoutAuditoriums() {
        Cinema existingCinema = new Cinema();
        existingCinema.setId(1L);
        existingCinema.setPublicId("uuid-123");
        existingCinema.setStatus(CinemaStatus.DRAFT);
        stubBasicInformation(existingCinema);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("uuid-123")).thenReturn(Optional.of(existingCinema));
        stubOperatingHours();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> cinemaService.updateCinemaStatus("uuid-123", CinemaStatus.ACTIVE));
        assertEquals(ErrorCode.CINEMA_ACTIVATION_BLOCKED, exception.getErrorCode());
        verify(cinemaRepository, never()).save(any(Cinema.class));
    }

    @Test
    void updateCinemaStatus_shouldAllowActivationWithoutPublicProfileImages() {
        Cinema existingCinema = new Cinema();
        existingCinema.setId(1L);
        existingCinema.setPublicId("uuid-123");
        existingCinema.setStatus(CinemaStatus.DRAFT);
        stubBasicInformation(existingCinema);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("uuid-123")).thenReturn(Optional.of(existingCinema));
        stubOperatingHours();
        stubActiveRoomWithLayout(existingCinema);
        when(cinemaMediaRepository.findByCinemaIdAndStatusAndDeletedAtIsNullOrderByDisplayOrderAsc(
                1L, ActiveStatus.ACTIVE)).thenReturn(List.of());
        when(cinemaRepository.save(any(Cinema.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CinemaResponse expectedResponse = new CinemaResponse();
        expectedResponse.setStatus(CinemaStatus.ACTIVE);
        when(cinemaMapper.toResponse(any(Cinema.class))).thenReturn(expectedResponse);

        CinemaResponse response = cinemaService.updateCinemaStatus("uuid-123", CinemaStatus.ACTIVE);

        assertEquals(CinemaStatus.ACTIVE, response.getStatus());
        verify(cinemaRepository).save(any(Cinema.class));
    }

    @Test
    void updateCinemaStatus_shouldThrowException_whenActivatingWithoutOperatingHours() {
        Cinema existingCinema = new Cinema();
        existingCinema.setId(1L);
        existingCinema.setPublicId("uuid-123");
        existingCinema.setStatus(CinemaStatus.DRAFT);
        stubBasicInformation(existingCinema);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("uuid-123")).thenReturn(Optional.of(existingCinema));
        stubActiveRoomWithLayout(existingCinema);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> cinemaService.updateCinemaStatus("uuid-123", CinemaStatus.ACTIVE));
        assertEquals(ErrorCode.CINEMA_ACTIVATION_BLOCKED, exception.getErrorCode());
        verify(cinemaRepository, never()).save(any(Cinema.class));
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

    @Test
    void addCinemaMedia_shouldSaveSuccessfully_whenValid() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        CreateCinemaMediaRequest request = new CreateCinemaMediaRequest();
        request.setMediaType(CinemaMediaType.BANNER);
        request.setUrl("http://example.com/banner.jpg");
        request.setTitle("Banner");
        request.setIsPrimary(false);

        when(cinemaRepository.findByPublicIdForScheduling("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(cinemaMediaRepository.save(any(CinemaMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        CinemaMediaResponse responseDto = new CinemaMediaResponse();
        responseDto.setPublicId("media-uuid");
        responseDto.setUrl(request.getUrl());
        when(cinemaMapper.toMediaResponse(any(CinemaMedia.class))).thenReturn(responseDto);

        CinemaMediaResponse response = cinemaService.addCinemaMedia("cinema-uuid", request);

        assertNotNull(response);
        assertEquals("media-uuid", response.getPublicId());
        verify(cinemaMediaRepository, times(1)).save(any(CinemaMedia.class));
    }

    @Test
    void addCinemaMedia_shouldSwitchPrimaryFlag_whenNewMediaIsPrimary() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        CreateCinemaMediaRequest request = new CreateCinemaMediaRequest();
        request.setMediaType(CinemaMediaType.BANNER);
        request.setUrl("http://example.com/banner.jpg");
        request.setIsPrimary(true);

        CinemaMedia oldPrimary = new CinemaMedia();
        oldPrimary.setId(2L);
        oldPrimary.setIsPrimary(true);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(cinemaMediaRepository.findByCinemaIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                eq(1L), eq(CinemaMediaType.BANNER), eq(ActiveStatus.ACTIVE)))
                .thenReturn(java.util.Collections.singletonList(oldPrimary));

        when(cinemaMediaRepository.save(any(CinemaMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        cinemaService.addCinemaMedia("cinema-uuid", request);

        assertFalse(oldPrimary.getIsPrimary());
        verify(cinemaMediaRepository, times(2)).save(any(CinemaMedia.class));
    }

    @Test
    void updateOperatingHours_shouldUpdateSuccessfully() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        List<OperatingHourUpdateRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            OperatingHourUpdateRequest req = new OperatingHourUpdateRequest();
            req.setDayOfWeek(i);
            req.setIsClosed(false);
            req.setOpenTime("08:00");
            req.setCloseTime("22:00");
            requests.add(req);
        }

        when(cinemaRepository.findByPublicIdForScheduling("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(cinemaOperatingHourRepository.findByCinemaId(1L)).thenReturn(new ArrayList<>());
        when(cinemaOperatingHourRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        cinemaService.updateOperatingHours("cinema-uuid", requests);

        verify(cinemaOperatingHourRepository, times(1)).saveAll(anyList());
    }

    @Test
    void updateOperatingHours_shouldAcceptOvernightRange() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        List<OperatingHourUpdateRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            OperatingHourUpdateRequest req = new OperatingHourUpdateRequest();
            req.setDayOfWeek(i);
            req.setIsClosed(true);
            requests.add(req);
        }
        // Day 7 closes on the following service day.
        OperatingHourUpdateRequest req7 = new OperatingHourUpdateRequest();
        req7.setDayOfWeek(7);
        req7.setIsClosed(false);
        req7.setOpenTime("22:00");
        req7.setCloseTime("08:00");
        requests.add(req7);

        when(cinemaRepository.findByPublicIdForScheduling("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(cinemaOperatingHourRepository.findByCinemaId(1L)).thenReturn(new ArrayList<>());
        when(cinemaOperatingHourRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        cinemaService.updateOperatingHours("cinema-uuid", requests);

        verify(cinemaOperatingHourRepository).saveAll(anyList());
    }

    @Test
    void updateOperatingHours_shouldRejectZeroLengthRange() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        List<OperatingHourUpdateRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            OperatingHourUpdateRequest req = new OperatingHourUpdateRequest();
            req.setDayOfWeek(i);
            req.setIsClosed(true);
            requests.add(req);
        }
        OperatingHourUpdateRequest req7 = new OperatingHourUpdateRequest();
        req7.setDayOfWeek(7);
        req7.setIsClosed(false);
        req7.setOpenTime("08:00");
        req7.setCloseTime("08:00");
        requests.add(req7);

        when(cinemaRepository.findByPublicIdForScheduling("cinema-uuid")).thenReturn(Optional.of(cinema));

        assertThrows(BusinessException.class,
                () -> cinemaService.updateOperatingHours("cinema-uuid", requests));
    }

    @Test
    void createClosurePeriod_shouldSaveSuccessfully() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        CreateCinemaClosurePeriodRequest request = new CreateCinemaClosurePeriodRequest();
        request.setStartTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));
        request.setReason("Maintenance");

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(cinemaClosurePeriodRepository.findOverlappingClosures(eq(1L), any(Instant.class), any(Instant.class)))
                .thenReturn(java.util.Collections.emptyList());
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(cinemaClosurePeriodRepository.save(any(CinemaClosurePeriod.class))).thenAnswer(inv -> inv.getArgument(0));

        cinemaService.createClosurePeriod("cinema-uuid", request);

        verify(cinemaClosurePeriodRepository, times(1)).save(any(CinemaClosurePeriod.class));
        verify(cinemaClosurePeriodRepository).save(argThat(period -> period.getServiceDate() != null));
    }

    @Test
    void createClosurePeriod_shouldRejectAffectedShowtimes() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");
        cinema.setTimezone("Asia/Ho_Chi_Minh");

        CreateCinemaClosurePeriodRequest request = new CreateCinemaClosurePeriodRequest();
        request.setStartTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));
        request.setReason("MAINTENANCE");

        when(cinemaRepository.findByPublicIdForScheduling("cinema-uuid"))
                .thenReturn(Optional.of(cinema));
        when(cinemaClosurePeriodRepository.findOverlappingClosures(
                eq(1L), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(showtimeRepository.findCinemaPotentialOverlaps(
                eq(1L), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new com.lorafilm.movie.showtime.domain.entity.Showtime()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> cinemaService.createClosurePeriod("cinema-uuid", request));

        assertEquals(ErrorCode.CINEMA_CLOSURE_HAS_AFFECTED_SHOWTIMES, exception.getErrorCode());
        verify(cinemaClosurePeriodRepository, never()).save(any());
    }

    @Test
    void createClosurePeriod_shouldThrowConflict_whenOverlappingClosureExists() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        CreateCinemaClosurePeriodRequest request = new CreateCinemaClosurePeriodRequest();
        request.setStartTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(cinemaClosurePeriodRepository.findOverlappingClosures(eq(1L), any(Instant.class), any(Instant.class)))
                .thenReturn(java.util.Collections.singletonList(new CinemaClosurePeriod()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cinemaService.createClosurePeriod("cinema-uuid", request));
        assertEquals(ErrorCode.CINEMA_CLOSURE_CONFLICT, ex.getErrorCode());
    }

    @Test
    void cancelClosurePeriod_shouldCancelSuccessfully() {
        CinemaClosurePeriod period = new CinemaClosurePeriod();
        period.setId(5L);
        period.setStatus(ActionStatus.ACTIVE);

        when(cinemaClosurePeriodRepository.findById(5L)).thenReturn(Optional.of(period));
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(cinemaClosurePeriodRepository.save(any(CinemaClosurePeriod.class))).thenAnswer(inv -> inv.getArgument(0));

        cinemaService.cancelClosurePeriod(5L);

        assertEquals(ActionStatus.CANCELLED, period.getStatus());
        verify(cinemaClosurePeriodRepository, times(1)).save(any(CinemaClosurePeriod.class));
    }

    @Test
    void getAdminCinemaDetail_shouldReturnDetail_regardlessOfStatus() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");
        cinema.setStatus(CinemaStatus.DRAFT);
        cinema.setName("Draft Cinema");

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-uuid")).thenReturn(Optional.of(cinema));
        
        CinemaDto baseDto = new CinemaDto();
        baseDto.setPublicId("cinema-uuid");
        baseDto.setName("Draft Cinema");
        when(cinemaMapper.toDto(cinema)).thenReturn(baseDto);

        CinemaDetailDto detail = cinemaService.getAdminCinemaDetail("cinema-uuid");

        assertNotNull(detail);
        assertEquals("cinema-uuid", detail.getPublicId());
        assertEquals("Draft Cinema", detail.getName());
    }

    @Test
    void deleteCinema_shouldSoftDelete_whenNoAuditoriumOrShowtime() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(auditoriumRepository.existsByCinemaIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(showtimeRepository.existsByCinemaIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);

        cinemaService.deleteCinema("cinema-uuid");

        assertNotNull(cinema.getDeletedAt());
        assertEquals(99L, cinema.getDeletedBy());
        verify(cinemaRepository, times(1)).save(cinema);
    }

    @Test
    void deleteCinema_shouldSoftDelete_whenHasAuditoriums() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(showtimeRepository.existsByCinemaIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(auditoriumRepository.findByCinemaIdAndDeletedAtIsNull(1L)).thenReturn(Collections.emptyList());

        cinemaService.deleteCinema("cinema-uuid");

        assertEquals(1L, cinema.getDeletedBy());
        assertNotNull(cinema.getDeletedAt());
        verify(cinemaRepository).save(cinema);
    }

    @Test
    void deleteCinema_shouldThrowException_whenHasShowtimes() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-uuid");

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-uuid")).thenReturn(Optional.of(cinema));
        when(auditoriumRepository.existsByCinemaIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(showtimeRepository.existsByCinemaIdAndDeletedAtIsNull(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> cinemaService.deleteCinema("cinema-uuid"));
        assertEquals(ErrorCode.CINEMA_CANNOT_BE_DELETED_HAS_SHOWTIME_HISTORY, ex.getErrorCode());
        verify(cinemaRepository, never()).save(any(Cinema.class));
    }

    @Test
    void deleteCinemaMedia_shouldSoftDeleteAndResetPrimary() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);

        CinemaMedia media = new CinemaMedia();
        media.setId(2L);
        media.setPublicId("media-uuid");
        media.setCinema(cinema);
        media.setIsPrimary(true);

        when(cinemaMediaRepository.findByPublicIdAndDeletedAtIsNull("media-uuid")).thenReturn(Optional.of(media));
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);

        cinemaService.deleteCinemaMedia("media-uuid");

        assertNotNull(media.getDeletedAt());
        assertEquals(99L, media.getDeletedBy());
        assertFalse(media.getIsPrimary());
        assertEquals(ActiveStatus.INACTIVE, media.getStatus());
        verify(cinemaMediaRepository, times(1)).save(media);
    }

    @Test
    void getCinemaClosurePeriods_shouldReturnOnlyActiveFutureClosures() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setStatus(CinemaStatus.ACTIVE);

        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-uuid")).thenReturn(Optional.of(cinema));
        
        CinemaClosurePeriod activeClosure = new CinemaClosurePeriod();
        activeClosure.setId(10L);
        activeClosure.setStatus(ActionStatus.ACTIVE);
        when(cinemaClosurePeriodRepository.findByCinemaIdAndStatusAndEndTimeAfterOrderByStartTimeAsc(
                eq(1L), eq(ActionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(java.util.Collections.singletonList(activeClosure));

        CinemaClosurePeriodResponse response = new CinemaClosurePeriodResponse();
        response.setId(10L);
        response.setStatus(ActionStatus.ACTIVE);
        when(cinemaMapper.toClosurePeriodResponse(activeClosure)).thenReturn(response);

        List<CinemaClosurePeriodResponse> result = cinemaService.getCinemaClosurePeriods("cinema-uuid");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    private void stubBasicInformation(Cinema cinema) {
        cinema.setName("LoraFilm Test");
        cinema.setCity("HCM");
        cinema.setAddress("1 Test Street");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
    }

    private void stubOperatingHours() {
        List<CinemaOperatingHour> hours = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            CinemaOperatingHour hour = new CinemaOperatingHour();
            hour.setDayOfWeek(day);
            hour.setOpenTime(LocalTime.of(8, 0));
            hour.setCloseTime(LocalTime.of(23, 0));
            hour.setIsClosed(false);
            hours.add(hour);
        }
        when(cinemaOperatingHourRepository.findByCinemaId(1L)).thenReturn(hours);
    }

    private void stubActiveRoomWithLayout(Cinema cinema) {
        Auditorium auditorium = new Auditorium();
        auditorium.setId(10L);
        auditorium.setCinema(cinema);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCapacity(100);
        when(auditoriumRepository.findByCinemaIdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(auditorium));
        when(seatRepository.countSellableLayoutSeatsByAuditoriumId(10L)).thenReturn(100L);
    }
}
