package com.project.userservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class PiiAccess {
    private PiiAccess() {
    }

    public static boolean canViewSensitive() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority())
                        || "PII_VIEW".equals(authority.getAuthority()));
    }

    public static String maskPhone(String phone) {
        if (phone == null || canViewSensitive()) {
            return phone;
        }
        return phone.length() <= 4 ? "****" : "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
