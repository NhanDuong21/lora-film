package com.lorafilm.booking.realtime;

import com.lorafilm.booking.reservation.entity.SeatReservation;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converts reservation mutations into post-transaction domain events.
 */
@Service
public class SeatAvailabilityEventService {

    private static final Logger log = LoggerFactory.getLogger(SeatAvailabilityEventService.class);
    private final ApplicationEventPublisher eventPublisher;

    public SeatAvailabilityEventService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(Collection<SeatReservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return;
        }

        var grouped = reservations.stream()
                .filter(Objects::nonNull)
                .filter(row -> hasPublicIdentity(row))
                .collect(Collectors.groupingBy(SeatReservation::getShowtimePublicId));

        log.debug("Publishing seat availability change: inputRows={}, showtimes={}, publicRows={}",
                reservations.size(), grouped.keySet(),
                grouped.values().stream().mapToInt(Collection::size).sum());
        grouped.forEach((showtimePublicId, rows) -> eventPublisher.publishEvent(
                        new SeatAvailabilityChangedEvent(
                                showtimePublicId,
                                Instant.now(),
                                rows.stream()
                                        .map(row -> new SeatAvailabilityChangedEvent.SeatUpdate(
                                                row.getSeatPublicId(),
                                                row.getStatus() == null ? null : row.getStatus().name(),
                                                row.getExpiresAt()))
                                        .toList())));
    }

    private boolean hasPublicIdentity(SeatReservation row) {
        return row.getShowtimePublicId() != null
                && !row.getShowtimePublicId().isBlank()
                && row.getSeatPublicId() != null
                && !row.getSeatPublicId().isBlank()
                && row.getStatus() != null;
    }
}
