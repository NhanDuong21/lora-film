package com.project.bookingservice.controller;

import com.project.bookingservice.api.TicketApi;
import com.project.bookingservice.common.ApiResponse;
import com.project.bookingservice.dto.response.TicketResponse;
import com.project.bookingservice.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController implements TicketApi {

    private final BookingService bookingService;

    public TicketController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{ticketId}")
    @Override
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable Long ticketId) {
        TicketResponse response = bookingService.getTicketById(ticketId);
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved successfully", response));
    }
}
