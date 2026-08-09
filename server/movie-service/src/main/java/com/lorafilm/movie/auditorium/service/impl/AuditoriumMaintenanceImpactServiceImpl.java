package com.lorafilm.movie.auditorium.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.MaintenanceImpactResponse;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceImpactService;
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
import java.util.List;

@Service
public class AuditoriumMaintenanceImpactServiceImpl implements AuditoriumMaintenanceImpactService {

    private final AuditoriumRepository auditoriumRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatAvailabilityClient bookingAvailabilityClient;

    public AuditoriumMaintenanceImpactServiceImpl(
            AuditoriumRepository auditoriumRepository,
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            BookingSeatAvailabilityClient bookingAvailabilityClient) {
        this.auditoriumRepository = auditoriumRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.bookingAvailabilityClient = bookingAvailabilityClient;
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceImpactResponse preview(
            String auditoriumPublicId,
            CreateMaintenanceWindowRequest request) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));
        if (request.startTime() == null || request.endTime() == null
                || !request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(
                    ErrorCode.INVALID_MAINTENANCE_TIME_RANGE,
                    "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        List<Showtime> showtimes = showtimeRepository.findPotentialOverlaps(
                auditorium.getId(), request.startTime(), request.endTime());
        List<Long> seatIds = seatRepository.findAdminLayoutByAuditoriumId(auditorium.getId())
                .stream().map(Seat::getId).toList();
        List<MaintenanceImpactResponse.AffectedShowtime> affected = new ArrayList<>();
        boolean bookingDataComplete = true;
        int occupiedSeats = 0;

        for (Showtime showtime : showtimes) {
            BookingSeatAvailabilityClient.AvailabilityResult availability =
                    bookingAvailabilityClient.check(showtime.getId(), seatIds);
            int occupiedSeatCount = availability.verified()
                    ? availability.unavailableSeatIds().size()
                    : 0;
            bookingDataComplete = bookingDataComplete && availability.verified();
            occupiedSeats += occupiedSeatCount;
            affected.add(new MaintenanceImpactResponse.AffectedShowtime(
                    showtime.getPublicId(),
                    showtime.getMovie().getTitle(),
                    showtime.getStartTime(),
                    showtime.getEndTime(),
                    showtime.getStatus(),
                    occupiedSeatCount,
                    availability.verified()));
        }

        return new MaintenanceImpactResponse(
                auditorium.getPublicId(),
                auditorium.getName(),
                request.startTime(),
                request.endTime(),
                showtimes.size(),
                (int) showtimes.stream().filter(value -> value.getStatus() == ShowtimeStatus.OPEN_FOR_BOOKING).count(),
                (int) showtimes.stream().filter(value -> value.getStatus() == ShowtimeStatus.DRAFT).count(),
                occupiedSeats,
                bookingDataComplete,
                affected);
    }
}
