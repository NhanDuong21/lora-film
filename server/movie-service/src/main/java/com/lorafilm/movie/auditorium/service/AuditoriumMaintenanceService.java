package com.lorafilm.movie.auditorium.service;

import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.ExtendMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceWindowResponse;
import com.lorafilm.movie.auditorium.dto.ResolveMaintenanceWindowRequest;
import java.util.List;

public interface AuditoriumMaintenanceService {
    MaintenanceWindowResponse createWindow(String auditoriumPublicId, CreateMaintenanceWindowRequest request);
    MaintenanceWindowResponse cancelWindow(Long maintenanceWindowId);
    MaintenanceWindowResponse resolveWindow(Long maintenanceWindowId, ResolveMaintenanceWindowRequest request);
    MaintenanceWindowResponse extendWindow(Long maintenanceWindowId, ExtendMaintenanceWindowRequest request);
    List<MaintenanceWindowResponse> getMaintenanceWindows(String auditoriumPublicId);
}
