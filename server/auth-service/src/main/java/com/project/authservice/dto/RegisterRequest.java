package com.project.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
	@NotBlank(message = "email is required")
	@Email(message = "email format is invalid")
	@Size(max = 150, message = "email max length is 150")
	private String email;

	@NotBlank(message = "password is required")
	@Size(min = 8, max = 50, message = "password length must be between 8 and 50")
	private String password;

	@NotBlank(message = "confirmPassword is required")
	@Size(min = 8, max = 50, message = "confirmPassword length must be between 8 and 50")
	private String confirmPassword;

	// Extra profile fields sent from frontend
	@NotBlank(message = "fullName is required")
	@Size(max = 200, message = "fullName max length is 200")
	private String fullName;

	@NotBlank(message = "citizenId is required")
	@Size(min = 12, max = 12, message = "citizenId must be exactly 12 digits")
	private String citizenId;

	@NotBlank(message = "gender is required")
	private String gender;

	@NotBlank(message = "dob is required")
	// Expecting ISO date string (yyyy-MM-dd)
	private String dob;

	/**
	 * Validates that confirmPassword matches password.
	 *
	 * @return true when both passwords match
	 */
	@AssertTrue(message = "confirmPassword must equal password")
	@JsonIgnore
	public boolean isPasswordConfirmed() {
		return password != null && password.equals(confirmPassword);
	}
}