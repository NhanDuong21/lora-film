package com.lorafilm.booking.security.service;

import com.lorafilm.booking.security.principal.UserPrincipal;
import java.util.Optional;

public interface SecurityContextService {

    Long getCurrentUserId();

    Optional<UserPrincipal> getCurrentUser();

    boolean hasRole(String role);

    boolean isAdmin();

    boolean hasPermission(String permission);

    boolean isIdentityVerified();
}
