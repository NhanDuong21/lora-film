package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.request.CancelShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewPageResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.repository.SelectionItemSnapshot;
import com.lorafilm.movie.autoschedule.service.impl.ShowtimeSchedulePreviewServiceImpl;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShowtimeSchedulePreviewServiceTest {

    @Mock
    private ShowtimeSchedulePreviewRepository previewRepository;
    @Mock
    private ShowtimeSchedulePreviewItemRepository itemRepository;
    @Mock
    private ShowtimeSchedulePreviewMapper mapper;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private com.lorafilm.movie.autoschedule.service.impl.ShowtimeSchedulePreviewExpiryService expiryService;
    private Clock clock = Clock.fixed(Instant.parse("2026-07-20T03:00:00Z"), ZoneId.of("UTC"));

    private ShowtimeSchedulePreviewService service;

    private ShowtimeSchedulePreview preview;
    @BeforeEach
    void setUp() throws Exception {
        service = new ShowtimeSchedulePreviewServiceImpl(
                previewRepository,
                itemRepository,
                mapper,
                currentUserProvider,
                expiryService,
                clock
        );

        java.lang.reflect.Constructor<ShowtimeSchedulePreview> previewConstructor = ShowtimeSchedulePreview.class.getDeclaredConstructor();
        previewConstructor.setAccessible(true);
        preview = previewConstructor.newInstance();
        ReflectionTestUtils.setField(preview, "id", 1L);
        preview.setPublicId("preview-1");
        preview.setStatus(SchedulePreviewStatus.PREVIEWED);
        preview.setExpiresAt(Instant.parse("2026-07-20T04:00:00Z")); // Not expired relative to clock
        
    }

    // --- GET PREVIEW TESTS ---

    @Test
    void getPreview_shouldReturnPreview_whenValid() {
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));
        org.springframework.data.domain.Page<ShowtimeSchedulePreviewItem> emptyPage = org.springframework.data.domain.Page.empty();
        when(itemRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(emptyPage);
        when(mapper.toPageResponse(any(), any())).thenReturn(new ShowtimeSchedulePreviewPageResponse());

        com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery query = new com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery();
        ShowtimeSchedulePreviewPageResponse response = service.getPreview("preview-1", query);

        assertThat(response).isNotNull();
        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.PREVIEWED);
    }

    @Test
    void getPreview_shouldThrowException_whenNotFound() {
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.empty());

        com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery query = new com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery();
        assertThatThrownBy(() -> service.getPreview("preview-1", query))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND);
    }

    @Test
    void getPreview_shouldNormalizeExpiry_whenExpiredBoundary() {
        preview.setExpiresAt(Instant.parse("2026-07-20T03:00:00Z")); // Exact match with clock now
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));
        
        doAnswer(invocation -> {
            preview.setStatus(SchedulePreviewStatus.EXPIRED);
            return true;
        }).when(expiryService).expireIfNecessary(eq("preview-1"), any(Instant.class));

        com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery query = new com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery();
        service.getPreview("preview-1", query);

        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.EXPIRED);
    }

    @Test
    void getPreview_shouldNotNormalizeExpiry_whenStatusIsApplied() {
        preview.setStatus(SchedulePreviewStatus.APPLIED);
        preview.setExpiresAt(Instant.parse("2026-07-20T02:00:00Z")); // In the past
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));

        com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery query = new com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery();
        service.getPreview("preview-1", query);

        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.APPLIED);
    }

    // --- PUT SELECTION TESTS ---

    @Test
    void cancelPreview_shouldCancelOnlyCurrentPreviewVersion() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        CancelShowtimeSchedulePreviewRequest request = new CancelShowtimeSchedulePreviewRequest();
        request.setExpectedVersion(2L);
        ShowtimeSchedulePreviewSummaryResponse mapped = new ShowtimeSchedulePreviewSummaryResponse();
        mapped.setStatus(SchedulePreviewStatus.CANCELLED);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdForUpdate("preview-1")).thenReturn(Optional.of(preview));
        when(previewRepository.saveAndFlush(preview)).thenReturn(preview);
        when(mapper.toSummaryResponse(preview)).thenReturn(mapped);

        ShowtimeSchedulePreviewSummaryResponse response = service.cancelPreview("preview-1", request);

        assertThat(response.getStatus()).isEqualTo(SchedulePreviewStatus.CANCELLED);
        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.CANCELLED);
        verify(previewRepository).saveAndFlush(preview);
    }

    @Test
    void cancelPreview_shouldRejectAppliedPreviewWithoutChangingIt() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        preview.setStatus(SchedulePreviewStatus.APPLIED);
        CancelShowtimeSchedulePreviewRequest request = new CancelShowtimeSchedulePreviewRequest();
        request.setExpectedVersion(2L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdForUpdate("preview-1")).thenReturn(Optional.of(preview));

        assertThatThrownBy(() -> service.cancelPreview("preview-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_CANNOT_BE_CANCELLED);
        verify(previewRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateSelections_shouldValidateThenBulkUpdateAndIncrementVersion_whenHappyPath() throws Exception {
        java.lang.reflect.Field versionField = ShowtimeSchedulePreview.class.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(preview, 2L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("item-1")))
                .thenReturn(List.of(snapshot(1L, "item-1", 1L, 1L, false,
                        PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                        "10:00:00", "11:30:00", "11:45:00")));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of());
        when(previewRepository.compareAndSetSelectionVersion(
                1L, SchedulePreviewStatus.PREVIEWED, 2L, 1, Instant.parse("2026-07-20T03:00:00Z")))
                .thenReturn(1);
        when(itemRepository.updateSelectionState(
                1L, List.of(1L), false, true, Instant.parse("2026-07-20T03:00:00Z"), 99L))
                .thenReturn(1);

        UpdatePreviewItemSelectionRequest itemReq1 = new UpdatePreviewItemSelectionRequest();
        itemReq1.setItemPublicId("item-1");
        itemReq1.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(2L);
        request.setItems(List.of(itemReq1));

        service.updateSelections("preview-1", request);

        verify(previewRepository).compareAndSetSelectionVersion(
                1L, SchedulePreviewStatus.PREVIEWED, 2L, 1, Instant.parse("2026-07-20T03:00:00Z"));
        verify(itemRepository).updateSelectionState(
                1L, List.of(1L), false, true, Instant.parse("2026-07-20T03:00:00Z"), 99L);
    }

    @Test
    void updateSelections_shouldThrowException_whenVersionMismatch() throws Exception {
        java.lang.reflect.Field versionField = ShowtimeSchedulePreview.class.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(preview, 2L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));

        UpdatePreviewItemSelectionRequest itemReq1 = new UpdatePreviewItemSelectionRequest();
        itemReq1.setItemPublicId("item-1");
        itemReq1.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(1L); // Mismatch
        request.setItems(List.of(itemReq1));

        assertThatThrownBy(() -> service.updateSelections("preview-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
    }

    @Test
    void updateSelections_shouldThrowException_whenDuplicateItems() {
        UpdatePreviewItemSelectionRequest itemReq1 = new UpdatePreviewItemSelectionRequest();
        itemReq1.setItemPublicId("item-1");
        itemReq1.setSelected(true);

        UpdatePreviewItemSelectionRequest itemReq2 = new UpdatePreviewItemSelectionRequest();
        itemReq2.setItemPublicId("item-1");
        itemReq2.setSelected(false);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(null);
        request.setItems(List.of(itemReq1, itemReq2));

        assertThatThrownBy(() -> service.updateSelections("preview-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_DUPLICATE_ITEM_SELECTION);
    }

    @Test
    void updateSelections_shouldThrowException_whenItemBelongsToDifferentPreview() throws Exception {
        java.lang.reflect.Field versionField = ShowtimeSchedulePreview.class.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(preview, 2L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("item-1")))
                .thenReturn(List.of(snapshot(1L, "item-1", 2L, 1L, false,
                        PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                        "10:00:00", "11:30:00", "11:45:00")));

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest(
                2L,
                List.of(new UpdatePreviewItemSelectionRequest("item-1", true))
        );

        assertThatThrownBy(() -> service.updateSelections("preview-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_ITEM_NOT_BELONG_TO_PREVIEW);
    }

    @Test
    void updateSelections_shouldThrowException_whenItemRejectedAndSelectedTrue() throws Exception {
        java.lang.reflect.Field versionField = ShowtimeSchedulePreview.class.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(preview, 2L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("item-1")))
                .thenReturn(List.of(snapshot(1L, "item-1", 1L, 1L, false,
                        PreviewItemValidationStatus.REJECTED, PreviewItemApplyStatus.PENDING,
                        "10:00:00", "11:30:00", "11:45:00")));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of());

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest(
                2L,
                List.of(new UpdatePreviewItemSelectionRequest("item-1", true))
        );

        assertThatThrownBy(() -> service.updateSelections("preview-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_REJECTED_ITEM_CANNOT_BE_SELECTED);
    }

    @Test
    void updateSelections_shouldRejectOverlapAgainstUnmentionedSelectedItemWithoutWriting() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        SelectionItemSnapshot retained = snapshot(1L, "retained", 1L, 1L, true,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "10:00:00", "11:00:00", "11:15:00");
        SelectionItemSnapshot addition = snapshot(2L, "item-1", 1L, 1L, false,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "11:05:00", "12:05:00", "12:20:00");
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("item-1"))).thenReturn(List.of(addition));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of(retained));

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest(
                2L, List.of(new UpdatePreviewItemSelectionRequest("item-1", true)));

        assertThatThrownBy(() -> service.updateSelections("preview-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_SELECTION_OVERLAP);
        verify(previewRepository, never()).compareAndSetSelectionVersion(any(), any(), any(), any(), any());
        verify(itemRepository, never()).updateSelectionState(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateSelections_shouldAllowExactAdjacency() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        SelectionItemSnapshot retained = snapshot(1L, "retained", 1L, 1L, true,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "10:00:00", "11:00:00", "11:15:00");
        SelectionItemSnapshot addition = snapshot(2L, "item-1", 1L, 1L, false,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "11:15:00", "12:15:00", "12:30:00");
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("item-1"))).thenReturn(List.of(addition));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of(retained));
        when(previewRepository.compareAndSetSelectionVersion(any(), any(), any(), any(), any())).thenReturn(1);
        when(itemRepository.updateSelectionState(any(), any(), any(), any(), any(), any())).thenReturn(1);

        service.updateSelections("preview-1", new UpdatePreviewItemSelectionsRequest(
                2L, List.of(new UpdatePreviewItemSelectionRequest("item-1", true))));

        verify(previewRepository).compareAndSetSelectionVersion(
                1L, SchedulePreviewStatus.PREVIEWED, 2L, 2, Instant.parse("2026-07-20T03:00:00Z"));
    }

    @Test
    void updateSelections_shouldKeepVersionForValidatedNoOp() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        SelectionItemSnapshot selected = snapshot(1L, "item-1", 1L, 1L, true,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "10:00:00", "11:00:00", "11:15:00");
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("item-1"))).thenReturn(List.of(selected));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of(selected));

        service.updateSelections("preview-1", new UpdatePreviewItemSelectionsRequest(
                2L, List.of(new UpdatePreviewItemSelectionRequest("item-1", true))));

        verify(previewRepository, never()).compareAndSetSelectionVersion(any(), any(), any(), any(), any());
        verify(itemRepository, never()).updateSelectionState(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateSelections_shouldRejectTwoNewOverlappingSelections() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        SelectionItemSnapshot first = snapshot(1L, "first", 1L, 1L, false,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "10:00:00", "11:00:00", "11:15:00");
        SelectionItemSnapshot second = snapshot(2L, "second", 1L, 1L, false,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "11:05:00", "12:05:00", "12:20:00");
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("first", "second")))
                .thenReturn(List.of(first, second));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of());

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest(2L, List.of(
                new UpdatePreviewItemSelectionRequest("first", true),
                new UpdatePreviewItemSelectionRequest("second", true)));

        assertThatThrownBy(() -> service.updateSelections("preview-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_SELECTION_OVERLAP);
        verify(previewRepository, never()).compareAndSetSelectionVersion(any(), any(), any(), any(), any());
    }

    @Test
    void updateSelections_shouldAllowSameTimesInDifferentAuditoriums() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        SelectionItemSnapshot first = snapshot(1L, "first", 1L, 1L, false,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "10:00:00", "11:00:00", "11:15:00");
        SelectionItemSnapshot second = snapshot(2L, "second", 1L, 2L, false,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "10:00:00", "11:00:00", "11:15:00");
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("first", "second")))
                .thenReturn(List.of(first, second));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of());
        when(previewRepository.compareAndSetSelectionVersion(any(), any(), any(), any(), any())).thenReturn(1);
        when(itemRepository.updateSelectionState(any(), any(), any(), any(), any(), any())).thenReturn(2);

        service.updateSelections("preview-1", new UpdatePreviewItemSelectionsRequest(2L, List.of(
                new UpdatePreviewItemSelectionRequest("first", true),
                new UpdatePreviewItemSelectionRequest("second", true))));

        verify(previewRepository).compareAndSetSelectionVersion(
                1L, SchedulePreviewStatus.PREVIEWED, 2L, 2, Instant.parse("2026-07-20T03:00:00Z"));
    }

    @Test
    void updateSelections_shouldRejectNonPendingSelection() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        SelectionItemSnapshot applied = snapshot(1L, "item-1", 1L, 1L, false,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.CREATED,
                "10:00:00", "11:00:00", "11:15:00");
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("item-1"))).thenReturn(List.of(applied));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateSelections("preview-1",
                new UpdatePreviewItemSelectionsRequest(2L, List.of(
                        new UpdatePreviewItemSelectionRequest("item-1", true)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_INVALID_ITEM_SELECTION);
        verify(previewRepository, never()).compareAndSetSelectionVersion(any(), any(), any(), any(), any());
    }

    @Test
    void updateSelections_shouldBlockAdditionBesideRetainedMalformedLegacyItem() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        SelectionItemSnapshot malformed = new SelectionItemSnapshot(
                1L, "legacy", 1L, 1L,
                Instant.parse("2026-07-20T10:00:00Z"),
                Instant.parse("2026-07-20T11:00:00Z"),
                null,
                PreviewItemValidationStatus.VALID, true, PreviewItemApplyStatus.PENDING);
        SelectionItemSnapshot addition = snapshot(2L, "item-1", 1L, 1L, false,
                PreviewItemValidationStatus.VALID, PreviewItemApplyStatus.PENDING,
                "12:00:00", "13:00:00", "13:15:00");
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("item-1"))).thenReturn(List.of(addition));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of(malformed));

        assertThatThrownBy(() -> service.updateSelections("preview-1",
                new UpdatePreviewItemSelectionsRequest(2L, List.of(
                        new UpdatePreviewItemSelectionRequest("item-1", true)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_INVALID_ITEM_SELECTION);
    }

    @Test
    void updateSelections_shouldAllowMalformedLegacyItemToBeDeselected() throws Exception {
        ReflectionTestUtils.setField(preview, "version", 2L);
        SelectionItemSnapshot malformed = new SelectionItemSnapshot(
                1L, "legacy", 1L, null,
                Instant.parse("2026-07-20T10:00:00Z"),
                Instant.parse("2026-07-20T11:00:00Z"),
                null,
                PreviewItemValidationStatus.VALID, true, PreviewItemApplyStatus.CREATED);
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectionSnapshotsByPublicIdIn(Set.of("legacy"))).thenReturn(List.of(malformed));
        when(itemRepository.findSelectedSelectionSnapshots(1L)).thenReturn(List.of(malformed));
        when(previewRepository.compareAndSetSelectionVersion(any(), any(), any(), any(), any())).thenReturn(1);
        when(itemRepository.updateSelectionState(any(), any(), any(), any(), any(), any())).thenReturn(1);

        service.updateSelections("preview-1", new UpdatePreviewItemSelectionsRequest(2L, List.of(
                new UpdatePreviewItemSelectionRequest("legacy", false))));

        verify(itemRepository).updateSelectionState(
                1L, List.of(1L), true, false, Instant.parse("2026-07-20T03:00:00Z"), 99L);
    }

    private SelectionItemSnapshot snapshot(Long itemId,
                                           String publicId,
                                           Long previewId,
                                           Long auditoriumId,
                                           boolean selected,
                                           PreviewItemValidationStatus validationStatus,
                                           PreviewItemApplyStatus applyStatus,
                                           String start,
                                           String end,
                                           String occupancyEnd) {
        return new SelectionItemSnapshot(
                itemId,
                publicId,
                previewId,
                auditoriumId,
                Instant.parse("2026-07-20T" + start + "Z"),
                Instant.parse("2026-07-20T" + end + "Z"),
                Instant.parse("2026-07-20T" + occupancyEnd + "Z"),
                validationStatus,
                selected,
                applyStatus);
    }
}
