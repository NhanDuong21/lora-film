package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.CloneAuditoriumRequest;


public interface AuditoriumService {
    AuditoriumResponse createAuditorium(String cinemaPublicId, CreateAuditoriumRequest request);
    AuditoriumResponse updateAuditorium(String auditoriumPublicId, UpdateAuditoriumRequest request);
    AuditoriumResponse cloneAuditoriumLayout(String cinemaPublicId, String targetAuditoriumPublicId, CloneAuditoriumRequest request);

    void deleteAuditorium(String auditoriumPublicId);
}
