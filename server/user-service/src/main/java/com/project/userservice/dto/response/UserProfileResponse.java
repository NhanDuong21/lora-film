package com.project.userservice.dto.response;

import com.project.userservice.enumtype.Gender;

import java.time.LocalDate;

public class UserProfileResponse {

    private Long accountId;
    private String fullName;
    private String phoneNumber;
    private Gender gender;
    private LocalDate birthday;
    private String cccdMasked;
    private String provinceName;
    private Integer birthYear;


    public UserProfileResponse() {
    }

    public UserProfileResponse(Long accountId, String fullName, String phoneNumber, Gender gender,
                               LocalDate birthday, String cccdMasked, String provinceName,
                               Integer birthYear) {
        this.accountId = accountId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.birthday = birthday;
        this.cccdMasked = cccdMasked;
        this.provinceName = provinceName;
        this.birthYear = birthYear;
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

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

}
