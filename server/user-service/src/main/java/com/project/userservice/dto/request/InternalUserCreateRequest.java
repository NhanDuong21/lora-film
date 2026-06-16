package com.project.userservice.dto.request;

import com.project.userservice.enumtype.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class InternalUserCreateRequest {

    @NotNull(message = "Account ID cannot be null")
    private Long accountId;

    @NotBlank(message = "Full name cannot be blank")
    @Size(min = 2, max = 200, message = "Full name must be between 2 and 200 characters")
    @Pattern(regexp = "^\\s*[a-zA-ZÀ-ỹ]+(\\s+[a-zA-ZÀ-ỹ]+)+\\s*$", message = "Full name must not contain numbers or special characters and must have at least 2 words")
    private String fullName;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "CCCD cannot be blank")
    @Pattern(regexp = "^[0-9]{12}$", message = "CCCD must be exactly 12 digits")
    private String cccd;

    @NotBlank(message = "CCCD masked cannot be blank")
    private String cccdMasked;

    private String provinceCode;
    private String provinceName;
    private Integer birthYear;

    @NotNull(message = "Gender cannot be null")
    private Gender gender;

    @NotNull(message = "Birthday cannot be null")
    private LocalDate birthday;

    private String cccdCheckNote;

    public InternalUserCreateRequest() {
    }

    public InternalUserCreateRequest(Long accountId, String fullName, String phoneNumber, String cccd,
                                     String cccdMasked, String provinceCode, String provinceName,
                                     Integer birthYear, Gender gender, LocalDate birthday, String cccdCheckNote) {
        this.accountId = accountId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.cccd = cccd;
        this.cccdMasked = cccdMasked;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.birthYear = birthYear;
        this.gender = gender;
        this.birthday = birthday;
        this.cccdCheckNote = cccdCheckNote;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCccd() {
        return cccd;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
    }

    public String getCccdMasked() {
        return cccdMasked;
    }

    public void setCccdMasked(String cccdMasked) {
        this.cccdMasked = cccdMasked;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getCccdCheckNote() {
        return cccdCheckNote;
    }

    public void setCccdCheckNote(String cccdCheckNote) {
        this.cccdCheckNote = cccdCheckNote;
    }
}
