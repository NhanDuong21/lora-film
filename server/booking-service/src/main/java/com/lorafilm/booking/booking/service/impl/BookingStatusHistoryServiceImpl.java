package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.booking.dto.BookingStatusHistoryDto;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingStatusHistory;
import com.lorafilm.booking.booking.mapper.BookingStatusHistoryMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingStatusHistoryRepository;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingStatusHistoryServiceImpl implements BookingStatusHistoryService {

    private final BookingStatusHistoryRepository historyRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryMapper historyMapper;

    public BookingStatusHistoryServiceImpl(BookingStatusHistoryRepository historyRepository,
                                           BookingRepository bookingRepository,
                                           BookingStatusHistoryMapper historyMapper) {
        this.historyRepository = historyRepository;
        this.bookingRepository = bookingRepository;
        this.historyMapper = historyMapper;
    }

    @Override
    @Transactional
    public BookingStatusHistoryDto saveHistory(Booking booking, String fromStatus, String toStatus, String reason, String source, String changedBy) {
        if (booking == null || booking.getId() == null) {
            throw new BusinessException("INVALID_BOOKING_DATA", "Booking and Booking ID must not be null");
        }
        if (toStatus == null || toStatus.trim().isEmpty()) {
            throw new BusinessException("INVALID_STATUS_DATA", "Target status (toStatus) cannot be empty");
        }

        BookingStatusHistory history = new BookingStatusHistory();
        history.setBooking(booking);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setReason(reason);
        history.setSource(source != null ? source : "SYSTEM");
        history.setChangedBy(changedBy != null ? changedBy : "SYSTEM");

        BookingStatusHistory saved = historyRepository.save(history);
        return historyMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingStatusHistoryDto> findHistory(Pageable pageable) {
        return historyRepository.findAll(pageable).map(historyMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingStatusHistoryDto> findByBooking(Long bookingId) {
        if (bookingId == null) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking ID cannot be null");
        }
        if (!bookingRepository.existsById(bookingId)) {
            throw new BookingNotFoundException(bookingId);
        }

        List<BookingStatusHistory> histories = historyRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
        return histories.stream().map(historyMapper::toDto).collect(Collectors.toList());
    }
}
