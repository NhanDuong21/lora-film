package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoScheduleGenerateRequestNormalizerImplTest {

    private AutoScheduleGenerateRequestNormalizerImpl normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new AutoScheduleGenerateRequestNormalizerImpl();
    }

    @Test
    void normalize_validRequest_normalizesProperly() {
        GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
        request.setCinemaPublicId(" cinema-1 ");
        request.setScheduleFrom(LocalDate.of(2023, 1, 1));
        request.setScheduleTo(LocalDate.of(2023, 1, 7));
        request.setMovieVersionPublicIds(Arrays.asList("mv-2 ", "mv-1", " mv-1 ", ""));
        request.setAuditoriumPublicIds(Arrays.asList("aud-2", " aud-1 ", null));
        request.setSlotGranularityMinutes(15);
        request.setPreviewTtlMinutes(60);
        request.setIdempotencyKey(" key-123 ");

        NormalizedGeneratePreviewRequest normalized = normalizer.normalize(request);

        assertEquals("cinema-1", normalized.getCinemaPublicId());
        assertEquals("key-123", normalized.getIdempotencyKey());
        assertEquals(15, normalized.getSlotGranularityMinutes());
        assertEquals(60, normalized.getPreviewTtlMinutes());
        
        // Assert ordered, distinct, trimmed, non-empty
        assertEquals(List.of("mv-1", "mv-2"), normalized.getMovieVersionPublicIds());
        assertEquals(List.of("aud-1", "aud-2"), normalized.getAuditoriumPublicIds());
    }

    @Test
    void normalize_emptyMovieVersions_throwsException() {
        GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
        request.setMovieVersionPublicIds(Arrays.asList(" ", null));
        request.setAuditoriumPublicIds(List.of("aud-1"));

        BusinessException ex = assertThrows(BusinessException.class, () -> normalizer.normalize(request));
        assertEquals(ErrorCode.AUTO_SCHEDULE_EMPTY_MOVIE_VERSIONS, ex.getErrorCode());
    }

    @Test
    void normalize_emptyAuditoriums_throwsException() {
        GenerateShowtimeSchedulePreviewRequest request = new GenerateShowtimeSchedulePreviewRequest();
        request.setMovieVersionPublicIds(List.of("mv-1"));
        request.setAuditoriumPublicIds(Arrays.asList(" ", null));

        BusinessException ex = assertThrows(BusinessException.class, () -> normalizer.normalize(request));
        assertEquals(ErrorCode.AUTO_SCHEDULE_EMPTY_AUDITORIUMS, ex.getErrorCode());
    }
}
