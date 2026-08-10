package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.dto.CustomerSeatLayoutResponse;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class CustomerSeatLayoutIntegrationTest {



    @Autowired
    private SeatLayoutQueryService queryService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private SeatTypeRepository seatTypeRepository;

    private Auditorium auditorium;
    private SeatType activeType;
    private SeatType inactiveType;

    @Autowired
    private com.lorafilm.movie.cinema.repository.CinemaRepository cinemaRepository;

    private com.lorafilm.movie.cinema.domain.entity.Cinema cinema;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE seats");
        jdbcTemplate.execute("TRUNCATE TABLE seat_types");
        jdbcTemplate.execute("TRUNCATE TABLE auditoriums");
        jdbcTemplate.execute("TRUNCATE TABLE cinemas");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        cinema = new com.lorafilm.movie.cinema.domain.entity.Cinema();
        cinema.setPublicId(java.util.UUID.randomUUID().toString());
        cinema.setSlug("cinema-" + System.currentTimeMillis());
        cinema.setName("Test Cinema");
        cinema.setCity("Test City");
        cinema.setAddress("Test Address");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE);
        cinema = cinemaRepository.saveAndFlush(cinema);

        auditorium = new Auditorium();
        auditorium.setPublicId("aud-layout-1");
        auditorium.setStatus(AuditoriumStatus.ACTIVE);
        auditorium.setName("Layout Auditorium");
        auditorium.setCapacity(100);
        auditorium.setCinema(cinema);
        auditorium = auditoriumRepository.saveAndFlush(auditorium);

        activeType = new SeatType();
        activeType.setPublicId("type-active");
        activeType.setStatus(ActiveStatus.ACTIVE);
        activeType.setCode(SeatTypeCode.STANDARD);
        activeType.setName("Standard Active");
        activeType = seatTypeRepository.saveAndFlush(activeType);

        inactiveType = new SeatType();
        inactiveType.setPublicId("type-inactive");
        inactiveType.setStatus(ActiveStatus.INACTIVE);
        inactiveType.setCode(SeatTypeCode.VIP);
        inactiveType.setName("VIP Inactive");
        inactiveType = seatTypeRepository.saveAndFlush(inactiveType);
    }

    @Test
    void customerLayoutShouldExcludeInactiveSeatsAndInactiveTypes() {
        // Seat 1: Active Type, Active Seat -> Should be included
        Seat seat1 = new Seat();
        seat1.setPublicId(UUID.randomUUID().toString());
        seat1.setAuditorium(auditorium);
        seat1.setSeatType(activeType);
        seat1.setRowLabel("A");
        seat1.setSeatNumber(1);
        seat1.setSeatCode("A1");
        seat1.setPositionRow(1);
        seat1.setPositionColumn(1);
        seat1.setStatus(SeatStatus.ACTIVE);
        seatRepository.save(seat1);

        // Seat 2: Active Type, Maintenance Seat -> Should be included
        Seat seat2 = new Seat();
        seat2.setPublicId(UUID.randomUUID().toString());
        seat2.setAuditorium(auditorium);
        seat2.setSeatType(activeType);
        seat2.setRowLabel("A");
        seat2.setSeatNumber(2);
        seat2.setSeatCode("A2");
        seat2.setPositionRow(1);
        seat2.setPositionColumn(2);
        seat2.setStatus(SeatStatus.MAINTENANCE);
        seatRepository.save(seat2);

        // Seat 3: Active Type, Inactive Seat -> Should NOT be included
        Seat seat3 = new Seat();
        seat3.setPublicId(UUID.randomUUID().toString());
        seat3.setAuditorium(auditorium);
        seat3.setSeatType(activeType);
        seat3.setRowLabel("A");
        seat3.setSeatNumber(3);
        seat3.setSeatCode("A3");
        seat3.setPositionRow(1);
        seat3.setPositionColumn(3);
        seat3.setStatus(SeatStatus.INACTIVE);
        seatRepository.save(seat3);

        // Seat 4: Inactive Type, Active Seat -> Should NOT be included
        Seat seat4 = new Seat();
        seat4.setPublicId(UUID.randomUUID().toString());
        seat4.setAuditorium(auditorium);
        seat4.setSeatType(inactiveType);
        seat4.setRowLabel("A");
        seat4.setSeatNumber(4);
        seat4.setSeatCode("A4");
        seat4.setPositionRow(1);
        seat4.setPositionColumn(4);
        seat4.setStatus(SeatStatus.ACTIVE);
        seatRepository.save(seat4);

        seatRepository.flush();

        CustomerSeatLayoutResponse response = queryService.getCustomerSeatLayout(auditorium.getPublicId());

        assertThat(response.rows()).hasSize(1);
        assertThat(response.rows().get(0).seats())
                .extracting("seatCode")
                .containsExactlyInAnyOrder("A1", "A2");
    }
}
