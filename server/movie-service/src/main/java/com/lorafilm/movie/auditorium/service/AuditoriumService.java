package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.CloneAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumWithLayoutRequest;


public interface AuditoriumService {
    AuditoriumResponse createAuditorium(String cinemaPublicId, CreateAuditoriumRequest request);
    AuditoriumResponse createAuditoriumWithLayout(
            String cinemaPublicId, CreateAuditoriumWithLayoutRequest request);
    AuditoriumResponse updateAuditorium(String auditoriumPublicId, UpdateAuditoriumRequest request);
    AuditoriumResponse cloneAuditoriumLayout(String cinemaPublicId, String targetAuditoriumPublicId, CloneAuditoriumRequest request);

    void deleteAuditorium(String auditoriumPublicId);
}
