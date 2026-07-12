package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest;


public interface AuditoriumService {
    AuditoriumResponse createAuditorium(String cinemaPublicId, CreateAuditoriumRequest request);
    AuditoriumResponse updateAuditorium(String auditoriumPublicId, UpdateAuditoriumRequest request);

    void deleteAuditorium(String auditoriumPublicId);
}
