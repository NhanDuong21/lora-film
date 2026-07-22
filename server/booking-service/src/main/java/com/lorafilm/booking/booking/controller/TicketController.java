package com.lorafilm.booking.booking.controller;

import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking Ticket API", description = "Endpoints for managing and retrieving booking tickets")
public class TicketController {

    private final BookingTicketService bookingTicketService;
    private final BookingRepository bookingRepository;

    public TicketController(BookingTicketService bookingTicketService, BookingRepository bookingRepository) {
        this.bookingTicketService = bookingTicketService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/tickets")
    @Operation(summary = "Get tickets by booking public ID", description = "Retrieve all movie tickets associated with a given booking public UUID")
    public ResponseEntity<ApiResponse<List<BookingTicketDto>>> getTicketsByBookingId(@PathVariable String publicId) {
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BookingNotFoundException(UUID.fromString(publicId)));
        List<BookingTicketDto> tickets = bookingTicketService.findByBooking(booking.getId());
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", tickets));
    }
}
