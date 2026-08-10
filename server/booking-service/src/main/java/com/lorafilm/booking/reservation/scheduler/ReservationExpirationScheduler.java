package com.lorafilm.booking.reservation.scheduler;

import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import com.lorafilm.booking.infrastructure.lock.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ReservationExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final SeatReservationRepository seatReservationRepository;
    private final SeatReservationService seatReservationService;

    public ReservationExpirationScheduler(
            SeatReservationRepository seatReservationRepository,
            SeatReservationService seatReservationService) {
        this.seatReservationRepository = seatReservationRepository;
        this.seatReservationService = seatReservationService;
    }

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "ReservationExpirationScheduler", lockAtMostForSeconds = 55)
    public void processExpiredReservations() {
        log.debug("ReservationExpirationScheduler starting expired reservation check...");
        Instant now = Instant.now();

        try {
            List<SeatReservation> expiredReservations = seatReservationRepository.findExpiredUnlinkedReservations(
                    now, PageRequest.of(0, BATCH_SIZE));
            if (expiredReservations.isEmpty()) {
                log.debug("ReservationExpirationScheduler: No expired reservations found.");
                return;
            }

            List<Long> expiredIds = expiredReservations.stream().map(SeatReservation::getId).toList();
            log.info("ReservationExpirationScheduler: Found {} expired reservations to process.", expiredIds.size());

            seatReservationService.expireReservations(expiredIds);
            log.info("ReservationExpirationScheduler: Successfully processed expiration for {} reservations.", expiredIds.size());
        } catch (Exception ex) {
            log.error("ReservationExpirationScheduler: Error occurred while processing expired reservations: ", ex);
        }
    }
}
