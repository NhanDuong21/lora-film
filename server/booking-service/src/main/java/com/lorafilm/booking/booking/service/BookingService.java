package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.request.CancelBookingRequest;
import com.lorafilm.booking.booking.dto.request.CreateBookingRequest;
import com.lorafilm.booking.booking.dto.request.FinalizeCheckoutRequest;
import com.lorafilm.booking.booking.dto.response.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.dto.response.BookingSummaryResponse;
import com.lorafilm.booking.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse cancelBooking(String publicId, CancelBookingRequest request);

    BookingResponse finalizeCheckout(String publicId, FinalizeCheckoutRequest request);

    BookingResponse confirmBooking(String publicId);

    BookingResponse expireBooking(String publicId);

    BookingResponse refundBooking(String publicId);

    BookingDetailResponse findById(String publicId);

    BookingDetailResponse findByCode(String bookingCode);

    Optional<BookingResponse> findActiveByShowtime(String showtimePublicId);

    Page<BookingSummaryResponse> findAll(
            BookingStatus status, Instant fromDate, Instant toDate, Pageable pageable);

    Page<BookingSummaryResponse> findByUser(
            Long userId, BookingStatus status, Instant fromDate, Instant toDate, Pageable pageable);

    BookingResponse changeStatus(String publicId, BookingStatus targetStatus);
}
