package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.ApplyShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePricingPreflightResponse;
import com.lorafilm.movie.autoschedule.mapper.AutoScheduleApplyResponseMapper;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleApplyRevalidationService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleAuditoriumLockService;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePricingPreflightService;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreflightService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleShowtimeCreationService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePreviewApplyServiceImplTest {

    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private ShowtimeSchedulePreviewExpiryService expiryService;
    @Mock private ShowtimeSchedulePreviewRepository previewRepository;
    @Mock private ShowtimeSchedulePreviewItemRepository itemRepository;
    @Mock private AutoScheduleAuditoriumLockService auditoriumLockService;
    @Mock private AutoScheduleApplyRevalidationService revalidationService;
    @Mock private AutoScheduleShowtimeCreationService showtimeCreationService;
    @Mock private AutoSchedulePricingPreflightService pricingPreflightService;
    @Mock private AutoSchedulePreflightService preflightService;
    @Mock private AutoScheduleApplyResponseMapper responseMapper;
    @Mock private MovieRepository movieRepository;
    @Mock private MovieVersionRepository movieVersionRepository;
    @Mock private CinemaRepository cinemaRepository;
    @Mock private AuditoriumRepository auditoriumRepository;
    @Mock private EntityManager entityManager;
    @Mock private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private AutoSchedulePreviewApplyServiceImpl applyService;
    private TransactionTemplate transactionTemplate;
    private Clock clock;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-07-22T08:00:00Z");
        clock = mock(Clock.class);
        lenient().when(clock.instant()).thenReturn(now);
        transactionTemplate = new TransactionTemplate(transactionManager) {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
        AutoSchedulePricingPreflightResponse completePricing = new AutoSchedulePricingPreflightResponse(
                true, 1, 1, 0, 0, List.of(), List.of());
        lenient().when(pricingPreflightService.evaluate(any())).thenReturn(
                new AutoSchedulePricingPreflightService.Evaluation(completePricing, List.of()));
        lenient().when(cinemaRepository.findByIdForScheduling(anyLong())).thenAnswer(invocation -> {
            Cinema locked = new Cinema();
            locked.setId(invocation.getArgument(0));
            return Optional.of(locked);
        });
        applyService = new AutoSchedulePreviewApplyServiceImpl(
                currentUserProvider, expiryService, previewRepository, itemRepository,
                auditoriumLockService, revalidationService, showtimeCreationService,
                pricingPreflightService, responseMapper, clock, transactionTemplate, movieRepository,
                movieVersionRepository, cinemaRepository, auditoriumRepository, entityManager,
                preflightService);
    }

    @Test
    void applyPreview_successReloadsAuthoritativeStateBeforeCreation() {
        String previewId = "prev-1";
        String idempotencyKey = "key-1";
        long actorId = 100L;
        ApplyShowtimeSchedulePreviewRequest request = request(idempotencyKey, 1L);

        Cinema cinema = cinema();
        Auditorium auditorium = auditorium(cinema);
        Movie movie = movie();
        MovieVersion version = version(movie);
        ShowtimeSchedulePreview preview = preview(previewId, cinema, 1L, now.plusSeconds(3600));
        ShowtimeSchedulePreviewItem item = item(preview, movie, version, cinema, auditorium);
        Movie staleMovie = movie();
        staleMovie.setStatus(MovieStatus.DRAFT);
        item.setMovie(staleMovie);
        ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference =
                reference(item.getId(), movie.getId(), version.getId(), cinema.getId(), auditorium.getId());

        when(currentUserProvider.getCurrentUserId()).thenReturn(actorId);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);
        when(previewRepository.findByApplyIdempotencyKeyDetailed(idempotencyKey)).thenReturn(Optional.empty());
        when(previewRepository.findByPublicIdForApply(previewId)).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectedItemReferencesForApply(1L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of(reference));
        when(itemRepository.findAllById(List.of(item.getId()))).thenReturn(List.of(item));
        when(movieRepository.findAllById(List.of(movie.getId()))).thenReturn(List.of(movie));
        when(movieVersionRepository.findAllById(List.of(version.getId()))).thenReturn(List.of(version));
        when(cinemaRepository.findAllById(List.of(cinema.getId()))).thenReturn(List.of(cinema));
        when(auditoriumRepository.findAllById(List.of(auditorium.getId()))).thenReturn(List.of(auditorium));
        when(showtimeCreationService.createAll(any(), anyLong(), anyString(), any()))
                .thenReturn(List.of(new Showtime()));
        when(itemRepository.findDetailedItemsByPreviewId(1L)).thenReturn(List.of(item));
        when(responseMapper.toResponse(preview)).thenReturn(new ApplyShowtimeSchedulePreviewResponse());

        ApplyShowtimeSchedulePreviewResponse result = applyService.applyPreview(previewId, request);

        assertThat(result).isNotNull();
        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.APPLIED);
        assertThat(item.getApplyStatus()).isEqualTo(PreviewItemApplyStatus.CREATED);
        verify(auditoriumLockService).lockAll(List.of(auditorium.getId()));
        verify(entityManager).clear();
        verify(revalidationService).validateAll(preview, List.of(item), now);
        assertThat(item.getMovie()).isSameAs(movie);
    }

    @Test
    void applyPreview_incompletePricingStopsBeforeAnyShowtimeWrite() {
        String previewId = "prev-pricing-incomplete";
        Cinema cinema = cinema();
        Auditorium auditorium = auditorium(cinema);
        Movie movie = movie();
        MovieVersion version = version(movie);
        ShowtimeSchedulePreview preview = preview(previewId, cinema, 1L, now.plusSeconds(3600));
        ShowtimeSchedulePreviewItem item = item(preview, movie, version, cinema, auditorium);
        ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference =
                reference(item.getId(), movie.getId(), version.getId(), cinema.getId(), auditorium.getId());
        AutoSchedulePricingPreflightResponse blocked = new AutoSchedulePricingPreflightResponse(
                false, 1, 0, 1, 0, List.of(), List.of());

        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);
        when(previewRepository.findByApplyIdempotencyKeyDetailed("pricing-key")).thenReturn(Optional.empty());
        when(previewRepository.findByPublicIdForApply(previewId)).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectedItemReferencesForApply(1L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of(reference));
        when(itemRepository.findAllById(List.of(item.getId()))).thenReturn(List.of(item));
        when(movieRepository.findAllById(List.of(movie.getId()))).thenReturn(List.of(movie));
        when(movieVersionRepository.findAllById(List.of(version.getId()))).thenReturn(List.of(version));
        when(cinemaRepository.findAllById(List.of(cinema.getId()))).thenReturn(List.of(cinema));
        when(auditoriumRepository.findAllById(List.of(auditorium.getId()))).thenReturn(List.of(auditorium));
        when(pricingPreflightService.evaluate(List.of(item))).thenReturn(
                new AutoSchedulePricingPreflightService.Evaluation(blocked, List.of()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applyService.applyPreview(previewId, request("pricing-key", 1L)));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRICING_INCOMPLETE);
        assertThat(exception.getErrorData()).isSameAs(blocked);
        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.PREVIEWED);
        verify(showtimeCreationService, never()).createAll(any(), anyLong(), anyString(), any());
        verify(itemRepository, never()).findDetailedItemsByPreviewId(anyLong());
    }

    @Test
    void applyPreview_postLockExpiryRollsBackThenNormalizesInSeparateCall() {
        String previewId = "prev-expiring";
        Instant expiresAt = now.plusSeconds(30);
        when(clock.instant()).thenReturn(now, expiresAt, expiresAt);
        ApplyShowtimeSchedulePreviewRequest request = request("expiry-key", 1L);

        Cinema cinema = cinema();
        ShowtimeSchedulePreview preview = preview(previewId, cinema, 1L, expiresAt);
        ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference = reference(10L, 20L, 30L, 40L, 50L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);
        when(previewRepository.findByApplyIdempotencyKeyDetailed("expiry-key")).thenReturn(Optional.empty());
        when(previewRepository.findByPublicIdForApply(previewId)).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectedItemReferencesForApply(1L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of(reference));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applyService.applyPreview(previewId, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
        verify(auditoriumLockService).lockAll(List.of(50L));
        verify(expiryService).expireIfNecessary(previewId, expiresAt);
        verify(entityManager, never()).clear();
        verify(showtimeCreationService, never()).createAll(any(), anyLong(), anyString(), any());
    }

    @Test
    void applyPreview_idempotencyHitReturnsExistingResponse() {
        String previewId = "prev-1";
        String key = "key-1";
        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);

        ShowtimeSchedulePreview preview = preview(previewId, cinema(), 1L, now.plusSeconds(3600));
        ReflectionTestUtils.setField(preview, "status", SchedulePreviewStatus.APPLIED);
        when(previewRepository.findByApplyIdempotencyKeyDetailed(key)).thenReturn(Optional.of(preview));
        ApplyShowtimeSchedulePreviewResponse expected = new ApplyShowtimeSchedulePreviewResponse();
        when(responseMapper.toResponse(preview)).thenReturn(expected);

        assertThat(applyService.applyPreview(previewId, request(key, 1L))).isSameAs(expected);
        verify(previewRepository, never()).findByPublicIdForApply(anyString());
    }

    @Test
    void applyPreview_earlyExpiredThrowsSpecificError() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(expiryService.expireIfNecessary("prev-1", now)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applyService.applyPreview("prev-1", request("key", 1L)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
    }

    @Test
    void doApply_versionMismatchThrowsSpecificError() {
        ShowtimeSchedulePreview preview = preview("prev-1", cinema(), 2L, now.plusSeconds(3600));
        when(previewRepository.findByApplyIdempotencyKeyDetailed("key-1")).thenReturn(Optional.empty());
        when(previewRepository.findByPublicIdForApply("prev-1")).thenReturn(Optional.of(preview));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applyService.doApply("prev-1", "key-1", 1L, 100L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
    }

    @Test
    void applyPreview_dataIntegrityConflictUsesExistingIdempotencyRecovery() {
        AutoSchedulePreviewApplyServiceImpl spyService = spy(applyService);
        ApplyShowtimeSchedulePreviewRequest request = request("key-1", 1L);
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(expiryService.expireIfNecessary("prev-1", now)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(spyService).doApply("prev-1", "key-1", 1L, 1L);

        ShowtimeSchedulePreview conflict = preview("prev-1", cinema(), 1L, now.plusSeconds(3600));
        ReflectionTestUtils.setField(conflict, "status", SchedulePreviewStatus.APPLYING);
        when(previewRepository.findByApplyIdempotencyKeyDetailed("key-1")).thenReturn(Optional.of(conflict));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> spyService.applyPreview("prev-1", request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS);
    }

    @ParameterizedTest(name = "{0} deleted={1} returns {2}")
    @MethodSource("unavailableEntityCases")
    void applyPreview_missingOrDeletedCurrentEntityReturnsSpecificDomainError(
            CurrentEntity target, boolean deleted, ErrorCode expectedError) {
        String previewId = "prev-current-state";
        Cinema cinema = cinema();
        Auditorium auditorium = auditorium(cinema);
        Movie movie = movie();
        MovieVersion version = version(movie);
        ShowtimeSchedulePreview preview = preview(previewId, cinema, 1L, now.plusSeconds(3600));
        ShowtimeSchedulePreviewItem item = item(preview, movie, version, cinema, auditorium);
        ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference =
                reference(item.getId(), movie.getId(), version.getId(), cinema.getId(), auditorium.getId());

        if (deleted) {
            Instant deletedAt = now.minusSeconds(60);
            switch (target) {
                case MOVIE -> movie.setDeletedAt(deletedAt);
                case VERSION -> version.setDeletedAt(deletedAt);
                case CINEMA -> cinema.setDeletedAt(deletedAt);
                case AUDITORIUM -> auditorium.setDeletedAt(deletedAt);
            }
        }

        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);
        when(previewRepository.findByApplyIdempotencyKeyDetailed("current-key")).thenReturn(Optional.empty());
        when(previewRepository.findByPublicIdForApply(previewId)).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectedItemReferencesForApply(1L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of(reference));
        when(itemRepository.findAllById(List.of(item.getId()))).thenReturn(List.of(item));
        when(movieRepository.findAllById(List.of(movie.getId())))
                .thenReturn(target == CurrentEntity.MOVIE && !deleted ? List.of() : List.of(movie));
        when(movieVersionRepository.findAllById(List.of(version.getId())))
                .thenReturn(target == CurrentEntity.VERSION && !deleted ? List.of() : List.of(version));
        when(cinemaRepository.findAllById(List.of(cinema.getId())))
                .thenReturn(target == CurrentEntity.CINEMA && !deleted ? List.of() : List.of(cinema));
        when(auditoriumRepository.findAllById(List.of(auditorium.getId())))
                .thenReturn(target == CurrentEntity.AUDITORIUM && !deleted ? List.of() : List.of(auditorium));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applyService.applyPreview(previewId, request("current-key", 1L)));

        assertThat(ex.getErrorCode()).isEqualTo(expectedError);
        verify(entityManager).clear();
        verify(revalidationService, never()).validateAll(any(), any(), any());
        verify(showtimeCreationService, never()).createAll(any(), anyLong(), anyString(), any());
    }

    @ParameterizedTest(name = "current {0} ownership mismatch returns {1}")
    @MethodSource("ownershipMismatchCases")
    void applyPreview_revalidatesCurrentOwnershipLinks(
            OwnershipTarget target, ErrorCode expectedError) {
        String previewId = "prev-current-ownership";
        Cinema cinema = cinema();
        Auditorium auditorium = auditorium(cinema);
        Movie movie = movie();
        MovieVersion version = version(movie);
        ShowtimeSchedulePreview preview = preview(previewId, cinema, 1L, now.plusSeconds(3600));
        ShowtimeSchedulePreviewItem item = item(preview, movie, version, cinema, auditorium);
        ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference =
                reference(item.getId(), movie.getId(), version.getId(), cinema.getId(), auditorium.getId());

        if (target == OwnershipTarget.VERSION) {
            Movie otherMovie = movie();
            otherMovie.setId(999L);
            version.setMovie(otherMovie);
        } else {
            Cinema otherCinema = cinema();
            otherCinema.setId(999L);
            auditorium.setCinema(otherCinema);
        }

        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);
        when(previewRepository.findByApplyIdempotencyKeyDetailed("ownership-key")).thenReturn(Optional.empty());
        when(previewRepository.findByPublicIdForApply(previewId)).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectedItemReferencesForApply(1L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of(reference));
        when(itemRepository.findAllById(List.of(item.getId()))).thenReturn(List.of(item));
        when(movieRepository.findAllById(List.of(movie.getId()))).thenReturn(List.of(movie));
        when(movieVersionRepository.findAllById(List.of(version.getId()))).thenReturn(List.of(version));
        when(cinemaRepository.findAllById(List.of(cinema.getId()))).thenReturn(List.of(cinema));
        when(auditoriumRepository.findAllById(List.of(auditorium.getId()))).thenReturn(List.of(auditorium));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applyService.applyPreview(previewId, request("ownership-key", 1L)));

        assertThat(ex.getErrorCode()).isEqualTo(expectedError);
        verify(revalidationService, never()).validateAll(any(), any(), any());
        verify(showtimeCreationService, never()).createAll(any(), anyLong(), anyString(), any());
    }

    @ParameterizedTest(name = "current {0} change is revalidated as {1}")
    @MethodSource("authoritativeValidationChangeCases")
    void applyPreview_rejectsLifecycleAndTimezoneChangesFromFreshState(
            AuthoritativeChange target, ErrorCode expectedError) {
        String previewId = "prev-current-validation";
        Cinema currentCinema = cinema();
        Auditorium currentAuditorium = auditorium(currentCinema);
        Movie currentMovie = movie();
        MovieVersion currentVersion = version(currentMovie);
        ShowtimeSchedulePreview preview = preview(previewId, currentCinema, 1L, now.plusSeconds(3600));
        ShowtimeSchedulePreviewItem item = item(
                preview, movie(), version(movie()), cinema(), auditorium(cinema()));
        ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference = reference(
                item.getId(), currentMovie.getId(), currentVersion.getId(),
                currentCinema.getId(), currentAuditorium.getId());

        if (target == AuthoritativeChange.MOVIE_LIFECYCLE) {
            currentMovie.setStatus(MovieStatus.DRAFT);
        } else {
            currentCinema.setTimezone("Invalid/Current-Cinema-Zone");
        }

        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);
        when(previewRepository.findByApplyIdempotencyKeyDetailed("validation-key")).thenReturn(Optional.empty());
        when(previewRepository.findByPublicIdForApply(previewId)).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectedItemReferencesForApply(1L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of(reference));
        when(itemRepository.findAllById(List.of(item.getId()))).thenReturn(List.of(item));
        when(movieRepository.findAllById(List.of(currentMovie.getId()))).thenReturn(List.of(currentMovie));
        when(movieVersionRepository.findAllById(List.of(currentVersion.getId()))).thenReturn(List.of(currentVersion));
        when(cinemaRepository.findAllById(List.of(currentCinema.getId()))).thenReturn(List.of(currentCinema));
        when(auditoriumRepository.findAllById(List.of(currentAuditorium.getId())))
                .thenReturn(List.of(currentAuditorium));
        doThrow(new BusinessException(expectedError))
                .when(revalidationService).validateAll(any(), any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> applyService.applyPreview(previewId, request("validation-key", 1L)));

        assertThat(ex.getErrorCode()).isEqualTo(expectedError);
        assertThat(item.getMovie()).isSameAs(currentMovie);
        assertThat(item.getCinema()).isSameAs(currentCinema);
        verify(entityManager).clear();
        verify(showtimeCreationService, never()).createAll(any(), anyLong(), anyString(), any());
    }

    private static Stream<Arguments> unavailableEntityCases() {
        return Stream.of(
                Arguments.of(CurrentEntity.MOVIE, false, ErrorCode.MOVIE_NOT_FOUND),
                Arguments.of(CurrentEntity.MOVIE, true, ErrorCode.MOVIE_NOT_FOUND),
                Arguments.of(CurrentEntity.VERSION, false, ErrorCode.MOVIE_VERSION_NOT_FOUND),
                Arguments.of(CurrentEntity.VERSION, true, ErrorCode.MOVIE_VERSION_NOT_FOUND),
                Arguments.of(CurrentEntity.CINEMA, false, ErrorCode.CINEMA_NOT_FOUND),
                Arguments.of(CurrentEntity.CINEMA, true, ErrorCode.CINEMA_NOT_FOUND),
                Arguments.of(CurrentEntity.AUDITORIUM, false, ErrorCode.AUDITORIUM_NOT_FOUND),
                Arguments.of(CurrentEntity.AUDITORIUM, true, ErrorCode.AUDITORIUM_NOT_FOUND));
    }

    private static Stream<Arguments> ownershipMismatchCases() {
        return Stream.of(
                Arguments.of(OwnershipTarget.VERSION, ErrorCode.MOVIE_VERSION_NOT_BELONG_TO_MOVIE),
                Arguments.of(OwnershipTarget.AUDITORIUM, ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA));
    }

    private static Stream<Arguments> authoritativeValidationChangeCases() {
        return Stream.of(
                Arguments.of(
                        AuthoritativeChange.MOVIE_LIFECYCLE,
                        ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING),
                Arguments.of(
                        AuthoritativeChange.CINEMA_TIMEZONE,
                        ErrorCode.INVALID_CINEMA_TIMEZONE));
    }

    private ApplyShowtimeSchedulePreviewRequest request(String key, Long version) {
        ApplyShowtimeSchedulePreviewRequest request = new ApplyShowtimeSchedulePreviewRequest();
        request.setIdempotencyKey(key);
        request.setExpectedVersion(version);
        return request;
    }

    private ShowtimeSchedulePreview preview(String publicId, Cinema cinema, Long version, Instant expiresAt) {
        ShowtimeSchedulePreview preview = ShowtimeSchedulePreview.createGenerating(
                cinema, LocalDate.of(2026, 7, 22), LocalDate.of(2026, 7, 22),
                15, 60, AutoScheduleStrategyVersions.BALANCED_V1_S5,
                "generate-key", "fingerprint", 1L, now);
        ReflectionTestUtils.setField(preview, "id", 1L);
        ReflectionTestUtils.setField(preview, "publicId", publicId);
        ReflectionTestUtils.setField(preview, "version", version);
        ReflectionTestUtils.setField(preview, "applyMode", SchedulePreviewApplyMode.ALL_OR_NOTHING);
        ReflectionTestUtils.setField(preview, "status", SchedulePreviewStatus.PREVIEWED);
        ReflectionTestUtils.setField(preview, "expiresAt", expiresAt);
        ReflectionTestUtils.setField(preview, "selectedCandidateCount", 1);
        return preview;
    }

    private ShowtimeSchedulePreviewItem item(ShowtimeSchedulePreview preview,
                                             Movie movie,
                                             MovieVersion version,
                                             Cinema cinema,
                                             Auditorium auditorium) {
        ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
        ReflectionTestUtils.setField(item, "id", 10L);
        item.setPublicId("item-10");
        item.setPreview(preview);
        item.setMovie(movie);
        item.setMovieVersion(version);
        item.setCinema(cinema);
        item.setAuditorium(auditorium);
        item.setApplyStatus(PreviewItemApplyStatus.PENDING);
        return item;
    }

    private ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference(
            Long itemId, Long movieId, Long versionId, Long cinemaId, Long auditoriumId) {
        ShowtimeSchedulePreviewItemRepository.ApplyItemReference reference =
                mock(ShowtimeSchedulePreviewItemRepository.ApplyItemReference.class);
        lenient().when(reference.getItemId()).thenReturn(itemId);
        lenient().when(reference.getMovieId()).thenReturn(movieId);
        lenient().when(reference.getMovieVersionId()).thenReturn(versionId);
        lenient().when(reference.getCinemaId()).thenReturn(cinemaId);
        lenient().when(reference.getAuditoriumId()).thenReturn(auditoriumId);
        return reference;
    }

    private Cinema cinema() {
        Cinema cinema = new Cinema();
        cinema.setId(40L);
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        return cinema;
    }

    private Auditorium auditorium(Cinema cinema) {
        Auditorium auditorium = new Auditorium();
        auditorium.setId(50L);
        auditorium.setCinema(cinema);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setCleaningBufferMinutes(15);
        return auditorium;
    }

    private Movie movie() {
        Movie movie = new Movie();
        movie.setId(20L);
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(90);
        return movie;
    }

    private MovieVersion version(Movie movie) {
        MovieVersion version = new MovieVersion();
        version.setId(30L);
        version.setMovie(movie);
        version.setStatus(ActiveStatus.ACTIVE);
        return version;
    }

    private enum CurrentEntity {
        MOVIE,
        VERSION,
        CINEMA,
        AUDITORIUM
    }

    private enum OwnershipTarget {
        VERSION,
        AUDITORIUM
    }

    private enum AuthoritativeChange {
        MOVIE_LIFECYCLE,
        CINEMA_TIMEZONE
    }
}
