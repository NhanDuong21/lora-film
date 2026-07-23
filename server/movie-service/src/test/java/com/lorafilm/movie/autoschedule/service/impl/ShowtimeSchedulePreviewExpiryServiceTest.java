package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeSchedulePreviewExpiryServiceTest {

    @Mock
    private ShowtimeSchedulePreviewRepository repository;

    @InjectMocks
    private ShowtimeSchedulePreviewExpiryService service;

    @Test
    void expireIfNecessary_reReadsUnderLockAndExpiresOnlyPreviewedState() {
        Instant now = Instant.parse("2026-07-22T08:00:00Z");
        ShowtimeSchedulePreview preview = org.mockito.Mockito.mock(ShowtimeSchedulePreview.class);
        when(preview.getStatus()).thenReturn(SchedulePreviewStatus.PREVIEWED);
        when(preview.getExpiresAt()).thenReturn(now);
        when(repository.findByPublicIdForExpiry("preview")).thenReturn(Optional.of(preview));

        assertTrue(service.expireIfNecessary("preview", now));

        verify(preview).setStatus(SchedulePreviewStatus.EXPIRED);
        verify(repository).saveAndFlush(preview);
    }

    @Test
    void expireIfNecessary_neverOverwritesAppliedTerminalState() {
        Instant now = Instant.parse("2026-07-22T08:00:00Z");
        ShowtimeSchedulePreview preview = org.mockito.Mockito.mock(ShowtimeSchedulePreview.class);
        when(preview.getStatus()).thenReturn(SchedulePreviewStatus.APPLIED);
        when(repository.findByPublicIdForExpiry("preview")).thenReturn(Optional.of(preview));

        assertFalse(service.expireIfNecessary("preview", now));

        verify(preview, never()).setStatus(SchedulePreviewStatus.EXPIRED);
        verify(repository, never()).saveAndFlush(preview);
    }
}
