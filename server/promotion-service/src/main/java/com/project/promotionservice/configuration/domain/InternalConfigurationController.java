package com.project.promotionservice.configuration.domain;

import com.project.promotionservice.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.project.promotionservice.common.constant.ValidationConstants.CONFIG_KEY_PATTERN;

@RestController
@Validated
@RequestMapping("/internal/configurations")
@Tag(name = "Internal Runtime Configuration")
public class InternalConfigurationController {
    private final ConfigurationService service;
    public InternalConfigurationController(ConfigurationService service) { this.service = service; }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(summary = "Read active runtime configuration")
    public ResponseEntity<ApiResponse<String>> get(
            @PathVariable @Pattern(regexp = CONFIG_KEY_PATTERN) String key) {
        String value = service.get(key);
        return ResponseEntity.ok(ApiResponse.success("Runtime configuration value", value));
    }
}
