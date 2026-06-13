package com.project.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "fullName is required")
    @Size(max = 200, message = "fullName max length is 200")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "Email is invalid")
    @Size(max = 100, message = "email max length is 100")
    private String email;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^0\\d{9,10}$", message = "phoneNumber format is invalid")
    private String phoneNumber;

    @NotBlank(message = "cccd is required")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD must contain 12 digits")
    private String cccd;

    @NotBlank(message = "birthday is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "birthday format is invalid")
    private String birthday;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "password length must be between 6 and 50")
    private String password;

    public RegisterRequest() {}

    public RegisterRequest(String fullName, String email, String phoneNumber, String cccd, String birthday, String password) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.cccd = cccd;
        this.birthday = birthday;
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}