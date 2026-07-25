package com.lorafilm.booking.booking.scheduler;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingService;
import com.lorafilm.booking.infrastructure.lock.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class BookingExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationScheduler.class);
    private static final int BATCH_SIZE = 50;

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public BookingExpirationScheduler(
            BookingRepository bookingRepository,
            BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 10000) // Run every 10 seconds
    @SchedulerLock(name = "BookingExpirationScheduler", lockAtMostForSeconds = 8)
    public void expirePendingBookings() {
        log.debug("BookingExpirationScheduler starting check...");
        Instant now = Instant.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(
                BookingStatus.PENDING_PAYMENT, now, PageRequest.of(0, BATCH_SIZE));

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired bookings to process.", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                bookingService.expireBooking(booking.getPublicId());
                log.info("Successfully expired booking: {}", booking.getBookingCode());
            } catch (Exception e) {
                log.error("Failed to expire booking publicId: {}", booking.getPublicId(), e);
            }
        }
    }
}
