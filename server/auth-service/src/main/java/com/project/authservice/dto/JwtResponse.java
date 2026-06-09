package com.project.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
	private String token;
	@Builder.Default
	private String tokenType = "Bearer";
	private String email;
	private String role;
}
