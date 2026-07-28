package com.project.promotionservice.benefit.controller;

import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

final class BenefitControllerSupport {

    private BenefitControllerSupport() {
    }

    static String actor(UserPrincipal principal) {
        if (principal == null) return "SYSTEM";
        String actor = principal.getId() != null
                ? principal.getId().toString()
                : principal.getUsername();
        if (actor == null || actor.isBlank()) return "SYSTEM";
        return actor.length() > 36 ? actor.substring(0, 36) : actor;
    }

    static Pageable pageable(int page, int size, String sort, Set<String> allowedProperties) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String[] parts = sort == null ? new String[0] : sort.split(",", 2);
        String property = parts.length == 0 || !allowedProperties.contains(parts[0])
                ? "createdAt" : parts[0];
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
