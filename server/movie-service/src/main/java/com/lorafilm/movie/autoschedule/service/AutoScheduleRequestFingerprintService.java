package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;

public interface AutoScheduleRequestFingerprintService {
    String generateFingerprint(NormalizedGeneratePreviewRequest request);
}
