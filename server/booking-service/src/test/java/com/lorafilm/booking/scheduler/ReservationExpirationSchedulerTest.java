package com.lorafilm.booking.scheduler;

import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.scheduler.ReservationExpirationScheduler;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationExpirationSchedulerTest {

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private ReservationExpirationScheduler scheduler;

    @Test
    public void processExpiredReservations_FindsExpired_CallsExpireService() {
        SeatReservation res1 = new SeatReservation();
        res1.setId(101L);
        SeatReservation res2 = new SeatReservation();
        res2.setId(102L);

        when(seatReservationRepository.findExpiredUnlinkedReservations(any(), any(Pageable.class)))
                .thenReturn(List.of(res1, res2));

        scheduler.processExpiredReservations();

        verify(seatReservationService).expireReservations(List.of(101L, 102L));
    }

    @Test
    public void processExpiredReservations_NoExpired_NoOp() {
        when(seatReservationRepository.findExpiredUnlinkedReservations(any(), any(Pageable.class)))
                .thenReturn(List.of());

        scheduler.processExpiredReservations();

        verify(seatReservationService, never()).expireReservations(anyList());
    }
}
