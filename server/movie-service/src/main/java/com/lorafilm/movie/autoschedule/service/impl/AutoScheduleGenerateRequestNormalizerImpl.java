package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerateRequestNormalizer;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AutoScheduleGenerateRequestNormalizerImpl implements AutoScheduleGenerateRequestNormalizer {

    @Override
    public NormalizedGeneratePreviewRequest normalize(GenerateShowtimeSchedulePreviewRequest request) {
        String cinemaPublicId = request.getCinemaPublicId() != null ? request.getCinemaPublicId().trim() : null;
        String idempotencyKey = request.getIdempotencyKey() != null ? request.getIdempotencyKey().trim() : null;

        List<String> movieVersionPublicIds = normalizeList(request.getMovieVersionPublicIds());
        List<String> auditoriumPublicIds = normalizeList(request.getAuditoriumPublicIds());

        if (movieVersionPublicIds.isEmpty()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_EMPTY_MOVIE_VERSIONS);
        }

        if (auditoriumPublicIds.isEmpty()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_EMPTY_AUDITORIUMS);
        }

        return new NormalizedGeneratePreviewRequest(
                cinemaPublicId,
                request.getScheduleFrom(),
                request.getScheduleTo(),
                movieVersionPublicIds,
                auditoriumPublicIds,
                request.getSlotGranularityMinutes() == null ? 15 : request.getSlotGranularityMinutes(),
                request.getPreviewTtlMinutes() == null ? 60 : request.getPreviewTtlMinutes(),
                idempotencyKey
        );
    }

    private List<String> normalizeList(List<String> input) {
        if (input == null) {
            return List.of();
        }
        return input.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
