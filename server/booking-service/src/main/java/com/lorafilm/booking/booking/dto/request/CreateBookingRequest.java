package com.lorafilm.booking.booking.dto.request;

import com.lorafilm.booking.common.constant.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Create a booking from active seat reservations")
public class CreateBookingRequest {

    @NotBlank(message = "showtimePublicId is required")
    @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "showtimePublicId must be a valid UUID")
    @Schema(
            description = "Public ID of the showtime",
            example = "550e8400-e29b-41d4-a716-446655440001")
    private String showtimePublicId;

    @NotEmpty(message = "reservationPublicIds cannot be empty")
    @Size(max = 8, message = "A booking cannot contain more than 8 reservations")
    @Schema(
            description = "Public IDs of the active seat reservations",
            example = "[\"8712253d-dc49-4f85-a6db-f99908dd61d7\", \"6f5867c6-9596-4011-844e-183f23e65bb6\"]")
    private List<
            @NotBlank(message = "reservationPublicId cannot be blank")
            @Pattern(
                    regexp = ValidationConstants.UUID_PATTERN,
                    message = "reservationPublicId must be a valid UUID") String> reservationPublicIds;

    public CreateBookingRequest() {
    }

    public CreateBookingRequest(String showtimePublicId, List<String> reservationPublicIds) {
        this.showtimePublicId = showtimePublicId;
        this.reservationPublicIds = reservationPublicIds;
    }

    public String getShowtimePublicId() {
        return showtimePublicId;
    }

    public void setShowtimePublicId(String showtimePublicId) {
        this.showtimePublicId = showtimePublicId;
    }

    public List<String> getReservationPublicIds() {
        return reservationPublicIds;
    }

    public void setReservationPublicIds(List<String> reservationPublicIds) {
        this.reservationPublicIds = reservationPublicIds;
    }

}
