package com.project.authservice.event.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Immutable data payload embedded inside {@link AccountCreatedEvent}.
 *
 * <p>Field names MUST match the event contract exactly – do not rename.
 * <p>No setters are provided; use the {@link Builder} for construction.
 */
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

    /**
     * Date of birth.
     * Serialized as {@code yyyy-MM-dd} string (e.g. {@code 2005-06-12}),
     * never as a JSON array.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate birthday;

    /** Four-digit birth year extracted from the birthday. */
    private final Integer birthYear;

    /** All-args constructor used by {@link Builder}. */
    public AccountCreatedEventData(Long accountId,
                                   String email,
                                   String role,
                                   String fullName,
                                   String phoneNumber,
                                   String cccd,
                                   String cccdMasked,
                                   String provinceCode,
                                   String provinceName,
                                   String gender,
                                   LocalDate birthday,
                                   Integer birthYear) {
        this.accountId    = accountId;
        this.email        = email;
        this.role         = role;
        this.fullName     = fullName;
        this.phoneNumber  = phoneNumber;
        this.cccd         = cccd;
        this.cccdMasked   = cccdMasked;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.gender       = gender;
        this.birthday     = birthday;
        this.birthYear    = birthYear;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long    getAccountId()   { return accountId; }
    public String  getEmail()       { return email; }
    public String  getRole()        { return role; }
    public String  getFullName()    { return fullName; }
    public String  getPhoneNumber() { return phoneNumber; }
    public String  getCccd()        { return cccd; }
    public String  getCccdMasked()  { return cccdMasked; }
    public String  getProvinceCode(){ return provinceCode; }
    public String  getProvinceName(){ return provinceName; }
    public String  getGender()      { return gender; }
    public LocalDate getBirthday()  { return birthday; }
    public Integer getBirthYear()   { return birthYear; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long     accountId;
        private String   email;
        private String   role;
        private String   fullName;
        private String   phoneNumber;
        private String   cccd;
        private String   cccdMasked;
        private String   provinceCode;
        private String   provinceName;
        private String   gender;
        private LocalDate birthday;
        private Integer  birthYear;

        private Builder() {}

        public Builder accountId(Long accountId)       { this.accountId = accountId;       return this; }
        public Builder email(String email)             { this.email = email;               return this; }
        public Builder role(String role)               { this.role = role;                 return this; }
        public Builder fullName(String fullName)       { this.fullName = fullName;         return this; }
        public Builder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber;   return this; }
        public Builder cccd(String cccd)               { this.cccd = cccd;                 return this; }
        public Builder cccdMasked(String cccdMasked)   { this.cccdMasked = cccdMasked;     return this; }
        public Builder provinceCode(String code)       { this.provinceCode = code;         return this; }
        public Builder provinceName(String name)       { this.provinceName = name;         return this; }
        public Builder gender(String gender)           { this.gender = gender;             return this; }
        public Builder birthday(LocalDate birthday)    { this.birthday = birthday;         return this; }
        public Builder birthYear(Integer birthYear)    { this.birthYear = birthYear;       return this; }

        public AccountCreatedEventData build() {
            return new AccountCreatedEventData(
                    accountId, email, role, fullName, phoneNumber,
                    cccd, cccdMasked, provinceCode, provinceName,
                    gender, birthday, birthYear);
        }
    }
}
