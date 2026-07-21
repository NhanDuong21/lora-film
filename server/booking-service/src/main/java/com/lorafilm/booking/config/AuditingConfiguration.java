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

    private static final String SYSTEM_ACTOR = "SYSTEM";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(SYSTEM_ACTOR);
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal userPrincipal) {
                return Optional.ofNullable(userPrincipal.getId())
                        .map(String::valueOf)
                        .or(() -> Optional.of(SYSTEM_ACTOR));
            }
            if (principal instanceof Long id) {
                return Optional.of(id.toString());
            }
            if (principal instanceof String str) {
                return Optional.of(str);
            }
            return Optional.of(SYSTEM_ACTOR);
        };
    }
}
