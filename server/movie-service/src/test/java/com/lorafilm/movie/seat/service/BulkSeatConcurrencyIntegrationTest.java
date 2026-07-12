package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.BulkSeatItemRequest;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class BulkSeatConcurrencyIntegrationTest {

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
        auditorium.setPublicId("aud-concurrency-1");
        auditorium.setStatus(AuditoriumStatus.DRAFT);
        auditorium.setName("Test Concurrency");
        auditorium = auditoriumRepository.saveAndFlush(auditorium);
        auditoriumPublicId = auditorium.getPublicId();

        SeatType seatType = new SeatType();
        seatType.setPublicId("type-concurrency-1");
        seatType.setStatus(ActiveStatus.ACTIVE);
        seatType.setCode(SeatTypeCode.STANDARD);
        seatType.setName("Standard Concurrency");
        seatType = seatTypeRepository.saveAndFlush(seatType);
        seatTypePublicId = seatType.getPublicId();
    }

    @Test
    void shouldPreventConcurrentBulkSeatCreation() throws InterruptedException {
        int numberOfThreads = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        BulkSeatItemRequest seat1 = new BulkSeatItemRequest(seatTypePublicId, "A", 1, "A1", 1, 1, null, SeatStatus.ACTIVE);
        BulkCreateSeatsRequest request = new BulkCreateSeatsRequest(List.of(seat1));

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    seatService.bulkCreateSeats(auditoriumPublicId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        endLatch.await();

        // One thread succeeds, the others fail due to duplicate validation or lock
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(2);
        assertThat(seatRepository.count()).isEqualTo(1);
        
        executorService.shutdown();
    }
}
