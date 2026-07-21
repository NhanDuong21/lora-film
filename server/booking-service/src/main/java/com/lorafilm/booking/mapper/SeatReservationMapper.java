package com.lorafilm.booking.mapper;

import com.lorafilm.booking.domain.entity.SeatReservation;
import com.lorafilm.booking.dto.response.SeatReservationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SeatReservationMapper {
    SeatReservationResponse toResponse(SeatReservation seatReservation);
}
