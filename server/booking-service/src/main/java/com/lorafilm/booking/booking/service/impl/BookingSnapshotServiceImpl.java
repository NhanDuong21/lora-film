package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.booking.dto.BookingSnapshotDto;
import com.lorafilm.booking.booking.dto.CreateSnapshotRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingSnapshot;
import com.lorafilm.booking.booking.mapper.BookingSnapshotMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingSnapshotRepository;
import com.lorafilm.booking.booking.service.BookingSnapshotService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BookingSnapshotServiceImpl implements BookingSnapshotService {

    private final BookingSnapshotRepository bookingSnapshotRepository;
    private final BookingRepository bookingRepository;
    private final BookingSnapshotMapper bookingSnapshotMapper;

    public BookingSnapshotServiceImpl(BookingSnapshotRepository bookingSnapshotRepository,
                                      BookingRepository bookingRepository,
                                      BookingSnapshotMapper bookingSnapshotMapper) {
        this.bookingSnapshotRepository = bookingSnapshotRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSnapshotMapper = bookingSnapshotMapper;
    }

    @Override
    @Transactional
    public BookingSnapshotDto createSnapshot(Long bookingId, CreateSnapshotRequest request) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        if (request == null) {
            throw new BusinessException("INVALID_SNAPSHOT_DATA", "Snapshot data cannot be null");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        Optional<BookingSnapshot> existingSnapshot = bookingSnapshotRepository.findByBookingId(bookingId);
        if (existingSnapshot.isPresent()) {
            throw new BusinessException("SNAPSHOT_ALREADY_EXISTS", "Snapshot already exists and cannot be updated");
        }

        BookingSnapshot snapshot = bookingSnapshotMapper.toEntity(request);
        snapshot.setBooking(booking);

        BookingSnapshot savedSnapshot = bookingSnapshotRepository.save(snapshot);
        return bookingSnapshotMapper.toDto(savedSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSnapshotDto findByBooking(Long bookingId) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        if (!bookingRepository.existsById(bookingId)) {
            throw new BookingNotFoundException(bookingId);
        }

        BookingSnapshot snapshot = bookingSnapshotRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException("NO_SNAPSHOT_FOUND", "Booking has no snapshot"));

        return bookingSnapshotMapper.toDto(snapshot);
    }
}
