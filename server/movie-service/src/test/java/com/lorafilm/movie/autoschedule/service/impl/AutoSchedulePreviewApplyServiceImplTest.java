package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.ApplyShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.mapper.AutoScheduleApplyResponseMapper;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleApplyRevalidationService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleAuditoriumLockService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleShowtimeCreationService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePreviewApplyServiceImplTest {

    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private ShowtimeSchedulePreviewExpiryService expiryService;
    @Mock
    private ShowtimeSchedulePreviewRepository previewRepository;
    @Mock
    private ShowtimeSchedulePreviewItemRepository itemRepository;
    @Mock
    private AutoScheduleAuditoriumLockService auditoriumLockService;
    @Mock
    private AutoScheduleApplyRevalidationService revalidationService;
    @Mock
    private AutoScheduleShowtimeCreationService showtimeCreationService;
    @Mock
    private AutoScheduleApplyResponseMapper responseMapper;

    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @InjectMocks
    private AutoSchedulePreviewApplyServiceImpl applyService;

    private Clock clock;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        transactionTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager) {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                return action.doInTransaction(new org.springframework.transaction.support.SimpleTransactionStatus());
            }
        };
        applyService = new AutoSchedulePreviewApplyServiceImpl(
                currentUserProvider, expiryService, previewRepository, itemRepository,
                auditoriumLockService, revalidationService, showtimeCreationService,
                responseMapper, clock, transactionTemplate
        );
    }

    @Test
    void applyPreview_success() {
        // Arrange
        String previewId = "prev-1";
        String idempotencyKey = "key-1";
        Long actorId = 100L;

        ApplyShowtimeSchedulePreviewRequest req = new ApplyShowtimeSchedulePreviewRequest();
        req.setExpectedVersion(1L);
        req.setIdempotencyKey(idempotencyKey);

        when(currentUserProvider.getCurrentUserId()).thenReturn(actorId);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);
        when(previewRepository.findByApplyIdempotencyKeyDetailed(idempotencyKey)).thenReturn(Optional.empty());

        ShowtimeSchedulePreview preview = org.springframework.test.util.ReflectionTestUtils.invokeMethod(ShowtimeSchedulePreview.class, "createGenerating", 
            new Cinema(), java.time.LocalDate.now(), java.time.LocalDate.now(), 30, 60, "key", "fp", 1L, Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "publicId", previewId);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "version", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "applyMode", SchedulePreviewApplyMode.ALL_OR_NOTHING);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "status", SchedulePreviewStatus.PREVIEWED);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "expiresAt", now.plusSeconds(3600));
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "selectedCandidateCount", 1);

        when(previewRepository.findByPublicIdForApply(previewId)).thenReturn(Optional.of(preview));

        com.lorafilm.movie.autoschedule.model.ShowtimeCandidate mockCandidate = new com.lorafilm.movie.autoschedule.model.ShowtimeCandidate();
        ShowtimeSchedulePreviewItem item = ShowtimeSchedulePreviewItem.createItem(preview, mockCandidate);
        org.springframework.test.util.ReflectionTestUtils.setField(item, "id", 10L);
        Auditorium aud = new Auditorium();
        aud.setId(5L);
        item.setAuditorium(aud);

        when(itemRepository.findSelectedItemsForApply(1L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of(item));

        when(showtimeCreationService.createAll(any(), anyLong(), anyString())).thenReturn(Arrays.asList(new Showtime(), new Showtime()));
        when(itemRepository.findDetailedItemsByPreviewId(1L)).thenReturn(List.of(item));

        ApplyShowtimeSchedulePreviewResponse responseDto = new ApplyShowtimeSchedulePreviewResponse();
        when(responseMapper.toResponse(preview)).thenReturn(responseDto);

        // Act
        ApplyShowtimeSchedulePreviewResponse result = applyService.applyPreview(previewId, req);

        // Assert
        assertThat(result).isNotNull();
        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.APPLIED);
        assertThat(preview.getAppliedBy()).isEqualTo(actorId);
        assertThat(item.getApplyStatus()).isEqualTo(PreviewItemApplyStatus.CREATED);

        verify(auditoriumLockService).lockAll(List.of(5L));
        verify(revalidationService).validateAll(preview, List.of(item), now);
    }

    @Test
    void applyPreview_idempotencyHit_returnsExistingResponse() {
        String previewId = "prev-1";
        String idempotencyKey = "key-1";
        Long actorId = 100L;

        ApplyShowtimeSchedulePreviewRequest req = new ApplyShowtimeSchedulePreviewRequest();
        req.setIdempotencyKey(idempotencyKey);
        req.setExpectedVersion(1L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(actorId);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);

        ShowtimeSchedulePreview preview = org.springframework.test.util.ReflectionTestUtils.invokeMethod(ShowtimeSchedulePreview.class, "createGenerating", 
            new Cinema(), java.time.LocalDate.now(), java.time.LocalDate.now(), 30, 60, "key", "fp", 1L, Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "publicId", previewId);
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "status", SchedulePreviewStatus.APPLIED);

        when(previewRepository.findByApplyIdempotencyKeyDetailed(idempotencyKey))
                .thenReturn(Optional.of(preview));

        ApplyShowtimeSchedulePreviewResponse mockResponse = new ApplyShowtimeSchedulePreviewResponse();
        when(responseMapper.toResponse(preview)).thenReturn(mockResponse);

        ApplyShowtimeSchedulePreviewResponse response = applyService.applyPreview(previewId, req);
        assertThat(response).isSameAs(mockResponse);

        verify(previewRepository, never()).findByPublicIdForApply(anyString());
    }

    @Test
    void applyPreview_expired_throwsException() {
        String previewId = "prev-1";
        ApplyShowtimeSchedulePreviewRequest req = new ApplyShowtimeSchedulePreviewRequest();
        req.setIdempotencyKey("k");

        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> applyService.applyPreview(previewId, req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
    }

    @Test
    void doApply_versionMismatch_throwsException() {
        String previewId = "prev-1";
        String applyKey = "key-1";
        Long actorId = 100L;
        Long expectedVersion = 1L;

        when(previewRepository.findByApplyIdempotencyKeyDetailed(applyKey)).thenReturn(Optional.empty());

        ShowtimeSchedulePreview preview = org.springframework.test.util.ReflectionTestUtils.invokeMethod(ShowtimeSchedulePreview.class, "createGenerating", 
            new Cinema(), java.time.LocalDate.now(), java.time.LocalDate.now(), 30, 60, "key", "fp", 1L, Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(preview, "version", 2L);

        when(previewRepository.findByPublicIdForApply(previewId)).thenReturn(Optional.of(preview));

        BusinessException ex = assertThrows(BusinessException.class, () -> 
                applyService.doApply(previewId, applyKey, expectedVersion, actorId, now));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
    }

    @Test
    void doApply_conflictIdempotencyKey_recoversAndHandles() {
        // Test that if doApply throws DataIntegrityViolationException, the applyPreview catches it and handles idempotency conflict
        String previewId = "prev-1";
        String idempotencyKey = "key-1";
        
        ApplyShowtimeSchedulePreviewRequest req = new ApplyShowtimeSchedulePreviewRequest();
        req.setIdempotencyKey(idempotencyKey);
        req.setExpectedVersion(1L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(expiryService.expireIfNecessary(previewId, now)).thenReturn(false);
        
        // Setup mock to throw when called directly, and spy the object
        AutoSchedulePreviewApplyServiceImpl spyService = spy(applyService);
        
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(spyService).doApply(previewId, idempotencyKey, 1L, 1L, now);

        ShowtimeSchedulePreview conflictPreview = org.springframework.test.util.ReflectionTestUtils.invokeMethod(ShowtimeSchedulePreview.class, "createGenerating", 
            new Cinema(), java.time.LocalDate.now(), java.time.LocalDate.now(), 30, 60, "key", "fp", 1L, Instant.now());
        org.springframework.test.util.ReflectionTestUtils.setField(conflictPreview, "publicId", previewId);
        org.springframework.test.util.ReflectionTestUtils.setField(conflictPreview, "status", SchedulePreviewStatus.APPLYING);

        when(previewRepository.findByApplyIdempotencyKeyDetailed(idempotencyKey))
                .thenReturn(Optional.of(conflictPreview));

        BusinessException ex = assertThrows(BusinessException.class, () -> spyService.applyPreview(previewId, req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_APPLY_IN_PROGRESS);
    }
}
