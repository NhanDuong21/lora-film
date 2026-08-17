package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.dto.AuditoriumLayoutSourcePreview;
import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CloneAuditoriumAsNewRequest;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumFromTemplateRequest;

import java.util.List;

public interface AuditoriumLayoutSourceService {
    List<AuditoriumLayoutSourcePreview> getSystemTemplates();
    AuditoriumLayoutSourcePreview getClonePreview(String auditoriumPublicId);
    AuditoriumResponse createFromTemplate(CreateAuditoriumFromTemplateRequest request);
    AuditoriumResponse cloneAsNew(String sourceAuditoriumPublicId, CloneAuditoriumAsNewRequest request);
}
