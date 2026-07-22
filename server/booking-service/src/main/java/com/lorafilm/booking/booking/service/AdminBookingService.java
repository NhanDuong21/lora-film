package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.UpdateBookingStatusRequest;
import com.lorafilm.booking.common.response.PagedResponse;

public interface AdminBookingService {

    PagedResponse<BookingAdminResponse> findBookings(BookingFilterRequest filter);

    BookingDetailResponse getBookingDetail(String publicId);

    BookingAdminResponse updateBookingStatus(String publicId, UpdateBookingStatusRequest request);
}
