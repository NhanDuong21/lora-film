package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking Ticket API", description = "Endpoints for managing and retrieving booking tickets")
public class TicketController {

    private final BookingTicketService bookingTicketService;

    public TicketController(BookingTicketService bookingTicketService) {
        this.bookingTicketService = bookingTicketService;
    }

    @GetMapping("/{bookingId}/tickets")
    @Operation(summary = "Get tickets by booking ID", description = "Retrieve all movie tickets associated with a given booking ID")
    public ResponseEntity<ApiResponse<List<BookingTicketDto>>> getTicketsByBookingId(@PathVariable Long bookingId) {
        List<BookingTicketDto> tickets = bookingTicketService.findByBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", tickets));
    }
}
