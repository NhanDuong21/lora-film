package com.lorafilm.booking.mapper;

import com.lorafilm.booking.domain.entity.Booking;
import com.lorafilm.booking.dto.response.BookingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {
    BookingResponse toResponse(Booking booking);
}
