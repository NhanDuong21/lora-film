package com.project.promotionservice.benefit.controller;

import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import org.springframework.data.domain.Pageable;

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
        return com.project.promotionservice.common.web.ControllerPageSupport.pageable(
                page, size, sort, allowedProperties, "createdAt");
    }
}
