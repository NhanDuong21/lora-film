package com.project.userservice.dto.response;

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
public class UserProfileResponse {
    private Long accountId;
    private String fullName;
    private String phoneNumber;
    private Gender gender;
    private LocalDate birthday;
    private String cccdMasked;
    private String provinceName;
    private Integer birthYear;
    private Boolean isVerifiedPhone;
}
