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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Disabled("Blocked by Docker Testcontainers environment issue")
public class BulkSeatAtomicityIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.32");

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

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        
        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId("aud-atomicity-1");
        auditorium.setStatus(AuditoriumStatus.DRAFT);
        auditorium.setName("Test Atomicity");
        auditorium = auditoriumRepository.saveAndFlush(auditorium);
        auditoriumPublicId = auditorium.getPublicId();

        SeatType seatType = new SeatType();
        seatType.setPublicId("type-atomicity-1");
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

        BulkCreateSeatsRequest failingRequest = new BulkCreateSeatsRequest(List.of(seat1, seat2));

        assertThatThrownBy(() -> seatService.bulkCreateSeats(auditoriumPublicId, failingRequest));

        // Verify that seat1 was not inserted because seat2 failed
        assertThat(seatRepository.count()).isEqualTo(initialCount + 1);
        assertThat(seatRepository.findByPublicIdAndDeletedAtIsNull("A1")).isEmpty();
    }
}
