package com.project.promotionservice.common.web;

import com.project.promotionservice.configuration.security.principal.InternalServicePrincipal;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityActor {
    private SecurityActor() {}

    public static String current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            if (user.getId() != null) return user.getId().toString();
            if (user.getUsername() != null) return trim(user.getUsername());
        }
        if (authentication != null && authentication.getPrincipal() instanceof InternalServicePrincipal service) {
            return trim(service.getServiceName());
        }
        return "SYSTEM";
    }

    private static String trim(String value) {
        return value == null ? "SYSTEM" : value.length() <= 36 ? value : value.substring(0, 36);
    }
}
