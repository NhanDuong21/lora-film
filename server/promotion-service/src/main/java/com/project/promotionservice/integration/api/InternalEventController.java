package com.project.promotionservice.integration.api;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.web.SecurityActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/events")
@Tag(name = "Internal Event Operations")
public class InternalEventController {
    private final IntegrationOperationsService service;
    public InternalEventController(IntegrationOperationsService service) { this.service = service; }

    @PostMapping("/publish")
    @PreAuthorize("hasRole('OPERATIONS_SERVICE')")
    @Operation(summary = "Persist an outbound event in the transactional outbox")
    public ResponseEntity<ApiResponse<EventHistoryResponse>> publish(
            @Valid @RequestBody EventPublishRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Event queued for publish", service.publish(request, SecurityActor.current())));
    }

    @PostMapping("/retry/{id}")
    @PreAuthorize("hasRole('OPERATIONS_SERVICE')")
    @Operation(summary = "Retry failed outbound event")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable String id) {
        service.retry(id, SecurityActor.current());
        return ResponseEntity.ok(ApiResponse.success("Event retry scheduled", null));
    }

    /**
     * Body-based alias for schedulers that cannot safely put an event id in a
     * path. It follows the issue contract while the path form remains
     * backwards compatible for existing callers.
     */
    @PostMapping("/retry")
    @PreAuthorize("hasRole('OPERATIONS_SERVICE')")
    @Operation(summary = "Retry failed outbound event")
    public ResponseEntity<ApiResponse<Void>> retry(@Valid @RequestBody EventReprocessRequest request) {
        service.retry(request.getEventPublicId(), SecurityActor.current());
        return ResponseEntity.ok(ApiResponse.success("Event retry scheduled", null));
    }

    @PostMapping("/dlq/reprocess")
    @PreAuthorize("hasRole('OPERATIONS_SERVICE')")
    @Operation(summary = "Reprocess inbound or outbound dead-letter event")
    public ResponseEntity<ApiResponse<Void>> reprocess(@Valid @RequestBody EventReprocessRequest request) {
        service.reprocess(request);
        return ResponseEntity.ok(ApiResponse.success("Event reprocess requested", null));
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('OPERATIONS_SERVICE')")
    @Operation(summary = "Get event processing status")
    public ResponseEntity<ApiResponse<EventStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success("Event status", service.status()));
    }
}
