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
}
