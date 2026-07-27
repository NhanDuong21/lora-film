package com.project.authservice.service;

import com.project.authservice.dto.SessionDto;
import java.util.List;

public interface SessionService {
    List<SessionDto> getUserSessions(String email);
    void revokeSession(Long sessionId, String email);
    void revokeAllSessions(String email);
}
