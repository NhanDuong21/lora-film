package com.lorafilm.booking.booking.mapper;

import com.lorafilm.booking.booking.dto.BookingStatusHistoryDto;
import com.lorafilm.booking.booking.entity.BookingStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class BookingStatusHistoryMapper {

    public BookingStatusHistoryDto toDto(BookingStatusHistory history) {
        if (history == null) {
            return null;
        }

        BookingStatusHistoryDto dto = new BookingStatusHistoryDto();
        dto.setId(history.getId());
        if (history.getBooking() != null) {
            dto.setBookingId(history.getBooking().getId());
        }
        dto.setFromStatus(history.getFromStatus());
        dto.setToStatus(history.getToStatus());
        dto.setReason(history.getReason());
        dto.setSource(history.getSource());
        dto.setChangedBy(history.getChangedBy());
        dto.setCreatedAt(history.getCreatedAt());
        return dto;
    }
}
