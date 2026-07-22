package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;

@Service
public class BookingStatusTransitionService {

    private final EnumMap<BookingStatus, EnumSet<BookingStatus>> allowedTransitions = new EnumMap<>(BookingStatus.class);

    public BookingStatusTransitionService() {
        allowedTransitions.put(BookingStatus.PENDING_PAYMENT, EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED, BookingStatus.EXPIRED));
        allowedTransitions.put(BookingStatus.CONFIRMED, EnumSet.of(BookingStatus.CANCELLED, BookingStatus.REFUNDED, BookingStatus.COMPLETED));
        allowedTransitions.put(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class));
        allowedTransitions.put(BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class));
        allowedTransitions.put(BookingStatus.EXPIRED, EnumSet.noneOf(BookingStatus.class));
        allowedTransitions.put(BookingStatus.REFUNDED, EnumSet.noneOf(BookingStatus.class));
    }

    public void validateTransition(BookingStatus fromStatus, BookingStatus toStatus) {
        if (fromStatus == null) {
            throw new BusinessException("INVALID_STATUS", "Current booking status is null");
        }
        if (toStatus == null) {
            throw new BusinessException("INVALID_STATUS", "Target booking status is null");
        }

        if (fromStatus == toStatus) {
            if (fromStatus == BookingStatus.CONFIRMED) {
                throw new BusinessException("BOOKING_ALREADY_CONFIRMED", "Booking is already confirmed");
            }
            throw new BusinessException("SAME_STATUS_TRANSITION", "Booking is already in status: " + toStatus);
        }

        EnumSet<BookingStatus> validNextStates = allowedTransitions.get(fromStatus);
        if (validNextStates == null || !validNextStates.contains(toStatus)) {
            if (toStatus == BookingStatus.CONFIRMED) {
                if (fromStatus == BookingStatus.CANCELLED) {
                    throw new BusinessException("CANNOT_CONFIRM_CANCELLED", "Cancelled booking cannot be confirmed");
                }
                if (fromStatus == BookingStatus.REFUNDED) {
                    throw new BusinessException("CANNOT_CONFIRM_REFUNDED", "Refunded booking cannot be confirmed");
                }
                if (fromStatus == BookingStatus.EXPIRED) {
                    throw new BusinessException("CANNOT_CONFIRM_EXPIRED", "Expired booking cannot be confirmed");
                }
            }
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "Invalid status transition from " + fromStatus + " to " + toStatus);
        }
    }

    public boolean isTransitionAllowed(BookingStatus fromStatus, BookingStatus toStatus) {
        try {
            validateTransition(fromStatus, toStatus);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
}
