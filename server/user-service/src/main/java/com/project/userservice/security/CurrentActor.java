package com.project.userservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentActor {
    private CurrentActor() {
    }

    public static Long accountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated account");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long value) {
            return value;
        }
        if (principal instanceof Number value) {
            return value.longValue();
        }
        return Long.valueOf(principal.toString());
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase(java.util.Locale.ROOT).replaceFirst("^ROLE_", "");
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().toUpperCase(java.util.Locale.ROOT))
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .anyMatch(normalized::equals);
    }
}
