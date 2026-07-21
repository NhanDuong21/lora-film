package com.lorafilm.booking.mapper;

import com.lorafilm.booking.domain.entity.BookingSnapshot;
import com.lorafilm.booking.dto.response.BookingSnapshotResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingSnapshotMapper {
    BookingSnapshotResponse toResponse(BookingSnapshot bookingSnapshot);
}
