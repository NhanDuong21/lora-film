package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.BookingOperationsSummaryResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingAttentionFilter;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingSpecification;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.response.PagedResponse;
import com.lorafilm.booking.security.service.ManagerCinemaScopeService;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagerBookingService {
    private final AdminBookingService adminBookingService;
    private final BookingRepository bookingRepository;
    private final BookingLifecycleService lifecycleService;
    private final BookingMapper bookingMapper;
    private final ManagerCinemaScopeService cinemaScope;

    public ManagerBookingService(
            AdminBookingService adminBookingService,
            BookingRepository bookingRepository,
            BookingLifecycleService lifecycleService,
            BookingMapper bookingMapper,
            ManagerCinemaScopeService cinemaScope) {
        this.adminBookingService = adminBookingService;
        this.bookingRepository = bookingRepository;
        this.lifecycleService = lifecycleService;
        this.bookingMapper = bookingMapper;
        this.cinemaScope = cinemaScope;
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingAdminResponse> search(
            String cinemaPublicId, BookingFilterRequest filter) {
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        BookingFilterRequest actual = filter == null ? new BookingFilterRequest() : filter;
        actual.setCinemaPublicId(cinema);
        return adminBookingService.findBookings(actual);
    }

    @Transactional(readOnly = true)
    public BookingOperationsSummaryResponse summary(String cinemaPublicId) {
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        Instant now = Instant.now();
        return new BookingOperationsSummaryResponse(
                count(cinema, null, null, now),
                count(cinema, BookingStatus.PENDING_PAYMENT, null, now),
                count(cinema, BookingStatus.CONFIRMED, null, now),
                count(cinema, BookingStatus.COMPLETED, null, now),
                count(cinema, BookingStatus.CANCELLED, null, now),
                count(cinema, BookingStatus.EXPIRED, null, now),
                count(cinema, BookingStatus.REFUNDED, null, now),
                count(cinema, null, BookingAttentionFilter.EXPIRING_SOON, now),
                count(cinema, null, BookingAttentionFilter.OVERDUE, now),
                count(cinema, null, BookingAttentionFilter.PAYMENT_FAILED, now),
                count(cinema, null, BookingAttentionFilter.NEEDS_ATTENTION, now));
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse detail(String cinemaPublicId, String bookingPublicId) {
        Booking booking = scopedBooking(cinemaPublicId, bookingPublicId);
        return adminBookingService.getBookingDetail(booking.getPublicId());
    }

    @Transactional
    public BookingAdminResponse cancelHold(
            String cinemaPublicId, String bookingPublicId, String reason) {
        Booking booking = scopedBooking(cinemaPublicId, bookingPublicId);
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException(
                    "MANAGER_CAN_ONLY_CANCEL_PENDING_HOLD",
                    "Manager chỉ được hủy đơn đang giữ ghế và chưa thanh toán.",
                    HttpStatus.CONFLICT);
        }
        Booking saved = lifecycleService.cancel(
                booking, "MANAGER_CANCELLED_HOLD", reason, "MANAGER");
        return bookingMapper.toAdminResponse(saved);
    }

    private long count(
            String cinema, BookingStatus status, BookingAttentionFilter attention, Instant now) {
        var specification = BookingSpecification.isNotDeleted()
                .and(BookingSpecification.hasCinemaPublicId(cinema));
        if (status != null) specification = specification.and(BookingSpecification.hasStatus(status));
        if (attention != null) specification = specification.and(BookingSpecification.attention(attention, now));
        return bookingRepository.count(specification);
    }

    private Booking scopedBooking(String cinemaPublicId, String bookingPublicId) {
        String cinema = cinemaScope.requireAssigned(cinemaPublicId);
        Booking booking = bookingRepository.findByPublicId(bookingPublicId)
                .orElseThrow(() -> new BusinessException(
                        "BOOKING_NOT_FOUND", "Không tìm thấy đơn đặt vé.", HttpStatus.NOT_FOUND));
        if (!cinema.equals(booking.getCinemaPublicId())) {
            throw new BusinessException(
                    "MANAGER_BOOKING_SCOPE_DENIED",
                    "Đơn đặt vé không thuộc rạp bạn đang phụ trách.",
                    HttpStatus.FORBIDDEN);
        }
        return booking;
    }
}
