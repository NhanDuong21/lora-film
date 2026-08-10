package com.lorafilm.booking.audit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.audit.entity.BookingOperationLog;
import com.lorafilm.booking.audit.repository.BookingOperationLogRepository;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingOperationLogServiceImpl implements BookingOperationLogService {

    private final BookingOperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BookingOperationLogServiceImpl(BookingOperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Override
    @Transactional
    public BookingOperationLog logOperation(Long bookingId, String operationType, String actor, boolean success, Long executionTimeMs, String errorCode, String errorMessage, String metadata) {
        if (operationType == null || operationType.trim().isEmpty()) {
            throw new BusinessException("INVALID_OPERATION_TYPE", "Operation type cannot be null or empty");
        }

        BookingOperationLog log = new BookingOperationLog();
        log.setPublicId(UUID.randomUUID().toString());
        log.setBookingId(bookingId);
        log.setOperationType(operationType);
        log.setActor(actor != null ? actor : "SYSTEM");
        log.setSuccess(success);
        log.setExecutionTimeMs(executionTimeMs != null ? executionTimeMs : 0L);
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);

        if (metadata != null && !metadata.trim().isEmpty()) {
            String trimmed = metadata.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                log.setMetadata(trimmed);
            } else {
                try {
                    log.setMetadata(objectMapper.writeValueAsString(Map.of("note", trimmed)));
                } catch (Exception e) {
                    log.setMetadata("{\"note\":\"" + trimmed.replace("\"", "\\\"") + "\"}");
                }
            }
        } else {
            log.setMetadata(null);
        }

        return operationLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingOperationLog> findByBooking(Long bookingId) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        return operationLogRepository.findByBookingId(bookingId);
    }
}
