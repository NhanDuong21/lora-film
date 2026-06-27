package com.project.bookingservice.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class MockCurrentUserProvider implements CurrentUserProvider {
    @Override
    public Long getCurrentUserId() {
        return 40L;
    }
}
