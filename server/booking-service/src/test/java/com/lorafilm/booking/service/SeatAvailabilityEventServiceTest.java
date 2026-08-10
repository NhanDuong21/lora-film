package com.lorafilm.booking.service;

import com.lorafilm.booking.realtime.SeatAvailabilityChangedEvent;
import com.lorafilm.booking.realtime.SeatAvailabilityEventService;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SeatAvailabilityEventServiceTest {

    @Test
    void publishesPublicSeatUpdatesGroupedByShowtime() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        SeatAvailabilityEventService service = new SeatAvailabilityEventService(publisher);

        SeatReservation first = reservation("showtime-a", "seat-a", SeatReservationStatus.HELD);
        SeatReservation second = reservation("showtime-a", "seat-b", SeatReservationStatus.BOOKED);
        SeatReservation legacy = reservation(null, "seat-c", SeatReservationStatus.HELD);

        service.publish(List.of(first, second, legacy));

        ArgumentCaptor<SeatAvailabilityChangedEvent> captor =
                ArgumentCaptor.forClass(SeatAvailabilityChangedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        SeatAvailabilityChangedEvent event = captor.getValue();
        assertEquals("showtime-a", event.showtimePublicId());
        assertEquals(2, event.seats().size());
        assertEquals("seat-a", event.seats().get(0).seatPublicId());
        assertEquals("HELD", event.seats().get(0).status());
    }

    private SeatReservation reservation(String showtimePublicId, String seatPublicId,
                                        SeatReservationStatus status) {
        SeatReservation reservation = new SeatReservation();
        reservation.setShowtimePublicId(showtimePublicId);
        reservation.setSeatPublicId(seatPublicId);
        reservation.setStatus(status);
        reservation.setExpiresAt(Instant.now().plusSeconds(900));
        return reservation;
    }
}
