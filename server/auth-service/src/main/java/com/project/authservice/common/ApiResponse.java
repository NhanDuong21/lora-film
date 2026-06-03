package com.project.authservice.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
	private boolean success;
	private String message;
	private T data;

	/**
	 * Creates a success response.
	 *
	 * @param message response message
	 * @param data response payload
	 * @param <T> payload type
	 * @return success response
	 */
	public static <T> ApiResponse<T> success(String message, T data) {
		return ApiResponse.<T>builder()
				.success(true)
				.message(message)
				.data(data)
				.build();
	}

	/**
	 * Creates a failure response.
	 *
	 * @param message response message
	 * @param <T> payload type
	 * @return failure response
	 */
	public static <T> ApiResponse<T> failure(String message) {
		return ApiResponse.<T>builder()
				.success(false)
				.message(message)
				.build();
	}
}