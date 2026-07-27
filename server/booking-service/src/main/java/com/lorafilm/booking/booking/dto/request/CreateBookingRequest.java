package com.lorafilm.booking.booking.dto.request;

import com.lorafilm.booking.common.constant.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Schema(description = "Create a Booking atomically from public seat identities")
public class CreateBookingRequest {

    @NotBlank(message = "showtimePublicId is required")
    @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "showtimePublicId must be a valid UUID")
    @Schema(
            description = "Public ID of the showtime",
            example = "550e8400-e29b-41d4-a716-446655440001")
    private String showtimePublicId;

    @Schema(description = "Canonical public seat IDs.  The server validates the configured maximum.")
    private List<
            @NotBlank(message = "seatPublicId cannot be blank")
            @Pattern(
                    regexp = ValidationConstants.UUID_PATTERN,
                    message = "seatPublicId must be a valid UUID") String> seatPublicIds;

    @Schema(
            description = "Deprecated compatibility IDs of existing HELD reservations",
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

    public CreateBookingRequest(String showtimePublicId, List<String> seatPublicIds, boolean canonical) {
        this.showtimePublicId = showtimePublicId;
        this.seatPublicIds = seatPublicIds;
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

    public List<String> getSeatPublicIds() {
        return seatPublicIds;
    }

    public void setSeatPublicIds(List<String> seatPublicIds) {
        this.seatPublicIds = seatPublicIds;
    }

}
