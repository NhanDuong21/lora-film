package com.lorafilm.movie.common.api.error;

import com.lorafilm.movie.seat.dto.BulkValidationErrorData;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BulkSeatValidationErrorResponse")
public class BulkSeatValidationErrorResponse {

    @Schema(example = "false")
    private boolean success;

    @Schema(example = "One seat is invalid")
    private String message;

    @Schema(example = "BULK_SEAT_VALIDATION_ERROR")
    private String errorCode;

    private BulkValidationErrorData data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public BulkValidationErrorData getData() { return data; }
    public void setData(BulkValidationErrorData data) { this.data = data; }
}
