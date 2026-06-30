package com.project.bookingservice.worker;

import com.project.bookingservice.config.BookingProperties;
import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.ReservationStatus;
import com.project.bookingservice.repository.SeatReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReservationExpirationWorkerTest {

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @Mock
    private ExpirationProcessor expirationProcessor;

    @Mock
    private BookingProperties bookingProperties;

    @InjectMocks
    private ReservationExpirationWorker worker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        BookingProperties.ExpirationWorker workerProps = new BookingProperties.ExpirationWorker();
        workerProps.setBatchSize(10);
        when(bookingProperties.getExpirationWorker()).thenReturn(workerProps);
    }

    @Test
    void processExpiredReservations_withExpired_processesThem() {
        SeatReservation res = new SeatReservation();
        res.setId(1L);
        res.setStatus(ReservationStatus.HELD);
        
        when(seatReservationRepository.findExpiredReservations(eq(ReservationStatus.HELD), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(res)));

        worker.processExpiredReservations();

        verify(expirationProcessor, times(1)).processReservationExpiration(res);
    }

    @Test
    void processExpiredReservations_empty_doesNothing() {
        when(seatReservationRepository.findExpiredReservations(eq(ReservationStatus.HELD), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        worker.processExpiredReservations();

        verify(expirationProcessor, never()).processReservationExpiration(any());
    }

    @Test
    void processExpiredReservations_exception_stopsBatchAndLogs() {
        SeatReservation res1 = new SeatReservation();
        res1.setId(1L);
        SeatReservation res2 = new SeatReservation();
        res2.setId(2L);
        
        when(seatReservationRepository.findExpiredReservations(eq(ReservationStatus.HELD), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(res1, res2)));

        doThrow(new RuntimeException("Redis error")).when(expirationProcessor).processReservationExpiration(res1);

        worker.processExpiredReservations();

        verify(expirationProcessor, times(1)).processReservationExpiration(res1);
        verify(expirationProcessor, never()).processReservationExpiration(res2);
    }
}
