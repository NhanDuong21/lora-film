package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.SeatResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatRequest;

import java.util.List;

public interface SeatService {
    List<SeatResponse> bulkCreateSeats(String auditoriumPublicId, BulkCreateSeatsRequest request);
    SeatResponse updateSeat(String seatPublicId, UpdateSeatRequest request);

    List<Seat> getSeatsByAuditoriumId(Long auditoriumId);
    List<Seat> getSeatsByIds(List<Long> seatIds);
    List<Seat> getSeatsByPublicIds(List<String> seatPublicIds);
}
