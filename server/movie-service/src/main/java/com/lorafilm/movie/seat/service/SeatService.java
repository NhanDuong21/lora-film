package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.SeatResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatRequest;
import com.lorafilm.movie.seat.dto.UpdateSeatStatusRequest;
import java.util.List;

public interface SeatService {
    List<SeatResponse> bulkCreateSeats(String auditoriumPublicId, BulkCreateSeatsRequest request);
    SeatResponse updateSeat(String seatPublicId, UpdateSeatRequest request);
    SeatResponse updateSeatStatus(String seatPublicId, UpdateSeatStatusRequest request);
}
