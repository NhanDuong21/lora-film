package com.project.userservice.security;

import com.project.userservice.exception.ForbiddenException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ManagerCinemaScopeService {

    public Set<String> assignedCinemaPublicIds() {
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

    public void requireAssigned(String cinemaPublicId) {
        String normalized = cinemaPublicId == null ? "" : cinemaPublicId.trim().toLowerCase(Locale.ROOT);
        if (!assignedCinemaPublicIds().contains(normalized)) {
            throw denied("Bạn không được phân công quản lý rạp này.");
        }
    }

    private ForbiddenException denied(String message) {
        return new ForbiddenException(message);
    }
}
