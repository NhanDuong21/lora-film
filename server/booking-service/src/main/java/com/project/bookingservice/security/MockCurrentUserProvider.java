package com.project.bookingservice.security;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile({"local", "test"})
public class MockCurrentUserProvider implements CurrentUserProvider {
    @Override
    public Long getCurrentUserId() {
        return 15L;
    }
}
