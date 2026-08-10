package com.lorafilm.movie.common.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        try {
            // principal should be the userId string (as set in JwtFilter)
            return Optional.of(Long.valueOf(authentication.getName()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
