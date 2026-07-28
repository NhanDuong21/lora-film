package com.project.userservice.controller;

import com.project.userservice.dto.request.PositionRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.PositionResponse;
import com.project.userservice.service.PositionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/positions")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Positions")
public class PositionController {
    private final PositionService service;

    public PositionController(PositionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAnyAuthority('POSITION_VIEW', 'EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<List<PositionResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Positions retrieved", service.list()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAnyAuthority('POSITION_VIEW', 'EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<Page<PositionResponse>>> search(@RequestParam(required = false) String keyword,
                                                                      Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Positions retrieved", service.search(keyword, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('POSITION_CREATE')")
    public ResponseEntity<ApiResponse<PositionResponse>> create(@Valid @RequestBody PositionRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Position created", service.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('POSITION_UPDATE')")
    public ResponseEntity<ApiResponse<PositionResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody PositionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Position updated", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('POSITION_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Position deleted", null));
    }
}
