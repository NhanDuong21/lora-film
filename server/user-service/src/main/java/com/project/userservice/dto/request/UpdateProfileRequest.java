package com.project.userservice.dto.request;

import com.project.userservice.enumtype.Gender;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(min = 2, max = 150)
        @Pattern(
                regexp = "^[\\p{L}\\p{M}]+(?:[ '\\-.][\\p{L}\\p{M}]+)+$",
                message = "Full name must contain at least two words and no digits")
        String fullName,
        @Pattern(regexp = "^\\d{10,15}$", message = "Phone must contain 10 to 15 digits") String phoneNumber,
        Gender gender,
        @PastOrPresent LocalDate birthday
) {
}
