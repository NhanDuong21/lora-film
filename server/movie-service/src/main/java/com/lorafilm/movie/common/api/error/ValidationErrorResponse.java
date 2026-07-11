package com.lorafilm.movie.common.api.error;

import com.lorafilm.movie.common.api.ValidationErrorData;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationErrorResponse")
public class ValidationErrorResponse {

    @Schema(example = "false")
    private boolean success;

    @Schema(example = "Dữ liệu đầu vào không hợp lệ")
    private String message;

    @Schema(example = "VALIDATION_ERROR")
    private String errorCode;

    private ValidationErrorData data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public ValidationErrorData getData() { return data; }
    public void setData(ValidationErrorData data) { this.data = data; }
}
