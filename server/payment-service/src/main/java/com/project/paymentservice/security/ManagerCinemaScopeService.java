package com.project.paymentservice.security;

import com.project.paymentservice.exception.BusinessException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ManagerCinemaScopeService {
    private static final Pattern PUBLIC_ID = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    public Set<String> assignedCinemaPublicIds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw denied("Không xác định được tài khoản quản lý rạp.");
        }
        if (!(authentication.getDetails() instanceof Map<?, ?> claims)) return Set.of();
        Object raw = claims.get("cinemaPublicIds");
        if (!(raw instanceof Collection<?> values)) return Set.of();
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> PUBLIC_ID.matcher(value).matches())
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public String requireAssigned(String cinemaPublicId) {
        String normalized = normalize(cinemaPublicId);
        if (!assignedCinemaPublicIds().contains(normalized)) {
            throw denied("Bạn không được phân công quản lý rạp này.");
        }
        return normalized;
    }

    public String normalize(String cinemaPublicId) {
        return cinemaPublicId == null ? "" : cinemaPublicId.trim().toLowerCase(Locale.ROOT);
    }

    private BusinessException denied(String message) {
        return new BusinessException("MANAGER_CINEMA_ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
    }
}
