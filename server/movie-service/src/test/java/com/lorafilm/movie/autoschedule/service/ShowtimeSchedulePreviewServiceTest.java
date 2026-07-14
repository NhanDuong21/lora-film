package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionRequest;
import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
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

    private Clock clock = Clock.fixed(Instant.parse("2026-07-20T03:00:00Z"), ZoneId.of("UTC"));

    private ShowtimeSchedulePreviewService service;

    private ShowtimeSchedulePreview preview;
    private ShowtimeSchedulePreviewItem item1;

    @BeforeEach
    void setUp() throws Exception {
        service = new ShowtimeSchedulePreviewServiceImpl(
                previewRepository,
                itemRepository,
                mapper,
                currentUserProvider,
                clock
        );

        java.lang.reflect.Constructor<ShowtimeSchedulePreview> previewConstructor = ShowtimeSchedulePreview.class.getDeclaredConstructor();
        previewConstructor.setAccessible(true);
        preview = previewConstructor.newInstance();
        ReflectionTestUtils.setField(preview, "id", 1L);
        preview.setPublicId("preview-1");
        preview.setStatus(SchedulePreviewStatus.PREVIEWED);
        preview.setExpiresAt(Instant.parse("2026-07-20T04:00:00Z")); // Not expired relative to clock
        
        java.lang.reflect.Constructor<ShowtimeSchedulePreviewItem> itemConstructor = ShowtimeSchedulePreviewItem.class.getDeclaredConstructor();
        itemConstructor.setAccessible(true);
        item1 = itemConstructor.newInstance();
        item1.setPublicId("item-1");
        item1.setValidationStatus(PreviewItemValidationStatus.VALID);
        item1.setSelected(false);
        item1.setPreview(preview);
    }

    // --- GET PREVIEW TESTS ---

    @Test
    void getPreview_shouldReturnPreview_whenValid() {
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findDetailedItemsByPreviewId(1L)).thenReturn(List.of(item1));
        when(mapper.toResponse(any(), any())).thenReturn(new ShowtimeSchedulePreviewResponse());

        ShowtimeSchedulePreviewResponse response = service.getPreview("preview-1");

        assertThat(response).isNotNull();
        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.PREVIEWED);
    }

    @Test
    void getPreview_shouldThrowException_whenNotFound() {
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPreview("preview-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND);
    }

    @Test
    void getPreview_shouldNormalizeExpiry_whenExpiredBoundary() {
        preview.setExpiresAt(Instant.parse("2026-07-20T03:00:00Z")); // Exact match with clock now
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));

        service.getPreview("preview-1");

        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.EXPIRED);
    }

    @Test
    void getPreview_shouldNotNormalizeExpiry_whenStatusIsApplied() {
        preview.setStatus(SchedulePreviewStatus.APPLIED);
        preview.setExpiresAt(Instant.parse("2026-07-20T02:00:00Z")); // In the past
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));

        service.getPreview("preview-1");

        assertThat(preview.getStatus()).isEqualTo(SchedulePreviewStatus.APPLIED);
    }

    // --- PUT SELECTION TESTS ---

    @Test
    void updateSelections_shouldUpdateAndRecalculate_whenHappyPath() throws Exception {
        java.lang.reflect.Field versionField = ShowtimeSchedulePreview.class.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(preview, 2L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findAllByPublicIdIn(Set.of("item-1"))).thenReturn(List.of(item1));
        when(itemRepository.countByPreviewIdAndSelectedTrueAndValidationStatus(1L, PreviewItemValidationStatus.VALID)).thenReturn(5L);

        UpdatePreviewItemSelectionRequest itemReq1 = new UpdatePreviewItemSelectionRequest();
        itemReq1.setItemPublicId("item-1");
        itemReq1.setSelected(true);

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest();
        request.setExpectedVersion(2L);
        request.setItems(List.of(itemReq1));

        service.updateSelections("preview-1", request);

        assertThat(item1.getSelected()).isTrue();
        assertThat(item1.getSelectedBy()).isEqualTo(99L);
        assertThat(item1.getSelectedAt()).isEqualTo(Instant.parse("2026-07-20T03:00:00Z"));
        assertThat(preview.getSelectedCandidateCount()).isEqualTo(5);
        verify(previewRepository).flush();
    }

    @Test
    void updateSelections_shouldThrowException_whenVersionMismatch() throws Exception {
        java.lang.reflect.Field versionField = ShowtimeSchedulePreview.class.getDeclaredField("version");
        versionField.setAccessible(true);
        versionField.set(preview, 2L);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));

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
        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));

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

        java.lang.reflect.Constructor<ShowtimeSchedulePreview> previewConstructor = ShowtimeSchedulePreview.class.getDeclaredConstructor();
        previewConstructor.setAccessible(true);
        ShowtimeSchedulePreview otherPreview = previewConstructor.newInstance();
        ReflectionTestUtils.setField(otherPreview, "id", 2L);
        item1.setPreview(otherPreview);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findAllByPublicIdIn(Set.of("item-1"))).thenReturn(List.of(item1));

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

        item1.setValidationStatus(PreviewItemValidationStatus.REJECTED);

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(previewRepository.findByPublicId("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findAllByPublicIdIn(Set.of("item-1"))).thenReturn(List.of(item1));

        UpdatePreviewItemSelectionsRequest request = new UpdatePreviewItemSelectionsRequest(
                2L,
                List.of(new UpdatePreviewItemSelectionRequest("item-1", true))
        );

        assertThatThrownBy(() -> service.updateSelections("preview-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_REJECTED_ITEM_CANNOT_BE_SELECTED);
    }
}
