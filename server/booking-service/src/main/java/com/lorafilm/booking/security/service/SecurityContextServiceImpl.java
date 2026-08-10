package com.lorafilm.booking.security.service;

import com.lorafilm.booking.security.constant.RoleConstants;
import com.lorafilm.booking.security.principal.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecurityContextServiceImpl implements SecurityContextService {

    @Override
    public Long getCurrentUserId() {
        return getCurrentUser().map(UserPrincipal::getId).orElse(null);
    }

    @Override
    public Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal userPrincipal) {
                return Optional.of(userPrincipal);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(targetRole));
    }

    @Override
    public boolean isAdmin() {
        return hasRole(RoleConstants.ADMIN);
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(permission));
    }

    @Override
    public boolean isIdentityVerified() {
        return getCurrentUser().map(UserPrincipal::isIdentityVerified).orElse(false);
    }
}
