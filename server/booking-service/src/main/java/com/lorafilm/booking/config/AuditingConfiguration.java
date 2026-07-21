package com.lorafilm.booking.config;

import com.lorafilm.booking.security.principal.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class AuditingConfiguration {

    private static final Long SYSTEM_USER_ID = 0L;

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(SYSTEM_USER_ID);
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal userPrincipal) {
                return Optional.ofNullable(userPrincipal.getId()).or(() -> Optional.of(SYSTEM_USER_ID));
            }
            if (principal instanceof Long id) {
                return Optional.of(id);
            }
            if (principal instanceof String str) {
                try {
                    return Optional.of(Long.parseLong(str));
                } catch (NumberFormatException e) {
                    return Optional.of(SYSTEM_USER_ID);
                }
            }
            return Optional.of(SYSTEM_USER_ID);
        };
    }
}
