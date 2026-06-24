package com.project.bookingservice.security;

import org.springframework.stereotype.Component;

@Component
public class MockCurrentUserProvider implements CurrentUserProvider {
    @Override
    public Long getCurrentUserId() {
        return 15L;
    }
}
