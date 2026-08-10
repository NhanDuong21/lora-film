package com.lorafilm.booking.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional cancellation reason")
public class CancelBookingRequest {

    @Size(max = 50, message = "reasonCode must not exceed 50 characters")
    @Schema(example = "USER_CANCEL")
    private String reasonCode;

    @Size(max = 2000, message = "reasonDetail must not exceed 2000 characters")
    @Schema(example = "Customer changed plans")
    private String reasonDetail;

    public CancelBookingRequest() {
    }

    public CancelBookingRequest(String reasonCode, String reasonDetail) {
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }
}
