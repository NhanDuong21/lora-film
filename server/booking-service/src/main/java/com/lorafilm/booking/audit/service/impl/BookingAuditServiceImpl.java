package com.lorafilm.booking.audit.service.impl;

import com.lorafilm.booking.audit.entity.BookingAuditLog;
import com.lorafilm.booking.audit.repository.BookingAuditLogRepository;
import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BookingAuditServiceImpl implements BookingAuditService {

    private final BookingAuditLogRepository auditLogRepository;

    public BookingAuditServiceImpl(BookingAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public BookingAuditLog logAudit(Long bookingId, String actor, String action, String fieldName, String oldValue, String newValue, String requestId, String traceId, String ipAddress, String userAgent) {
        BookingAuditLog auditLog = new BookingAuditLog();
        auditLog.setPublicId(UUID.randomUUID().toString());
        auditLog.setBookingId(bookingId);
        auditLog.setActor(actor != null ? actor : "SYSTEM");
        auditLog.setAction(action != null ? action : "AUDIT_ACTION");
        auditLog.setFieldName(fieldName);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setRequestId(requestId);
        auditLog.setTraceId(traceId);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);

        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingAuditLog> findByBooking(Long bookingId) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        return auditLogRepository.findByBookingId(bookingId);
    }
}
