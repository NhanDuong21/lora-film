package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;

@Service
public class BookingStatusTransitionService {

    public BookingStatusTransitionService() {
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

        if (!fromStatus.canTransitionTo(toStatus)) {
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
