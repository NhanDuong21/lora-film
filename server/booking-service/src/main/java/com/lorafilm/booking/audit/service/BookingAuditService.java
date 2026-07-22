package com.lorafilm.booking.audit.service;

import com.lorafilm.booking.audit.entity.BookingAuditLog;

import java.util.List;

public interface BookingAuditService {

    BookingAuditLog logAudit(Long bookingId, String actor, String action, String fieldName, String oldValue, String newValue, String requestId, String traceId, String ipAddress, String userAgent);

    List<BookingAuditLog> findByBooking(Long bookingId);
}
