package com.project.authservice.security.oauth2;

public record OAuth2Profile(
        String registrationId,
        String providerUserId,
        String email,
        String fullName,
        String avatarUrl) {
}
