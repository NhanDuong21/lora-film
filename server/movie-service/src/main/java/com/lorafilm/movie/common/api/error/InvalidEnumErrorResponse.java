package com.lorafilm.movie.common.api.error;

import com.lorafilm.movie.common.api.InvalidEnumErrorData;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InvalidEnumErrorResponse")
public class InvalidEnumErrorResponse {

    @Schema(example = "false")
    private boolean success;

    @Schema(example = "Invalid enum value")
    private String message;

    @Schema(example = "INVALID_ENUM_VALUE")
    private String errorCode;

    private InvalidEnumErrorData data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public InvalidEnumErrorData getData() { return data; }
    public void setData(InvalidEnumErrorData data) { this.data = data; }
}
