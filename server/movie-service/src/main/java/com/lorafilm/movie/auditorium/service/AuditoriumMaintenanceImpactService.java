package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceImpactResponse;

public interface AuditoriumMaintenanceImpactService {
    MaintenanceImpactResponse preview(
            String auditoriumPublicId,
            CreateMaintenanceWindowRequest request);
}
