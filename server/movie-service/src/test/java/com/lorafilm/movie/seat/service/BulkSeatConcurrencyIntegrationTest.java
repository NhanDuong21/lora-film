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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class BulkSeatConcurrencyIntegrationTest {



    @Autowired
    private SeatService seatService;

    @Autowired
    private SeatRepository seatRepository;

    @org.springframework.boot.test.mock.mockito.SpyBean
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
        auditorium.setName("Test Concurrency");
        auditorium.setCapacity(100);
        auditorium.setCinema(cinema);
        auditorium = auditoriumRepository.saveAndFlush(auditorium);
        auditoriumPublicId = auditorium.getPublicId();

        SeatType seatType = new SeatType();
        seatType.setPublicId(java.util.UUID.randomUUID().toString());
        seatType.setStatus(ActiveStatus.ACTIVE);
        seatType.setCode(SeatTypeCode.STANDARD);
        seatType.setName("Standard Concurrency");
        seatType = seatTypeRepository.saveAndFlush(seatType);
        seatTypePublicId = seatType.getPublicId();
    }

    @Test
    void shouldPreventConcurrentBulkSeatCreation() throws InterruptedException {
        int numberOfThreads = 3;
        Auditorium lockedAuditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(auditoriumPublicId).orElseThrow();
        java.util.concurrent.atomic.AtomicBoolean lockAcquired = new java.util.concurrent.atomic.AtomicBoolean(false);
        CountDownLatch contendersObserved = new CountDownLatch(numberOfThreads - 1);
        org.mockito.Mockito.doAnswer(invocation -> {
            if (lockAcquired.compareAndSet(false, true)) {
                if (!contendersObserved.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent lock contenders did not arrive in time");
                }
                if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                    org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                lockAcquired.set(false);
                            }
                        }
                    );
                } else {
                    lockAcquired.set(false);
                }
                return java.util.Optional.of(lockedAuditorium);
            } else {
                contendersObserved.countDown();
                throw new org.springframework.dao.CannotAcquireLockException("Lock acquisition timeout");
            }
        }).when(auditoriumRepository).findByPublicIdAndDeletedAtIsNullForUpdate(org.mockito.ArgumentMatchers.anyString());

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
                    e.printStackTrace();
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
