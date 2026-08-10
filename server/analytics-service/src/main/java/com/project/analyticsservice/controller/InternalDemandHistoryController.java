package com.project.analyticsservice.controller;

import com.project.analyticsservice.service.DemandHistorySnapshotService;
import com.project.analyticsservice.common.ApiResponse;
import com.project.analyticsservice.dto.DemandHistorySnapshotRequest;
import com.project.analyticsservice.dto.DemandHistorySnapshotResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/analytics")
public class InternalDemandHistoryController {

    private final DemandHistorySnapshotService service;

    public InternalDemandHistoryController(DemandHistorySnapshotService service) {
        this.service = service;
    }

    @PostMapping("/demand-snapshot")
    public ApiResponse<DemandHistorySnapshotResponse> snapshot(
            @Valid @RequestBody DemandHistorySnapshotRequest request) {
        return ApiResponse.success("Demand history snapshot generated", service.snapshot(request));
    }
}
