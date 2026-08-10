package com.lorafilm.movie.seat.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.BulkSeatItemRequest;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;


@SpringBootTest
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class BulkSeatAtomicityIntegrationTest {



    @Autowired
    private SeatService seatService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private SeatTypeRepository seatTypeRepository;

    private String auditoriumPublicId;
    private String seatTypePublicId;

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

        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId(java.util.UUID.randomUUID().toString());
        auditorium.setStatus(AuditoriumStatus.DRAFT);
        auditorium.setName("Test Atomicity");
        auditorium.setCapacity(100);
        auditorium.setCinema(cinema);
        auditorium = auditoriumRepository.saveAndFlush(auditorium);
        auditoriumPublicId = auditorium.getPublicId();

        SeatType seatType = new SeatType();
        seatType.setPublicId(java.util.UUID.randomUUID().toString());
        seatType.setStatus(ActiveStatus.ACTIVE);
        seatType.setCode(SeatTypeCode.STANDARD);
        seatType.setName("Standard Atomicity");
        seatType = seatTypeRepository.saveAndFlush(seatType);
        seatTypePublicId = seatType.getPublicId();
    }

    @Test
    void shouldRollbackEntireBatchIfDataIntegrityViolationOccurs() {
        long initialCount = seatRepository.count();

        BulkSeatItemRequest seat1 = new BulkSeatItemRequest(seatTypePublicId, "A", 1, "A1", 1, 1, null, com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);
        BulkSeatItemRequest seat2 = new BulkSeatItemRequest(seatTypePublicId, "A", 2, "A2", 1, 2, null, com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);
        
        // Let's create seat2 in advance to cause DataIntegrityViolationException on the second insert
        BulkCreateSeatsRequest initialRequest = new BulkCreateSeatsRequest(List.of(seat2));
        seatService.bulkCreateSeats(auditoriumPublicId, initialRequest);

        assertThat(seatRepository.count()).isEqualTo(initialCount + 1);

        BulkSeatItemRequest seat2Invalid = new BulkSeatItemRequest(seatTypePublicId, "TOOLONG", 2, "A2", 1, 2, null, com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);
        BulkCreateSeatsRequest failingRequest = new BulkCreateSeatsRequest(List.of(seat1, seat2Invalid));

        assertThatThrownBy(() -> seatService.bulkCreateSeats(auditoriumPublicId, failingRequest));

        // Verify that seat1 was not inserted because seat2 failed
        assertThat(seatRepository.count()).isEqualTo(initialCount + 1);
        assertThat(seatRepository.findByPublicIdAndDeletedAtIsNull("A1")).isEmpty();
    }
}
