package com.project.promotionservice.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class ControllerPageSupport {

    private ControllerPageSupport() {
    }

    public static Pageable pageable(
            int page,
            int size,
            String sort,
            Set<String> allowedProperties,
            String defaultProperty) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String[] parts = sort == null ? new String[0] : sort.split(",", 2);
        String requestedProperty = parts.length == 0 ? null : parts[0].trim();
        String property = requestedProperty == null || !allowedProperties.contains(requestedProperty)
                ? defaultProperty : requestedProperty;
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
