package com.lorafilm.booking.service;

import com.lorafilm.booking.booking.dto.BookingSnapshotDto;
import com.lorafilm.booking.booking.dto.CreateSnapshotRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingSnapshot;
import com.lorafilm.booking.booking.mapper.BookingSnapshotMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingSnapshotRepository;
import com.lorafilm.booking.booking.service.impl.BookingSnapshotServiceImpl;
import com.lorafilm.booking.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookingSnapshotServiceTest {

    @Mock
    private BookingSnapshotRepository bookingSnapshotRepository;

    @Mock
    private BookingRepository bookingRepository;

    private BookingSnapshotMapper bookingSnapshotMapper = new BookingSnapshotMapper();

    private BookingSnapshotServiceImpl bookingSnapshotService;

    private Booking sampleBooking;
    private CreateSnapshotRequest createRequest;
    private BookingSnapshot sampleSnapshot;

    @BeforeEach
    public void setUp() {
        bookingSnapshotService = new BookingSnapshotServiceImpl(bookingSnapshotRepository, bookingRepository, bookingSnapshotMapper);

        sampleBooking = new Booking();
        sampleBooking.setId(10L);

        createRequest = new CreateSnapshotRequest();
        createRequest.setMovieTitle("Avatar 2");
        createRequest.setCinemaName("CGV Vincom");

        sampleSnapshot = new BookingSnapshot();
        sampleSnapshot.setId(100L);
        sampleSnapshot.setBooking(sampleBooking);
        sampleSnapshot.setMovieTitle("Avatar 2");
        sampleSnapshot.setSnapshotJson("""
                [{
                  "seatId": 101,
                  "seatPublicId": "seat-public-101",
                  "seatLabel": "D6",
                  "seatType": "VIP",
                  "price": 130000,
                  "currency": "VND"
                }]
                """);
    }

    @Test
    public void createSnapshot_Success() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));
        when(bookingSnapshotRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(bookingSnapshotRepository.save(any())).thenReturn(sampleSnapshot);

        BookingSnapshotDto result = bookingSnapshotService.createSnapshot(10L, createRequest);

        assertNotNull(result);
        assertEquals("Avatar 2", result.getMovieTitle());
        assertEquals(1, result.getSeats().size());
        assertEquals("D6", result.getSeats().get(0).seatLabel());
        verify(bookingSnapshotRepository).save(any());
    }

    @Test
    public void createSnapshot_AlreadyExists_ThrowsException() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));
        when(bookingSnapshotRepository.findByBookingId(10L)).thenReturn(Optional.of(sampleSnapshot));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                bookingSnapshotService.createSnapshot(10L, createRequest));
        assertEquals("SNAPSHOT_ALREADY_EXISTS", ex.getErrorCode());
    }

    @Test
    public void findByBooking_Success() {
        when(bookingRepository.existsById(10L)).thenReturn(true);
        when(bookingSnapshotRepository.findByBookingId(10L)).thenReturn(Optional.of(sampleSnapshot));

        BookingSnapshotDto result = bookingSnapshotService.findByBooking(10L);

        assertNotNull(result);
        assertEquals("Avatar 2", result.getMovieTitle());
    }

    @Test
    public void findByBooking_NoSnapshot_ReturnsNull() {
        when(bookingRepository.existsById(10L)).thenReturn(true);
        when(bookingSnapshotRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        BookingSnapshotDto result = bookingSnapshotService.findByBooking(10L);

        org.junit.jupiter.api.Assertions.assertNull(result);
    }
}
