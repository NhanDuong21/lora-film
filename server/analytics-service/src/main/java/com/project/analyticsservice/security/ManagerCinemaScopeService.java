package com.project.analyticsservice.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ManagerCinemaScopeService {

    public boolean isManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()));
    }

    public Set<String> assignedCinemaKeys() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw denied("Không xác định được tài khoản quản lý rạp.");
        }
        if (!(authentication.getDetails() instanceof Map<?, ?> claims)
                || !(claims.get("cinemaPublicIds") instanceof Collection<?> values)) {
            return Set.of();
        }
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public void requireIfManager(String cinemaKey) {
        if (!isManager()) {
            return;
        }
        String normalized = cinemaKey == null ? "" : cinemaKey.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw denied("Vui lòng chọn rạp được phân công để xem báo cáo.");
        }
        if (!assignedCinemaKeys().contains(normalized)) {
            throw denied("Bạn không được xem báo cáo của rạp này.");
        }
    }

    private AccessDeniedException denied(String message) {
        return new AccessDeniedException(message);
    }
}
