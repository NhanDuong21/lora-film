package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CinemaOperatingWindowResolver {

    private final CinemaOperatingHourRepository repository;

    public CinemaOperatingWindowResolver(CinemaOperatingHourRepository repository) {
        this.repository = repository;
    }

    public List<OperatingWindow> resolve(Cinema cinema, LocalDate fromDate, LocalDate toDate) {
        List<OperatingWindow> windows = new ArrayList<>();
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(cinema.getTimezone());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE, "Invalid timezone configured for cinema");
        }

        List<CinemaOperatingHour> hours = repository.findByCinemaId(cinema.getId());

        LocalDate date = fromDate;
        while (!date.isAfter(toDate)) {
            // DayOfWeek value: 1 (Monday) to 7 (Sunday)
            int dow = date.getDayOfWeek().getValue();
            Optional<CinemaOperatingHour> dailyHour = hours.stream()
                    .filter(h -> h.getDayOfWeek() != null && h.getDayOfWeek() == dow)
                    .findFirst();

            if (dailyHour.isPresent() && !Boolean.TRUE.equals(dailyHour.get().getIsClosed())) {
                CinemaOperatingHour h = dailyHour.get();
                if (h.getOpenTime() != null && h.getCloseTime() != null) {
                    ZonedDateTime openTime = date.atTime(h.getOpenTime()).atZone(zoneId);
                    ZonedDateTime closeTime = date.atTime(h.getCloseTime()).atZone(zoneId);
                    
                    if (h.getCloseTime().isBefore(h.getOpenTime())) {
                        closeTime = closeTime.plusDays(1);
                    }
                    
                    windows.add(new OperatingWindow(openTime.toInstant(), closeTime.toInstant()));
                }
            }

            date = date.plusDays(1);
        }

        return windows;
    }
}
