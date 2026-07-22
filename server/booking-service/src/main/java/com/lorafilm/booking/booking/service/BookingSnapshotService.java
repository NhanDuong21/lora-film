package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingSnapshotDto;
import com.lorafilm.booking.booking.dto.CreateSnapshotRequest;

public interface BookingSnapshotService {

    BookingSnapshotDto createSnapshot(Long bookingId, CreateSnapshotRequest request);

    BookingSnapshotDto findByBooking(Long bookingId);
}
