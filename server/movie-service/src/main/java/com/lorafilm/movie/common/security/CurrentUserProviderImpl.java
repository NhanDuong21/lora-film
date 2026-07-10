package com.lorafilm.movie.common.security;

import org.springframework.stereotype.Component;

@Component
public class CurrentUserProviderImpl implements CurrentUserProvider {
    @Override
    public Long getCurrentUserId() {
        // Identity from security context is not fully implemented yet
        return null;
    }
}
