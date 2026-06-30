package com.project.bookingservice.worker;

import com.project.bookingservice.config.BookingProperties;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.repository.SeatReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(prefix = "booking.expiration-worker", name = "enabled", havingValue = "true")
public class ReservationExpirationWorker {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationWorker.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SeatReservationRepository seatReservationRepository;
    private final ExpirationProcessor expirationProcessor;
    private final BookingProperties bookingProperties;

    public ReservationExpirationWorker(SeatReservationRepository seatReservationRepository,
                                       ExpirationProcessor expirationProcessor,
                                       BookingProperties bookingProperties) {
        this.seatReservationRepository = seatReservationRepository;
        this.expirationProcessor = expirationProcessor;
        this.bookingProperties = bookingProperties;
    }

    @Scheduled(fixedDelayString = "${booking.expiration-worker.fixed-delay-ms:30000}")
    public void processExpiredReservations() {
        int batchSize = bookingProperties.getExpirationWorker().getBatchSize();
        LocalDateTime now = LocalDateTime.now(ZONE_ID);

        Page<SeatReservation> expiredPage = seatReservationRepository.findExpiredReservations(
                ReservationStatus.HELD, now, PageRequest.of(0, batchSize));

        if (expiredPage.isEmpty()) {
            return;
        }

        int processed = 0;
        int expired = 0;
        int skipped = 0;
        int failed = 0;

        for (SeatReservation reservation : expiredPage) {
            processed++;
            try {
                expirationProcessor.processReservationExpiration(reservation);
                expired++;
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                log.debug("Optimistic lock failure for reservation {}", reservation.getId());
                skipped++;
            } catch (Exception e) {
                log.error("Failed to expire reservation {}", reservation.getId(), e);
                failed++;
                // Stop the batch if a critical error like Redis failure occurs
                break;
            }
        }

        log.info("[Expiration Worker] Processed: {}, Expired: {}, Skipped: {}, Failed: {}", processed, expired, skipped, failed);
    }
}
