package com.project.authservice.controller;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.SessionDto;
import com.project.authservice.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/sessions")
public class SessionController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SessionController.class);

    private final SessionService sessionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SessionDto>>> getSessions(@AuthenticationPrincipal String username) {
        log.info("Get sessions called for user={}", username);
        List<SessionDto> sessions = sessionService.getUserSessions(username);
        return ResponseEntity.ok(ApiResponse.success("Success", sessions));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable Long id, @AuthenticationPrincipal String username) {
        log.info("Revoke session id={} called for user={}", id, username);
        sessionService.revokeSession(id, username);
        return ResponseEntity.ok(ApiResponse.success("Session revoked successfully", null));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(@AuthenticationPrincipal String username) {
        log.info("Revoke all sessions called for user={}", username);
        sessionService.revokeAllSessions(username);
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked successfully", null));
    }
    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }
}
