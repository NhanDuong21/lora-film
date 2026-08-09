package com.project.userservice.dto.response;

import com.project.userservice.enumtype.UserStatus;

/**
 * Minimal customer identity returned to an employee serving the customer at a counter.
 * Sensitive profile fields such as birthday, gender and internal notes are intentionally omitted.
 */
public record CustomerCounterLookupResponse(
        Long accountId,
        String customerCode,
        String fullName,
        String email,
        String phoneNumber,
        UserStatus status) {
}
