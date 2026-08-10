package com.project.userservice.dto.response;

public record AccountDisplayNameResponse(
        Long accountId,
        String fullName) {
}
