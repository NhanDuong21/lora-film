package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class UserProfileCreateFailedException extends BaseAuthException {
    public UserProfileCreateFailedException() {
        super("Failed to create user profile", "USER_PROFILE_CREATE_FAILED", HttpStatus.BAD_GATEWAY);
    }
}
