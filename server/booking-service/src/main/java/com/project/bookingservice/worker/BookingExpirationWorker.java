package com.project.bookingservice.worker;

import com.project.bookingservice.config.BookingProperties;
import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.repository.BookingRepository;
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
public class BookingExpirationWorker {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationWorker.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookingRepository bookingRepository;
    private final ExpirationProcessor expirationProcessor;
    private final BookingProperties bookingProperties;

    public BookingExpirationWorker(BookingRepository bookingRepository,
                                   ExpirationProcessor expirationProcessor,
                                   BookingProperties bookingProperties) {
        this.bookingRepository = bookingRepository;
        this.expirationProcessor = expirationProcessor;
        this.bookingProperties = bookingProperties;
    }

    @Scheduled(fixedDelayString = "${booking.expiration-worker.fixed-delay-ms:30000}")
    public void processExpiredBookings() {
        int batchSize = bookingProperties.getExpirationWorker().getBatchSize();
        LocalDateTime now = LocalDateTime.now(ZONE_ID);

        Page<Booking> expiredPage = bookingRepository.findExpiredBookings(
                BookingStatus.PENDING_PAYMENT, now, PageRequest.of(0, batchSize));

        if (expiredPage.isEmpty()) {
            return;
        }

        int processed = 0;
        int expired = 0;
        int skipped = 0;
        int failed = 0;

        for (Booking booking : expiredPage) {
            processed++;
            try {
                expirationProcessor.processBookingExpiration(booking);
                expired++;
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                log.debug("Optimistic lock failure for booking {}", booking.getId());
                skipped++;
            } catch (Exception e) {
                log.error("Failed to expire booking {}", booking.getId(), e);
                failed++;
                break;
            }
        }

        log.info("[Expiration Worker] Processed: {}, Expired: {}, Skipped: {}, Failed: {}", processed, expired, skipped, failed);
    }
}
