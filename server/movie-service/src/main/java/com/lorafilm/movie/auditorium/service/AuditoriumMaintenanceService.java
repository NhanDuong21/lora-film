package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceWindowResponse;
import java.util.List;

public interface AuditoriumMaintenanceService {
    MaintenanceWindowResponse createWindow(String auditoriumPublicId, CreateMaintenanceWindowRequest request);
    MaintenanceWindowResponse cancelWindow(Long maintenanceWindowId);
    List<MaintenanceWindowResponse> getMaintenanceWindows(String auditoriumPublicId);
}
