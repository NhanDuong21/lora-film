package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingStatusHistoryDto;
import com.lorafilm.booking.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingStatusHistoryService {

    BookingStatusHistoryDto saveHistory(Booking booking, String fromStatus, String toStatus, String reason, String source, String changedBy);

    Page<BookingStatusHistoryDto> findHistory(Pageable pageable);

    List<BookingStatusHistoryDto> findByBooking(Long bookingId);
}
