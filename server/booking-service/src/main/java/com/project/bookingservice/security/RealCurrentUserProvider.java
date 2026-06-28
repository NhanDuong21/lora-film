package com.project.bookingservice.security;

import com.project.bookingservice.exception.BusinessException;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RealCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BusinessException("UNAUTHORIZED", "User not authenticated");
        }
        
        try {
            return (Long) authentication.getPrincipal();
        } catch (ClassCastException e) {
            throw new BusinessException("UNAUTHORIZED", "Invalid authentication token");
        }
    }
}
