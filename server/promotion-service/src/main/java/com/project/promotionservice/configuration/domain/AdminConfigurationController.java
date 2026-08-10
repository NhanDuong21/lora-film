package com.project.promotionservice.configuration.domain;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.common.web.ControllerPageSupport;
import com.project.promotionservice.common.web.SecurityActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

@RestController
@Validated
@RequestMapping("/api/admin/configurations")
@Tag(name = "Promotion Configuration Management")
public class AdminConfigurationController {
    private static final java.util.Set<String> SORT_FIELDS =
            java.util.Set.of("createdAt", "updatedAt", "configKey", "category", "status");
    private final ConfigurationService service;

    public AdminConfigurationController(ConfigurationService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
    @Operation(summary = "Create dynamic configuration")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> create(
            @Valid @RequestBody ConfigurationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Configuration created successfully", service.create(request, SecurityActor.current())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
    @Operation(summary = "Update dynamic configuration")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> update(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id,
            @Valid @RequestBody ConfigurationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Configuration updated successfully",
                service.update(id, request, SecurityActor.current())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
    @Operation(summary = "Deprecate configuration")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        service.delete(id, SecurityActor.current());
        return ResponseEntity.ok(ApiResponse.success("Configuration deprecated successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
    @Operation(summary = "Search configurations")
    public ResponseEntity<ApiResponse<PagedResponse<ConfigurationResponse>>> search(
            @RequestParam(required = false) @Size(max = 150) String keyword,
            @RequestParam(required = false) @Size(max = 100) String category,
            @RequestParam(required = false) ConfigurationStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") @Size(max = 60) String sort) {
        Pageable pageable = ControllerPageSupport.pageable(page, size, sort, SORT_FIELDS, "createdAt");
        return ResponseEntity.ok(ApiResponse.success("Configuration search results",
                service.search(keyword, category, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CONFIGURATION_MANAGER')")
    @Operation(summary = "Get configuration detail")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> detail(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        return ResponseEntity.ok(ApiResponse.success("Configuration detail", service.detail(id)));
    }
}
