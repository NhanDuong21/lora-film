package com.project.bookingservice.worker;

import com.project.bookingservice.config.BookingProperties;
import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.repository.BookingRepository;
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

class BookingExpirationWorkerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ExpirationProcessor expirationProcessor;

    @Mock
    private BookingProperties bookingProperties;

    @InjectMocks
    private BookingExpirationWorker worker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        BookingProperties.ExpirationWorker workerProps = new BookingProperties.ExpirationWorker();
        workerProps.setBatchSize(10);
        when(bookingProperties.getExpirationWorker()).thenReturn(workerProps);
    }

    @Test
    void processExpiredBookings_withExpired_processesThem() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        
        when(bookingRepository.findExpiredBookings(eq(BookingStatus.PENDING_PAYMENT), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        worker.processExpiredBookings();

        verify(expirationProcessor, times(1)).processBookingExpiration(booking);
    }

    @Test
    void processExpiredBookings_empty_doesNothing() {
        when(bookingRepository.findExpiredBookings(eq(BookingStatus.PENDING_PAYMENT), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        worker.processExpiredBookings();

        verify(expirationProcessor, never()).processBookingExpiration(any());
    }
}
