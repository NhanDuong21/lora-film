package com.project.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EmployeeAccountRequest {

    @NotBlank(message = "Vui lòng nhập email công việc")
    @Email(message = "Email công việc không đúng định dạng")
    private String email;

    @NotBlank(message = "Vui lòng nhập họ tên nhân viên")
    private String fullName;

    @NotNull(message = "Vui lòng chọn nhóm nghiệp vụ")
    private Long accessProfileId;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getAccessProfileId() { return accessProfileId; }
    public void setAccessProfileId(Long accessProfileId) { this.accessProfileId = accessProfileId; }
}
