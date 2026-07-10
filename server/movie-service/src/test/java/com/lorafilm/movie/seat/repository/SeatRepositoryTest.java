package com.lorafilm.movie.seat.repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SeatRepositoryTest {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatTypeRepository seatTypeRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    private Auditorium auditorium;
    private SeatType seatType;

    @BeforeEach
    void setUp() {
        Cinema cinema = new Cinema();
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema.setName("Test Cinema");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema = cinemaRepository.save(cinema);

        auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinema);
        auditorium.setName("Screen 1");
        auditorium.setCapacity(100);
        auditorium.setCleaningBufferMinutes(15);
        auditorium.setScreenType(ScreenType.STANDARD);
        auditorium.setSoundType(SoundType.STANDARD);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium = auditoriumRepository.save(auditorium);

        seatType = new SeatType();
        seatType.setPublicId(UUID.randomUUID().toString());
        seatType.setCode(SeatTypeCode.STANDARD);
        seatType.setName("Standard Seat");
        seatType.setStatus(ActiveStatus.ACTIVE);
        seatType = seatTypeRepository.save(seatType);
    }

    @Test
    void shouldFindLayoutByAuditoriumId() {
        Seat seat1 = new Seat();
        seat1.setPublicId(UUID.randomUUID().toString());
        seat1.setAuditorium(auditorium);
        seat1.setSeatType(seatType);
        seat1.setRowLabel("A");
        seat1.setSeatNumber(1);
        seat1.setSeatCode("A1");
        seat1.setPositionRow(1);
        seat1.setPositionColumn(1);
        seat1.setStatus(SeatStatus.ACTIVE);
        seatRepository.save(seat1);

        Seat seat2 = new Seat();
        seat2.setPublicId(UUID.randomUUID().toString());
        seat2.setAuditorium(auditorium);
        seat2.setSeatType(seatType);
        seat2.setRowLabel("A");
        seat2.setSeatNumber(2);
        seat2.setSeatCode("A2");
        seat2.setPositionRow(1);
        seat2.setPositionColumn(2);
        seat2.setStatus(SeatStatus.ACTIVE);
        seatRepository.save(seat2);

        List<Seat> layout = seatRepository.findAdminLayoutByAuditoriumId(auditorium.getId());

        assertThat(layout).hasSize(2);
        assertThat(layout.get(0).getSeatCode()).isEqualTo("A1");
        assertThat(layout.get(1).getSeatCode()).isEqualTo("A2");
    }
}
