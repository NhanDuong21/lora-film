package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.booking.client.ShowtimeBookingContext;
import com.lorafilm.booking.booking.client.ShowtimeClient;
import com.lorafilm.booking.booking.dto.request.CancelBookingRequest;
import com.lorafilm.booking.booking.dto.request.CreateBookingRequest;
import com.lorafilm.booking.booking.dto.response.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.dto.response.BookingSummaryResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingSpecification;
import com.lorafilm.booking.booking.service.BookingService;
import com.lorafilm.booking.common.constant.BookingConstants;
import com.lorafilm.booking.food.service.FoodOrderService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.ForbiddenException;
import com.lorafilm.booking.common.exception.IntegrationException;
import com.lorafilm.booking.common.exception.UnauthorizedException;
import com.lorafilm.booking.common.util.BookingCodeGenerator;
import com.lorafilm.booking.reservation.dto.ConvertReservationRequest;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import com.lorafilm.booking.security.service.SecurityContextService;
import com.lorafilm.booking.booking.dto.CreateSnapshotRequest;
import com.lorafilm.booking.booking.dto.CreateTicketRequest;
import com.lorafilm.booking.booking.service.BookingSnapshotService;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final String OPEN_FOR_BOOKING = "OPEN_FOR_BOOKING";
    private static final int BOOKING_CODE_GENERATION_ATTEMPTS = 3;

    private final BookingRepository bookingRepository;
    private final SeatReservationRepository reservationRepository;
    private final SeatReservationService reservationService;
    private final ShowtimeClient showtimeClient;
    private final SecurityContextService securityContextService;
    private final BookingCodeGenerator bookingCodeGenerator;
    private final BookingMapper bookingMapper;
    private final FoodOrderService foodOrderService;
    private final BookingMetricsManager bookingMetricsManager;
    private final BookingTicketService bookingTicketService;
    private final BookingSnapshotService bookingSnapshotService;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            SeatReservationRepository reservationRepository,
            SeatReservationService reservationService,
            ShowtimeClient showtimeClient,
            SecurityContextService securityContextService,
            BookingCodeGenerator bookingCodeGenerator,
            BookingMapper bookingMapper,
            FoodOrderService foodOrderService,
            BookingMetricsManager bookingMetricsManager,
            BookingTicketService bookingTicketService,
            BookingSnapshotService bookingSnapshotService) {
        this.bookingRepository = bookingRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.showtimeClient = showtimeClient;
        this.securityContextService = securityContextService;
        this.bookingCodeGenerator = bookingCodeGenerator;
        this.bookingMapper = bookingMapper;
        this.foodOrderService = foodOrderService;
        this.bookingMetricsManager = bookingMetricsManager;
        this.bookingTicketService = bookingTicketService;
        this.bookingSnapshotService = bookingSnapshotService;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        Long currentUserId = requireAuthenticatedUser();
        MDC.put("userId", currentUserId.toString());
        MDC.put("action", "CREATE_BOOKING");
        ValidatedCreateRequest validatedRequest = validateCreateRequest(request);

        List<SeatReservation> reservations =
                reservationRepository.findAllByPublicIdInForUpdate(validatedRequest.reservationPublicIds());
        Long showtimeId = validateReservations(
                reservations, validatedRequest.reservationPublicIds().size(), currentUserId);

        List<Long> seatIds = reservations.stream().map(SeatReservation::getSeatId).toList();
        ShowtimeBookingContext context = showtimeClient.getBookingContext(showtimeId, seatIds);
        validateShowtimeContext(context, showtimeId, validatedRequest.showtimePublicId(), seatIds);

        Booking booking = Booking.create(
                UUID.randomUUID().toString(),
                generateUniqueBookingCode(),
                currentUserId,
                context.showtimeId(),
                context.movieId(),
                context.cinemaId(),
                context.auditoriumId(),
                context.ticketAmount(),
                BigDecimal.ZERO,
                context.serviceFee(),
                BigDecimal.ZERO,
                context.discountAmount(),
                BigDecimal.ZERO,
                context.currency(),
                context.paymentExpiresAt(),
                null);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);
        MDC.put("bookingId", savedBooking.getPublicId());
        
        List<Long> reservationIds = reservations.stream().map(SeatReservation::getId).toList();
        reservationService.convertReservations(new ConvertReservationRequest(savedBooking.getId(), reservationIds));
        
        // Create Snapshot
        CreateSnapshotRequest snapshotRequest = new CreateSnapshotRequest();
        snapshotRequest.setMovieId(context.movieId());
        snapshotRequest.setMovieTitle(context.movieTitle());
        snapshotRequest.setShowtimeId(context.showtimeId());
        snapshotRequest.setShowtimeStart(context.startsAt());
        snapshotRequest.setShowtimeEnd(context.endsAt());
        snapshotRequest.setCinemaId(context.cinemaId());
        snapshotRequest.setCinemaName(context.cinemaName());
        snapshotRequest.setAuditoriumId(context.auditoriumId());
        snapshotRequest.setAuditoriumName(context.auditoriumName());
        snapshotRequest.setSeatCount(context.seats().size());
        bookingSnapshotService.createSnapshot(savedBooking.getId(), snapshotRequest);

        // Create Tickets
        List<CreateTicketRequest> ticketRequests = context.seats().stream().map(seat -> {
            CreateTicketRequest req = new CreateTicketRequest();
            req.setSeatId(seat.seatId());
            req.setSeatLabel(seat.seatLabel());
            req.setSeatType(seat.seatType());
            req.setTicketPrice(seat.price());
            req.setMovieTitle(context.movieTitle());
            req.setCinemaName(context.cinemaName());
            req.setAuditoriumName(context.auditoriumName());
            req.setShowtimeStart(context.startsAt());
            req.setShowtimeEnd(context.endsAt());
            return req;
        }).toList();
        bookingTicketService.createTickets(savedBooking.getId(), ticketRequests);

        bookingMetricsManager.incrementBookingCreated();
        
        return bookingMapper.toResponse(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(String publicId, CancelBookingRequest request) {
        Long currentUserId = requireAuthenticatedUser();
        MDC.put("bookingId", publicId);
        MDC.put("action", "CANCEL_BOOKING");
        Booking booking = getBooking(publicId);
        requireOwnerOrAdmin(booking, currentUserId);

        String reasonCode = request == null || request.getReasonCode() == null || request.getReasonCode().isBlank()
                ? "USER_CANCEL"
                : request.getReasonCode().trim();
        String reasonDetail = request == null ? null : request.getReasonDetail();
        booking.cancel(reasonCode, reasonDetail, Instant.now());
        Booking saved = bookingRepository.save(booking);
        reservationService.handleBookingStatusChange(saved.getId(), BookingStatus.CANCELLED, reasonDetail != null ? reasonDetail : reasonCode);
        
        bookingMetricsManager.incrementBookingCancelled();
        
        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(String publicId) {
        return changeStatusInternal(publicId, BookingStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public BookingResponse expireBooking(String publicId) {
        return changeStatusInternal(publicId, BookingStatus.EXPIRED);
    }

    @Override
    @Transactional
    public BookingResponse refundBooking(String publicId) {
        return changeStatusInternal(publicId, BookingStatus.REFUNDED);
    }

    @Override
    public BookingDetailResponse findById(String publicId) {
        Long currentUserId = requireAuthenticatedUser();
        MDC.put("bookingId", publicId);
        Booking booking = getBooking(publicId);
        requireOwnerOrAdmin(booking, currentUserId);
        return bookingMapper.toDetailResponse(booking);
    }

    @Override
    public BookingDetailResponse findByCode(String bookingCode) {
        Long currentUserId = requireAuthenticatedUser();
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .filter(this::isActive)
                .orElseThrow(() -> new BookingNotFoundException(bookingCode));
        MDC.put("bookingId", booking.getPublicId());
        requireOwnerOrAdmin(booking, currentUserId);
        return bookingMapper.toDetailResponse(booking);
    }

    @Override
    public Page<BookingSummaryResponse> findAll(
            BookingStatus status, Instant fromDate, Instant toDate, Pageable pageable) {
        if (!securityContextService.isAdmin()) {
            throw new ForbiddenException("Only administrators can list all bookings");
        }
        validateDateRange(fromDate, toDate);
        return bookingRepository.findAll(buildSpecification(null, status, fromDate, toDate), pageable)
                .map(bookingMapper::toSummaryResponse);
    }

    @Override
    public Page<BookingSummaryResponse> findByUser(
            Long userId, BookingStatus status, Instant fromDate, Instant toDate, Pageable pageable) {
        Long currentUserId = requireAuthenticatedUser();
        if (!Objects.equals(currentUserId, userId) && !securityContextService.isAdmin()) {
            throw new ForbiddenException("You cannot access another user's bookings");
        }
        validateDateRange(fromDate, toDate);
        return bookingRepository.findAll(buildSpecification(userId, status, fromDate, toDate), pageable)
                .map(bookingMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public BookingResponse changeStatus(String publicId, BookingStatus targetStatus) {
        if (!securityContextService.isAdmin()) {
            throw new ForbiddenException("Only administrators can change booking status directly");
        }
        return changeStatusInternal(publicId, targetStatus);
    }

    private BookingResponse changeStatusInternal(String publicId, BookingStatus targetStatus) {
        MDC.put("bookingId", publicId);
        MDC.put("action", "CHANGE_BOOKING_STATUS");
        Booking booking = getBooking(publicId);
        booking.changeStatus(targetStatus, Instant.now());
        Booking saved = bookingRepository.save(booking);
        
        // Sync status with Food Order
        foodOrderService.updateOrderStatusBasedOnBooking(saved.getId(), targetStatus);
        
        if (targetStatus == BookingStatus.CANCELLED || targetStatus == BookingStatus.EXPIRED || targetStatus == BookingStatus.REFUNDED) {
            reservationService.handleBookingStatusChange(saved.getId(), targetStatus, "Booking status changed to " + targetStatus);
            // Cancel tickets
            try {
                bookingTicketService.deleteTickets(saved.getId());
            } catch (Exception e) {
                log.warn("Failed to delete/cancel tickets for bookingId: {}", saved.getId(), e);
            }
        }

        // Increment Metrics
        if (targetStatus == BookingStatus.CONFIRMED) {
            bookingMetricsManager.incrementBookingConfirmed();
            bookingMetricsManager.incrementPaymentSuccess();
        } else if (targetStatus == BookingStatus.EXPIRED) {
            bookingMetricsManager.incrementBookingExpired();
            bookingMetricsManager.incrementPaymentFailed();
        } else if (targetStatus == BookingStatus.CANCELLED) {
            bookingMetricsManager.incrementBookingCancelled();
        }

        return bookingMapper.toResponse(saved);
    }

    private ValidatedCreateRequest validateCreateRequest(CreateBookingRequest request) {
        if (request == null) {
            throw new BusinessException("BOOKING_INVALID_REQUEST", "Create booking request is required");
        }
        String showtimePublicId = normalizeShowtimePublicId(request.getShowtimePublicId());
        List<String> reservationPublicIds = request.getReservationPublicIds();
        if (reservationPublicIds == null || reservationPublicIds.isEmpty()) {
            throw new BusinessException("BOOKING_RESERVATION_REQUIRED", "At least one reservation is required");
        }
        if (reservationPublicIds.size() > BookingConstants.DEFAULT_MAX_TICKETS_PER_BOOKING) {
            throw new BusinessException("BOOKING_TOO_MANY_RESERVATIONS", "A booking cannot contain more than 8 reservations");
        }

        List<String> normalizedPublicIds;
        try {
            normalizedPublicIds = reservationPublicIds.stream()
                    .map(this::normalizeReservationPublicId)
                    .toList();
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException(
                    "BOOKING_RESERVATION_PUBLIC_ID_INVALID",
                    "Reservation public IDs must be valid UUIDs");
        }

        Set<String> uniquePublicIds = new HashSet<>(normalizedPublicIds);
        if (uniquePublicIds.size() != normalizedPublicIds.size()) {
            throw new BusinessException(
                    "BOOKING_DUPLICATE_RESERVATION",
                    "Duplicate reservation public IDs are not allowed");
        }
        return new ValidatedCreateRequest(showtimePublicId, normalizedPublicIds);
    }

    private String normalizeShowtimePublicId(String publicId) {
        try {
            return normalizeUuid(publicId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException(
                    "BOOKING_SHOWTIME_PUBLIC_ID_INVALID",
                    "showtimePublicId must be a valid UUID");
        }
    }

    private String normalizeReservationPublicId(String publicId) {
        return normalizeUuid(publicId);
    }

    private String normalizeUuid(String publicId) {
        UUID parsedPublicId = UUID.fromString(publicId);
        if (!parsedPublicId.toString().equalsIgnoreCase(publicId)) {
            throw new IllegalArgumentException("Non-canonical UUID");
        }
        return parsedPublicId.toString();
    }

    private Long validateReservations(
            List<SeatReservation> reservations,
            int requestedReservationCount,
            Long currentUserId) {
        if (reservations.size() != requestedReservationCount) {
            throw new BusinessException(
                    "BOOKING_RESERVATION_NOT_FOUND",
                    "One or more reservations do not exist",
                    HttpStatus.NOT_FOUND);
        }

        Long showtimeId = reservations.getFirst().getShowtimeId();
        if (showtimeId == null || showtimeId <= 0) {
            throw new BusinessException(
                    "BOOKING_RESERVATION_SHOWTIME_INVALID",
                    "Reservation does not contain a valid showtime");
        }

        Instant now = Instant.now();
        for (SeatReservation reservation : reservations) {
            if (!Objects.equals(currentUserId, reservation.getUserId())) {
                throw new BusinessException(
                        "BOOKING_RESERVATION_OWNER_MISMATCH",
                        "One or more reservations do not belong to the current user",
                        HttpStatus.FORBIDDEN);
            }
            if (!Objects.equals(showtimeId, reservation.getShowtimeId())) {
                throw new BusinessException(
                        "BOOKING_RESERVATION_SHOWTIME_MISMATCH",
                        "All reservations must belong to the requested showtime");
            }
            if (reservation.getStatus() != SeatReservationStatus.HELD) {
                throw new BusinessException(
                        "BOOKING_RESERVATION_INVALID",
                        "All reservations must be in HELD status",
                        HttpStatus.CONFLICT);
            }
            if (reservation.getExpiresAt() == null || !reservation.getExpiresAt().isAfter(now)) {
                throw new BusinessException(
                        "BOOKING_RESERVATION_EXPIRED",
                        "One or more reservations have expired",
                        HttpStatus.CONFLICT);
            }
        }
        return showtimeId;
    }

    private void validateShowtimeContext(
            ShowtimeBookingContext context,
            Long requestedShowtimeId,
            String requestedShowtimePublicId,
            List<Long> requestedSeatIds) {
        if (context == null || !Objects.equals(context.showtimeId(), requestedShowtimeId)) {
            throw new IntegrationException("Movie Service returned a mismatched showtime");
        }
        if (!Objects.equals(context.showtimePublicId(), requestedShowtimePublicId)) {
            throw new BusinessException(
                    "BOOKING_RESERVATION_SHOWTIME_MISMATCH",
                    "Reservations do not belong to the requested showtime");
        }
        if (!OPEN_FOR_BOOKING.equals(context.status())) {
            throw new BusinessException("BOOKING_SHOWTIME_NOT_OPEN", "Showtime is not open for booking");
        }
        Instant now = Instant.now();
        if (context.endsAt() == null || !context.endsAt().isAfter(now)) {
            throw new BusinessException("BOOKING_SHOWTIME_ENDED", "Showtime has already ended");
        }
        if (context.paymentExpiresAt() == null || !context.paymentExpiresAt().isAfter(now)) {
            throw new IntegrationException("Movie Service returned an invalid payment deadline");
        }
        if (context.seats() == null) {
            throw new IntegrationException("Movie Service returned incomplete seat information");
        }
        Set<Long> returnedSeatIds = context.seats().stream()
                .map(ShowtimeBookingContext.SeatContext::seatId)
                .collect(java.util.stream.Collectors.toSet());
        if (returnedSeatIds.size() != requestedSeatIds.size()
                || !returnedSeatIds.containsAll(requestedSeatIds)) {
            throw new IntegrationException("Movie Service returned mismatched seat information");
        }
        validatePricing(context);
    }

    private void validatePricing(ShowtimeBookingContext context) {
        if (context.ticketAmount() == null || context.serviceFee() == null
                || context.discountAmount() == null || context.totalAmount() == null
                || context.currency() == null || context.currency().isBlank()) {
            throw new IntegrationException("Movie Service returned incomplete pricing information");
        }
        BigDecimal calculatedTotal = context.ticketAmount()
                .add(context.serviceFee())
                .subtract(context.discountAmount());
        if (calculatedTotal.signum() < 0 || calculatedTotal.compareTo(context.totalAmount()) != 0) {
            throw new IntegrationException("Movie Service returned inconsistent pricing information");
        }
    }

    private String generateUniqueBookingCode() {
        for (int attempt = 0; attempt < BOOKING_CODE_GENERATION_ATTEMPTS; attempt++) {
            String bookingCode = bookingCodeGenerator.generate();
            if (!bookingRepository.existsByBookingCode(bookingCode)) {
                return bookingCode;
            }
        }
        throw new BusinessException(
                "BOOKING_CODE_GENERATION_FAILED",
                "Could not generate a unique booking code",
                HttpStatus.CONFLICT);
    }

    private Booking getBooking(String publicId) {
        UUID normalizedPublicId = normalizePublicId(publicId);
        return bookingRepository.findByPublicId(normalizedPublicId.toString())
                .filter(this::isActive)
                .orElseThrow(() -> new BookingNotFoundException(normalizedPublicId));
    }

    private UUID normalizePublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new BusinessException("BOOKING_PUBLIC_ID_INVALID", "publicId is required");
        }
        try {
            UUID parsedPublicId = UUID.fromString(publicId);
            if (!parsedPublicId.toString().equalsIgnoreCase(publicId)) {
                throw new IllegalArgumentException("Non-canonical UUID");
            }
            return parsedPublicId;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("BOOKING_PUBLIC_ID_INVALID", "publicId must be a valid UUID");
        }
    }

    private boolean isActive(Booking booking) {
        return !Boolean.TRUE.equals(booking.getIsDeleted());
    }

    private Long requireAuthenticatedUser() {
        Long userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("User must be authenticated");
        }
        return userId;
    }

    private void requireOwnerOrAdmin(Booking booking, Long currentUserId) {
        if (!Objects.equals(booking.getUserId(), currentUserId) && !securityContextService.isAdmin()) {
            throw new ForbiddenException("You do not own this booking");
        }
    }

    private void validateDateRange(Instant fromDate, Instant toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("BOOKING_INVALID_DATE_RANGE", "fromDate must not be after toDate");
        }
    }

    private Specification<Booking> buildSpecification(
            Long userId, BookingStatus status, Instant fromDate, Instant toDate) {
        return Specification.where(BookingSpecification.isNotDeleted())
                .and(BookingSpecification.hasUserId(userId))
                .and(BookingSpecification.hasStatus(status))
                .and(BookingSpecification.createdFrom(fromDate))
                .and(BookingSpecification.createdTo(toDate));
    }

    private record ValidatedCreateRequest(String showtimePublicId, List<String> reservationPublicIds) {
    }
}
