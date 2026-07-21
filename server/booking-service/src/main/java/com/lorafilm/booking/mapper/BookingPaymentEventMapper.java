package com.lorafilm.booking.mapper;

import com.lorafilm.booking.domain.entity.BookingPaymentEvent;
import com.lorafilm.booking.dto.response.BookingPaymentEventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingPaymentEventMapper {
    BookingPaymentEventResponse toResponse(BookingPaymentEvent bookingPaymentEvent);
}
