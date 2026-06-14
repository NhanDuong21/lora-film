package com.project.authservice.dto.response;

public class RegisterResponse {
    private Long accountId;
    private String email;
    private String role;
    private String fullName;
    private String phoneNumber;
    private String cccdMasked;
    private String provinceName;
    private String gender;
    private Integer birthYear;

    public RegisterResponse() {}

    public RegisterResponse(Long accountId, String email, String role, String fullName, String phoneNumber,
                            String cccdMasked, String provinceName, String gender, Integer birthYear) {
        this.accountId = accountId;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.cccdMasked = cccdMasked;
        this.provinceName = provinceName;
        this.gender = gender;
        this.birthYear = birthYear;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getCccdMasked() {
        return cccdMasked;
    }

    public void setCccdMasked(String cccdMasked) {
        this.cccdMasked = cccdMasked;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }
}