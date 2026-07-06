package com.project.bookingservice.service;

import com.project.bookingservice.dto.request.CreateBookingRequest;
import com.project.bookingservice.dto.response.BookingResponse;
import com.project.bookingservice.enumtype.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, String idempotencyKey);

    BookingResponse getBooking(Long bookingId);

    Page<BookingResponse> getMyBookings(BookingStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    void cancelBooking(Long bookingId, String idempotencyKey);

    java.util.List<com.project.bookingservice.dto.response.TicketResponse> getTicketsByBookingId(Long bookingId);

    com.project.bookingservice.dto.response.TicketResponse getTicketById(Long ticketId);
}
