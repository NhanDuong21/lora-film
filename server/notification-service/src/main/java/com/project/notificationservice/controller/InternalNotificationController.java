package com.project.notificationservice.controller;

import com.project.notificationservice.api.ApiResponse;
import com.project.notificationservice.service.NotificationApplicationService;
import com.project.notificationservice.service.NotificationApplicationService.RequestDetails;
import com.project.notificationservice.service.NotificationCommands.AcceptedNotification;
import com.project.notificationservice.service.NotificationCommands.CreateNotificationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/internal/notifications")
public class InternalNotificationController {

    private final NotificationApplicationService service;

    public InternalNotificationController(NotificationApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AcceptedNotification>> create(
            @Valid @RequestBody CreateNotificationCommand command) {
        AcceptedNotification accepted = service.accept(command);
        return ResponseEntity.status(accepted.idempotent() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(ApiResponse.accepted(accepted));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<AcceptedNotification>>> batch(
            @RequestBody @Size(min = 1, max = 100) List<@Valid CreateNotificationCommand> commands) {
        return ResponseEntity.accepted().body(ApiResponse.accepted(commands.stream()
                .map(service::accept).toList()));
    }

    @GetMapping("/{publicId}")
    public ApiResponse<RequestDetails> get(@PathVariable String publicId) {
        return ApiResponse.success(service.get(publicId));
    }

    @GetMapping("/{publicId}/deliveries")
    public ApiResponse<List<NotificationApplicationService.DeliveryDetails>> deliveries(
            @PathVariable String publicId) {
        return ApiResponse.success(service.get(publicId).deliveries());
    }

    @PostMapping("/{publicId}/cancel")
    public ApiResponse<RequestDetails> cancel(@PathVariable String publicId) {
        return ApiResponse.success(service.cancel(publicId));
    }
}
