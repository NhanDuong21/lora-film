package com.lorafilm.movie.showtime.dto.request;

import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateShowtimeStatusRequest {
    
    @NotNull(message = "Status cannot be null")
    private ShowtimeStatus status;
    
    @Size(max = 255, message = "Cancellation reason must not exceed 255 characters")
    private String reason;

    public UpdateShowtimeStatusRequest() {}

    public ShowtimeStatus getStatus() {
        return status;
    }

    public void setStatus(ShowtimeStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
