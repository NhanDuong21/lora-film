package com.project.authservice.event.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Immutable data payload embedded inside {@link AccountCreatedEvent}.
 *
 * <p>Field names MUST match the event contract exactly – do not rename.
 * <p>No setters are provided; use the {@link Builder} for construction.
 */
@Getter
@Builder
@AllArgsConstructor
public class AccountCreatedEventData {

    /** Internal account identifier (primary key). */
    private final Long accountId;

    /** Registered e-mail address (lower-cased). */
    private final String email;

    /** Assigned role name (e.g. {@code CUSTOMER}). */
    private final String role;

    /** Full name as supplied during registration. */
    private final String fullName;

    /** Phone number as supplied during registration. */
    private final String phoneNumber;

    /**
     * Raw CCCD value.
     * <strong>NEVER</strong> log this field; use {@link #cccdMasked} in logs.
     */
    private final String cccd;

    /** Masked CCCD, e.g. {@code 092******789}. Safe to log. */
    private final String cccdMasked;

    /** Province code derived from the first 3 digits of the CCCD. */
    private final String provinceCode;

    /** Human-readable province name. */
    private final String provinceName;

    /** Gender derived from the CCCD (e.g. {@code MALE} / {@code FEMALE}). */
    private final String gender;

    /** Date of birth in ISO-8601 format ({@code YYYY-MM-DD}). */
    private final LocalDate birthday;

    /** Four-digit birth year extracted from the birthday. */
    private final Integer birthYear;
}
