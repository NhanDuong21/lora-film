package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.seat.dto.CreateSeatTypeRequest;
import com.lorafilm.movie.seat.dto.SeatTypeResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatTypeRequest;

public interface SeatTypeService {
    SeatTypeResponse createSeatType(CreateSeatTypeRequest request);
    SeatTypeResponse updateSeatType(String publicId, UpdateSeatTypeRequest request);
    java.util.List<SeatTypeResponse> getAllSeatTypes();
}
