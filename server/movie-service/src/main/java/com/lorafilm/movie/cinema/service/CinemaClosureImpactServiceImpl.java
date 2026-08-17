package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.dto.CinemaClosureImpactResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaClosurePeriodRequest;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.integration.BookingSeatAvailabilityClient;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CinemaClosureImpactServiceImpl implements CinemaClosureImpactService {

    private final CinemaRepository cinemaRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatAvailabilityClient bookingAvailabilityClient;

    public CinemaClosureImpactServiceImpl(
            CinemaRepository cinemaRepository,
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            BookingSeatAvailabilityClient bookingAvailabilityClient) {
        this.cinemaRepository = cinemaRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.bookingAvailabilityClient = bookingAvailabilityClient;
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaClosureImpactResponse preview(
            String cinemaPublicId,
            CreateCinemaClosurePeriodRequest request) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CINEMA_NOT_FOUND));
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        List<Showtime> showtimes = showtimeRepository.findCinemaPotentialOverlaps(
                cinema.getId(), request.getStartTime(), request.getEndTime());
        Map<Long, List<Long>> seatIdsByAuditorium = new HashMap<>();
        List<CinemaClosureImpactResponse.AffectedShowtime> affected = new ArrayList<>();
        int occupiedSeats = 0;
        boolean bookingDataComplete = true;

        for (Showtime showtime : showtimes) {
            Long auditoriumId = showtime.getAuditorium().getId();
            List<Long> seatIds = seatIdsByAuditorium.computeIfAbsent(
                    auditoriumId,
                    id -> seatRepository.findAdminLayoutByAuditoriumId(id).stream()
                            .map(Seat::getId)
                            .toList());
            BookingSeatAvailabilityClient.AvailabilityResult availability =
                    bookingAvailabilityClient.check(showtime.getId(), seatIds);
            int occupied = availability.verified()
                    ? availability.unavailableSeatIds().size()
                    : 0;
            occupiedSeats += occupied;
            bookingDataComplete = bookingDataComplete && availability.verified();
            affected.add(new CinemaClosureImpactResponse.AffectedShowtime(
                    showtime.getPublicId(),
                    showtime.getAuditorium().getName(),
                    showtime.getMovie().getTitle(),
                    showtime.getStartTime(),
                    showtime.getEndTime(),
                    showtime.getStatus(),
                    occupied,
                    availability.verified()));
        }

        return new CinemaClosureImpactResponse(
                cinema.getPublicId(),
                cinema.getName(),
                request.getStartTime(),
                request.getEndTime(),
                showtimes.size(),
                (int) showtimes.stream()
                        .filter(showtime -> showtime.getStatus() == ShowtimeStatus.OPEN_FOR_BOOKING)
                        .count(),
                (int) showtimes.stream()
                        .filter(showtime -> showtime.getStatus() == ShowtimeStatus.DRAFT)
                        .count(),
                occupiedSeats,
                bookingDataComplete,
                List.copyOf(affected));
    }
}
