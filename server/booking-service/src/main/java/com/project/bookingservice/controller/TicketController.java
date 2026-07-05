package com.project.bookingservice.controller;

import com.project.bookingservice.api.TicketApi;
import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.response.TicketResponse;
import com.project.bookingservice.service.TicketService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TicketController implements TicketApi {

    private final TicketService ticketService;
    private final com.project.bookingservice.security.CurrentUserProvider currentUserProvider;

    public TicketController(TicketService ticketService, com.project.bookingservice.security.CurrentUserProvider currentUserProvider) {
        this.ticketService = ticketService;
        this.currentUserProvider = currentUserProvider;
    }

    private Long getCurrentUserId() {
        return currentUserProvider.getCurrentUserId();
    }


    @Override
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getTicketsByBookingId(
            @PathVariable @Positive(message = "Booking ID must be greater than 0") Long bookingId) {
        List<TicketResponse> tickets = ticketService.getTicketsByBookingId(bookingId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", tickets));
    }

    @Override
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketDetails(
            @PathVariable @Positive(message = "Ticket ID must be greater than 0") Long ticketId) {
        TicketResponse ticket = ticketService.getTicketDetails(ticketId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved successfully", ticket));
    }
}
