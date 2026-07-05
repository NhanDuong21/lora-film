package com.project.bookingservice.api;

import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.response.TicketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Ticket API", description = "Endpoints for managing tickets")
public interface TicketApi {

    @Operation(summary = "Get tickets by booking ID", description = "Retrieves a list of tickets belonging to a specific booking")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tickets retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/bookings/{bookingId}/tickets")
    ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByBookingId(
            @Parameter(description = "ID of the booking") @PathVariable @Positive(message = "Booking ID must be greater than 0") Long bookingId);

    @Operation(summary = "Get ticket details", description = "Retrieves details of a specific ticket")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/tickets/{ticketId}")
    ResponseEntity<ApiResponse<TicketResponse>> getTicketDetails(
            @Parameter(description = "ID of the ticket") @PathVariable @Positive(message = "Ticket ID must be greater than 0") Long ticketId);
}
