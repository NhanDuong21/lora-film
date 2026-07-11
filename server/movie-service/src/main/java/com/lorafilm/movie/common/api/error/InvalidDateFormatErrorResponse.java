package com.lorafilm.movie.common.api.error;

import com.lorafilm.movie.common.api.InvalidDateFormatData;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InvalidDateFormatErrorResponse")
public class InvalidDateFormatErrorResponse {

    @Schema(example = "false")
    private boolean success;

    @Schema(example = "Định dạng thời gian không hợp lệ")
    private String message;

    @Schema(example = "INVALID_DATE_TIME_FORMAT")
    private String errorCode;

    private InvalidDateFormatData data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public InvalidDateFormatData getData() { return data; }
    public void setData(InvalidDateFormatData data) { this.data = data; }
}
