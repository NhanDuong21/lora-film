package com.project.bookingservice.service.movie;

import com.project.bookingservice.dto.movie.SeatInfo;
import com.project.bookingservice.dto.movie.ShowtimeInfo;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile({"local", "test"})
public class MockMovieServiceClient implements MovieServiceClient {

    @Override
    public ShowtimeInfo getShowtime(Long showtimeId) {
        if (showtimeId == 999L) {
            return null; // missing showtime
        }
        if (showtimeId == 888L) {
            return new ShowtimeInfo(888L, 1L, false); // inactive showtime
        }
        return new ShowtimeInfo(showtimeId, 1L, true); // valid showtime in room 1
    }

    @Override
    public List<SeatInfo> getSeats(List<Long> seatIds) {
        return seatIds.stream()
                .filter(id -> !id.equals(999L)) // 999 means missing seat
                .map(id -> {
                    if (id.equals(888L)) {
                        return new SeatInfo(888L, 1L, false); // inactive seat
                    }
                    if (id.equals(777L)) {
                        return new SeatInfo(777L, 2L, true); // room mismatch
                    }
                    return new SeatInfo(id, 1L, true); // valid seat in room 1
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSeatBooked(Long showtimeId, Long seatId) {
        // Mock already booked seat
        return seatId.equals(111L);
    }
}
