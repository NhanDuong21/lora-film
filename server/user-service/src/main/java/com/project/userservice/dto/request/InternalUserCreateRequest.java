package com.project.userservice.dto.request;

import com.project.userservice.enumtype.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserCreateRequest {
    private Long accountId;
    private String fullName;
    private String phoneNumber;
    private String cccd;
    private String cccdMasked;
    private String provinceCode;
    private String provinceName;
    private Integer birthYear;
    private Gender gender;
    private LocalDate birthday;
    private String cccdCheckNote;
}
