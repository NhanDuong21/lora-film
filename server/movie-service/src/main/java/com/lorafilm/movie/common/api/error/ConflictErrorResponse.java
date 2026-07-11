package com.lorafilm.movie.common.api.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConflictErrorResponse")
public class ConflictErrorResponse {

    @Schema(example = "false")
    private boolean success;

    @Schema(example = "Dữ liệu bị trùng lặp hoặc xung đột")
    private String message;

    @Schema(example = "DUPLICATE_SEAT_CODE")
    private String errorCode;

    @Schema(nullable = true)
    private Object data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
