package com.lorafilm.booking.service;

import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookingStatusTransitionServiceTest {

    private BookingStatusTransitionService transitionService;

    @BeforeEach
    public void setUp() {
        transitionService = new BookingStatusTransitionService();
    }

    @Test
    public void validateTransition_PendingToConfirmed_Success() {
        assertDoesNotThrow(() -> transitionService.validateTransition(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED));
        assertTrue(transitionService.isTransitionAllowed(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED));
    }

    @Test
    public void validateTransition_PendingToCancelled_Success() {
        assertDoesNotThrow(() -> transitionService.validateTransition(BookingStatus.PENDING_PAYMENT, BookingStatus.CANCELLED));
        assertTrue(transitionService.isTransitionAllowed(BookingStatus.PENDING_PAYMENT, BookingStatus.CANCELLED));
    }

    @Test
    public void validateTransition_PendingToExpired_Success() {
        assertDoesNotThrow(() -> transitionService.validateTransition(BookingStatus.PENDING_PAYMENT, BookingStatus.EXPIRED));
        assertTrue(transitionService.isTransitionAllowed(BookingStatus.PENDING_PAYMENT, BookingStatus.EXPIRED));
    }

    @Test
    public void validateTransition_ConfirmedToCancelled_IsRejected() {
        assertThrows(BusinessException.class,
                () -> transitionService.validateTransition(BookingStatus.CONFIRMED, BookingStatus.CANCELLED));
    }

    @Test
    public void validateTransition_ConfirmedToRefunded_Success() {
        assertDoesNotThrow(() -> transitionService.validateTransition(BookingStatus.CONFIRMED, BookingStatus.REFUNDED));
    }

    @Test
    public void validateTransition_ConfirmedToCompleted_Success() {
        assertDoesNotThrow(() -> transitionService.validateTransition(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
    }

    @Test
    public void validateTransition_CancelledToConfirmed_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transitionService.validateTransition(BookingStatus.CANCELLED, BookingStatus.CONFIRMED));
        assertEquals("CANNOT_CONFIRM_CANCELLED", ex.getErrorCode());
        assertFalse(transitionService.isTransitionAllowed(BookingStatus.CANCELLED, BookingStatus.CONFIRMED));
    }

    @Test
    public void validateTransition_RefundedToConfirmed_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transitionService.validateTransition(BookingStatus.REFUNDED, BookingStatus.CONFIRMED));
        assertEquals("CANNOT_CONFIRM_REFUNDED", ex.getErrorCode());
    }

    @Test
    public void validateTransition_ExpiredToConfirmed_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transitionService.validateTransition(BookingStatus.EXPIRED, BookingStatus.CONFIRMED));
        assertEquals("CANNOT_CONFIRM_EXPIRED", ex.getErrorCode());
    }

    @Test
    public void validateTransition_ConfirmedToConfirmed_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transitionService.validateTransition(BookingStatus.CONFIRMED, BookingStatus.CONFIRMED));
        assertEquals("BOOKING_ALREADY_CONFIRMED", ex.getErrorCode());
    }

    @Test
    public void validateTransition_CancelledToAny_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transitionService.validateTransition(BookingStatus.CANCELLED, BookingStatus.EXPIRED));
        assertEquals("INVALID_STATUS_TRANSITION", ex.getErrorCode());
    }

    @Test
    public void validateTransition_NullFromStatus_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transitionService.validateTransition(null, BookingStatus.CONFIRMED));
        assertEquals("INVALID_STATUS", ex.getErrorCode());
    }

    @Test
    public void validateTransition_NullToStatus_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transitionService.validateTransition(BookingStatus.PENDING_PAYMENT, null));
        assertEquals("INVALID_STATUS", ex.getErrorCode());
    }
}
