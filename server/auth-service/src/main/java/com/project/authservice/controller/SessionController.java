package com.project.authservice.controller;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.SessionDto;
import com.project.authservice.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SessionDto>>> getSessions(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Get sessions called for user={}", userDetails.getUsername());
        List<SessionDto> sessions = sessionService.getUserSessions(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Success", sessions));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Revoke session id={} called for user={}", id, userDetails.getUsername());
        sessionService.revokeSession(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Session revoked successfully", null));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Revoke all sessions called for user={}", userDetails.getUsername());
        sessionService.revokeAllSessions(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked successfully", null));
    }
}
