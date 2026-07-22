package com.lorafilm.booking.audit.service;

import com.lorafilm.booking.audit.entity.BookingOperationLog;

import java.util.List;

public interface BookingOperationLogService {

    BookingOperationLog logOperation(Long bookingId, String operationType, String actor, boolean success, Long executionTimeMs, String errorCode, String errorMessage, String metadata);

    List<BookingOperationLog> findByBooking(Long bookingId);
}
