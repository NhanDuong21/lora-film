package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {
    
    private final SeatRepository seatRepository;
    
    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }
    
    public List<Seat> getSeatsByAuditoriumId(Long auditoriumId) {
        return seatRepository.findByAuditoriumIdAndDeletedAtIsNull(auditoriumId);
    }

    public List<Seat> getSeatsByIds(List<Long> seatIds) {
        return seatRepository.findByIdInAndDeletedAtIsNull(seatIds);
    }
}
