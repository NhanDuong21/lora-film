package com.project.bookingservice.service.movie;

import com.project.bookingservice.dto.movie.SeatInfo;
import com.project.bookingservice.dto.movie.ShowtimeInfo;

import java.util.List;

public interface MovieServiceClient {
    ShowtimeInfo getShowtime(Long showtimeId);

    List<SeatInfo> getSeats(List<Long> seatIds);

    boolean isSeatBooked(Long showtimeId, Long seatId);
}
