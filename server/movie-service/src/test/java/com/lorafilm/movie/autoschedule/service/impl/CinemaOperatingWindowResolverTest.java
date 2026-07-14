package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaOperatingWindowResolverTest {

    @Mock
    private CinemaOperatingHourRepository repository;

    @InjectMocks
    private CinemaOperatingWindowResolver resolver;

    private Cinema cinema;

    @BeforeEach
    void setUp() {
        cinema = new Cinema();
        cinema.setId(1L);
        cinema.setTimezone("Asia/Ho_Chi_Minh");
    }

    @Test
    void resolve_returnsCorrectWindows() {
        CinemaOperatingHour monday = new CinemaOperatingHour();
        monday.setDayOfWeek(1);
        monday.setOpenTime(LocalTime.of(8, 0));
        monday.setCloseTime(LocalTime.of(23, 0));
        monday.setIsClosed(false);

        CinemaOperatingHour tuesday = new CinemaOperatingHour();
        tuesday.setDayOfWeek(2);
        tuesday.setOpenTime(LocalTime.of(9, 0));
        tuesday.setCloseTime(LocalTime.of(2, 0)); // Crosses midnight
        tuesday.setIsClosed(false);

        CinemaOperatingHour wednesday = new CinemaOperatingHour();
        wednesday.setDayOfWeek(3);
        wednesday.setIsClosed(true); // Closed

        when(repository.findByCinemaId(1L)).thenReturn(List.of(monday, tuesday, wednesday));

        // Monday to Wednesday
        LocalDate from = LocalDate.of(2023, 10, 2); // 2023-10-02 is Monday
        LocalDate to = LocalDate.of(2023, 10, 4); // Wednesday

        List<OperatingWindow> windows = resolver.resolve(cinema, from, to);

        assertEquals(2, windows.size());
        
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        
        // Monday window
        assertEquals(from.atTime(8, 0).atZone(zone).toInstant(), windows.get(0).getOpenInstant());
        assertEquals(from.atTime(23, 0).atZone(zone).toInstant(), windows.get(0).getCloseInstant());

        // Tuesday window (crosses midnight)
        LocalDate tueDate = LocalDate.of(2023, 10, 3);
        assertEquals(tueDate.atTime(9, 0).atZone(zone).toInstant(), windows.get(1).getOpenInstant());
        assertEquals(tueDate.plusDays(1).atTime(2, 0).atZone(zone).toInstant(), windows.get(1).getCloseInstant());
    }
}
