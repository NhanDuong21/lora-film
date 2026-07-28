package com.project.userservice.dto.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.List;

@JsonSerialize(using = ApiResponse.Serializer.class)
public class ApiResponse<T> {
	private boolean success;
	private String message;
	private String errorCode;
	private T data;
	private List<ValidationError> errors;

	public ApiResponse() {
	}

	public ApiResponse(boolean success, String message, String errorCode, T data) {
		this.success = success;
		this.message = message;
		this.errorCode = errorCode;
		this.data = data;
	}

	public ApiResponse(boolean success, String message, String errorCode, T data, List<ValidationError> errors) {
		this.success = success;
		this.message = message;
		this.errorCode = errorCode;
		this.data = data;
		this.errors = errors;
	}

	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(true, message, null, data, null);
	}

	public static <T> ApiResponse<T> failure(String message) {
		return new ApiResponse<>(false, message, null, null, null);
	}

	public static <T> ApiResponse<T> error(String message, String errorCode) {
		return new ApiResponse<>(false, message, errorCode, null, null);
	}

	public static <T> ApiResponse<T> validationError(String message, List<ValidationError> errors) {
		return new ApiResponse<>(false, message, "VALIDATION_ERROR", null, errors);
	}

	public boolean isSuccess() { return success; }
	public void setSuccess(boolean success) { this.success = success; }

	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }

	public String getErrorCode() { return errorCode; }
	public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

	public T getData() { return data; }
	public void setData(T data) { this.data = data; }

	public List<ValidationError> getErrors() { return errors; }
	public void setErrors(List<ValidationError> errors) { this.errors = errors; }

	public static class ValidationError {
		private String field;
		private String code;
		private String message;

		public ValidationError() {}

		public ValidationError(String field, String code, String message) {
			this.field = field;
			this.code = code;
			this.message = message;
		}

		public String getField() { return field; }
		public void setField(String field) { this.field = field; }

		public String getCode() { return code; }
		public void setCode(String code) { this.code = code; }

		public String getMessage() { return message; }
		public void setMessage(String message) { this.message = message; }
	}

	public static class Serializer extends JsonSerializer<ApiResponse<?>> {
		@Override
		public void serialize(ApiResponse<?> value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			gen.writeStartObject();
			gen.writeBooleanField("success", value.success);
			gen.writeStringField("message", value.message);

			if (value.errorCode != null) {
				gen.writeStringField("errorCode", value.errorCode);
			}

			// Omit 'data' only if this is a validation error containing error details
			if (!("VALIDATION_ERROR".equals(value.errorCode) && value.errors != null)) {
				gen.writeObjectField("data", value.data);
			}

			if (value.errors != null) {
				gen.writeObjectField("errors", value.errors);
			}

			gen.writeEndObject();
		}
	}
}
