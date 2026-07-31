package com.lorafilm.booking.booking.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.client.ShowtimeBookingContext;
import com.lorafilm.booking.booking.client.ShowtimeClient;
import com.lorafilm.booking.booking.client.ScoreRedemptionClient;
import com.lorafilm.booking.booking.dto.request.CancelBookingRequest;
import com.lorafilm.booking.booking.dto.request.CreateBookingRequest;
import com.lorafilm.booking.booking.dto.request.FinalizeCheckoutRequest;
import com.lorafilm.booking.booking.dto.BookingPriceSnapshotPayload;
import com.lorafilm.booking.booking.dto.response.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.response.BookingFoodResponse;
import com.lorafilm.booking.booking.dto.response.BookingPresentationResponse;
import com.lorafilm.booking.booking.dto.response.BookingResponse;
import com.lorafilm.booking.booking.dto.response.BookingSpendingSummaryResponse;
import com.lorafilm.booking.booking.dto.response.BookingSummaryResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.policy.SingleSeatGapPolicy;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingSpecification;
import com.lorafilm.booking.booking.service.BookingService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.ForbiddenException;
import com.lorafilm.booking.common.exception.IntegrationException;
import com.lorafilm.booking.common.exception.UnauthorizedException;
import com.lorafilm.booking.common.util.BookingCodeGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lorafilm.booking.booking.repository.BookingSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingTicketRepository;
import com.lorafilm.booking.booking.entity.BookingSnapshot;
import com.lorafilm.booking.booking.dto.BookingTicketDto;
import com.lorafilm.booking.reservation.dto.ConvertReservationRequest;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import com.lorafilm.booking.reservation.service.RedisLockService;
import com.lorafilm.booking.config.BookingPolicyProperties;
import com.lorafilm.booking.security.service.SecurityContextService;
import com.lorafilm.booking.booking.dto.CreateSnapshotRequest;
import com.lorafilm.booking.booking.dto.CreateTicketRequest;
import com.lorafilm.booking.booking.service.BookingSnapshotService;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.booking.service.BookingLifecycleService;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.realtime.SeatAvailabilityEventService;
import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final BookingPriceSnapshotRepository priceSnapshotRepository;
    private final ObjectMapper objectMapper;
    private final BookingSnapshotRepository bookingSnapshotRepository;
    private final BookingTicketRepository bookingTicketRepository;
    private final com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService;
    private final BookingMetricsManager bookingMetricsManager;
    private final BookingTicketService bookingTicketService;
    private final BookingSnapshotService bookingSnapshotService;
    private final BookingPolicyProperties bookingPolicyProperties;
    private final RedisLockService redisLockService;
    private final BookingLifecycleService lifecycleService;
    private SeatAvailabilityEventService seatAvailabilityEventService;
    private ScoreRedemptionClient scoreRedemptionClient;

    @Autowired
    public BookingServiceImpl(
            BookingRepository bookingRepository,
            SeatReservationRepository reservationRepository,
            SeatReservationService reservationService,
            ShowtimeClient showtimeClient,
            SecurityContextService securityContextService,
            BookingCodeGenerator bookingCodeGenerator,
            BookingMapper bookingMapper,
            BookingPriceSnapshotRepository priceSnapshotRepository,
            ObjectMapper objectMapper,
            BookingSnapshotRepository bookingSnapshotRepository,
            BookingTicketRepository bookingTicketRepository,
            com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService,
            BookingMetricsManager bookingMetricsManager,
            BookingTicketService bookingTicketService,
            BookingSnapshotService bookingSnapshotService,
            BookingPolicyProperties bookingPolicyProperties,
            RedisLockService redisLockService,
            BookingLifecycleService lifecycleService) {
        this.bookingRepository = bookingRepository;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.showtimeClient = showtimeClient;
        this.securityContextService = securityContextService;
        this.bookingCodeGenerator = bookingCodeGenerator;
        this.bookingMapper = bookingMapper;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.objectMapper = objectMapper;
        this.bookingSnapshotRepository = bookingSnapshotRepository;
        this.bookingTicketRepository = bookingTicketRepository;
        this.outboxService = outboxService;
        this.bookingMetricsManager = bookingMetricsManager;
        this.bookingTicketService = bookingTicketService;
        this.bookingSnapshotService = bookingSnapshotService;
        this.bookingPolicyProperties = bookingPolicyProperties;
        this.redisLockService = redisLockService;
        this.lifecycleService = lifecycleService;
    }

    /**
     * Optional setter keeps legacy unit-test constructors source compatible
     * while allowing committed seat changes to be projected in production.
     */
    @Autowired(required = false)
    public void setSeatAvailabilityEventService(SeatAvailabilityEventService service) {
        this.seatAvailabilityEventService = service;
    }

    @Autowired(required = false)
    public void setScoreRedemptionClient(ScoreRedemptionClient service) {
        this.scoreRedemptionClient = service;
    }

    /** Backwards-compatible constructor for existing unit/integration callers. */
    public BookingServiceImpl(
            BookingRepository bookingRepository,
            SeatReservationRepository reservationRepository,
            SeatReservationService reservationService,
            ShowtimeClient showtimeClient,
            SecurityContextService securityContextService,
            BookingCodeGenerator bookingCodeGenerator,
            BookingMapper bookingMapper,
            BookingPriceSnapshotRepository priceSnapshotRepository,
            ObjectMapper objectMapper,
            BookingSnapshotRepository bookingSnapshotRepository,
            BookingTicketRepository bookingTicketRepository,
            com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService,
            BookingMetricsManager bookingMetricsManager,
            BookingTicketService bookingTicketService,
            BookingSnapshotService bookingSnapshotService,
            BookingPolicyProperties bookingPolicyProperties,
            RedisLockService redisLockService) {
        this(bookingRepository, reservationRepository, reservationService, showtimeClient,
                securityContextService, bookingCodeGenerator, bookingMapper, priceSnapshotRepository,
                objectMapper, bookingSnapshotRepository, bookingTicketRepository, outboxService,
                bookingMetricsManager, bookingTicketService, bookingSnapshotService,
                bookingPolicyProperties, redisLockService, null);
    }

    /** Backwards-compatible constructor for existing unit/integration callers. */
    public BookingServiceImpl(
            BookingRepository bookingRepository,
            SeatReservationRepository reservationRepository,
            SeatReservationService reservationService,
            ShowtimeClient showtimeClient,
            SecurityContextService securityContextService,
            BookingCodeGenerator bookingCodeGenerator,
            BookingMapper bookingMapper,
            BookingPriceSnapshotRepository priceSnapshotRepository,
            ObjectMapper objectMapper,
            BookingSnapshotRepository bookingSnapshotRepository,
            BookingTicketRepository bookingTicketRepository,
            com.lorafilm.booking.payment.port.PaymentIntegrationPort ignoredPaymentIntegrationPort,
            com.lorafilm.booking.payment.repository.BookingPaymentEventRepository ignoredPaymentEventRepository,
            com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService,
            BookingMetricsManager bookingMetricsManager,
            BookingTicketService bookingTicketService,
            BookingSnapshotService bookingSnapshotService) {
        this(bookingRepository, reservationRepository, reservationService, showtimeClient,
                securityContextService, bookingCodeGenerator, bookingMapper, priceSnapshotRepository,
                objectMapper, bookingSnapshotRepository, bookingTicketRepository,
                outboxService, bookingMetricsManager, bookingTicketService,
                bookingSnapshotService, new BookingPolicyProperties(), null, null);
    }

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        Long currentUserId = requireAuthenticatedUser();
        MDC.put("userId", currentUserId.toString());
        MDC.put("action", "CREATE_BOOKING");
        ValidatedCreateRequest validatedRequest = validateCreateRequest(request);

        if (validatedRequest.seatPublicIds() != null) {
            return createAtomicBookingFromSeats(currentUserId, validatedRequest);
        }

        List<SeatReservation> reservations = reservationRepository
                .findAllByPublicIdInForUpdate(validatedRequest.reservationPublicIds());
        Long showtimeId = validateReservations(
                reservations, validatedRequest.reservationPublicIds().size(), currentUserId);

        List<Long> seatIds = reservations.stream().map(SeatReservation::getSeatId).toList();
        ShowtimeBookingContext context = showtimeClient.getBookingContext(showtimeId, seatIds);
        validateShowtimeContext(context, showtimeId, validatedRequest.showtimePublicId(), seatIds);
        validateSingleSeatGap(showtimeId, seatIds);

        Instant now = Instant.now();
        enforceSingleActiveBooking(currentUserId, context.showtimeId(), now);
        Instant bookingDeadline = calculateDeadline(now, context.startsAt(),
                reservations.stream().map(SeatReservation::getExpiresAt).filter(Objects::nonNull).min(Instant::compareTo).orElse(null));
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
                bookingDeadline,
                null);
        booking.setShowtimePublicId(context.showtimePublicId());

        Booking savedBooking = saveNewPendingBooking(booking);
        persistAuthoritativePriceSnapshot(savedBooking, context);
        MDC.put("bookingId", savedBooking.getPublicId());
        List<Long> reservationIds = reservations.stream().map(SeatReservation::getId).toList();
        // Compatibility reservations remain HELD until Payment SUCCESS.  Linking
        // them is part of this transaction; Redis is never used as their owner.
        for (SeatReservation reservation : reservations) {
            reservation.setBookingId(savedBooking.getId());
            context.seats().stream()
                    .filter(seat -> Objects.equals(seat.seatId(), reservation.getSeatId()))
                    .findFirst()
                    .ifPresent(seat -> {
                        reservation.setShowtimePublicId(context.showtimePublicId());
                        reservation.setSeatPublicId(seat.seatPublicId());
                    });
            if (reservation.getExpiresAt() != null && reservation.getExpiresAt().isAfter(bookingDeadline)) {
                reservation.setExpiresAt(bookingDeadline);
            }
            reservationRepository.save(reservation);
        }
        publishSeatAvailability(reservations);

        // Create Snapshot
        CreateSnapshotRequest snapshotRequest = new CreateSnapshotRequest();
        snapshotRequest.setMovieId(context.movieId());
        snapshotRequest.setMovieTitle(context.movieTitle());
        snapshotRequest.setMoviePoster(context.moviePosterUrl());
        snapshotRequest.setShowtimeId(context.showtimeId());
        snapshotRequest.setShowtimeStart(context.startsAt());
        snapshotRequest.setShowtimeEnd(context.endsAt());
        snapshotRequest.setCinemaId(context.cinemaId());
        snapshotRequest.setCinemaName(context.cinemaName());
        snapshotRequest.setAuditoriumId(context.auditoriumId());
        snapshotRequest.setAuditoriumName(context.auditoriumName());
        snapshotRequest.setSeatCount(context.seats().size());
        try {
            snapshotRequest.setSnapshotJson(objectMapper.writeValueAsString(context.seats()));
        } catch (Exception e) {
            log.error("Failed to serialize seats to snapshotJson", e);
        }
        bookingSnapshotService.createSnapshot(savedBooking.getId(), snapshotRequest);

        bookingMetricsManager.incrementBookingCreated();

        return bookingMapper.toResponse(savedBooking);
    }

    private BookingResponse createAtomicBookingFromSeats(Long userId, ValidatedCreateRequest request) {
        ShowtimeBookingContext context = showtimeClient.getBookingContextByPublicId(
                request.showtimePublicId(), request.seatPublicIds());
        if (context == null || context.startsAt() == null || !context.startsAt().isAfter(Instant.now())) {
            throw new BusinessException("BOOKING_SHOWTIME_NOT_OPEN", "Showtime has already started", HttpStatus.CONFLICT);
        }
        List<Long> seatIds = context.seats().stream().map(ShowtimeBookingContext.SeatContext::seatId).toList();
        Set<String> requestedPublicIds = new HashSet<>(request.seatPublicIds());
        Set<String> returnedPublicIds = context.seats().stream()
                .map(ShowtimeBookingContext.SeatContext::seatPublicId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (returnedPublicIds.size() != requestedPublicIds.size()
                || !returnedPublicIds.equals(requestedPublicIds)) {
            throw new IntegrationException("Movie Service returned mismatched public seat information");
        }
        validateShowtimeContext(context, context.showtimeId(), request.showtimePublicId(), seatIds);
        validateSingleSeatGap(context.showtimeId(), seatIds);
        if (seatIds.size() > bookingPolicyProperties.getMaxSeatsPerBooking()) {
            throw new BusinessException("BOOKING_TOO_MANY_SEATS",
                    "A booking cannot contain more than " + bookingPolicyProperties.getMaxSeatsPerBooking() + " seats",
                    HttpStatus.BAD_REQUEST);
        }

        String ownerToken = UUID.randomUUID().toString();
        boolean locked = redisLockService == null || redisLockService.acquireHoldLocks(
                context.showtimeId(), seatIds, ownerToken, bookingPolicyProperties.getCreationLockTtlSeconds());
        if (!locked) {
            throw new BusinessException("BOOKING_SEAT_CONFLICT",
                    "One or more seats are currently being booked", HttpStatus.CONFLICT);
        }
        registerRedisCleanup(context.showtimeId(), seatIds, ownerToken);
        try {
            Instant now = Instant.now();
            Instant deadline = calculateDeadline(now, context.startsAt(), null);
            enforceSingleActiveBooking(userId, context.showtimeId(), now);
            expireStaleReservationsBeforeInsert(context.showtimeId(), seatIds, now);
            Booking booking = Booking.create(
                    UUID.randomUUID().toString(), generateUniqueBookingCode(), userId,
                    context.showtimeId(), context.movieId(), context.cinemaId(), context.auditoriumId(),
                    context.ticketAmount(), BigDecimal.ZERO, context.serviceFee(), BigDecimal.ZERO,
                    context.discountAmount(), BigDecimal.ZERO, context.currency(), deadline, null);
            booking.setShowtimePublicId(context.showtimePublicId());
            Booking saved = saveNewPendingBooking(booking);
            persistAuthoritativePriceSnapshot(saved, context);

            List<SeatReservation> reservations = context.seats().stream().map(seat -> {
                SeatReservation reservation = new SeatReservation();
                reservation.setPublicId(UUID.randomUUID().toString());
                reservation.setReservationCode("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                reservation.setUserId(userId);
                reservation.setShowtimeId(context.showtimeId());
                reservation.setShowtimePublicId(context.showtimePublicId());
                reservation.setSeatId(seat.seatId());
                reservation.setSeatPublicId(seat.seatPublicId());
                reservation.setSeatLabel(seat.seatLabel());
                reservation.setSeatType(seat.seatType());
                reservation.setReservationSource(com.lorafilm.booking.reservation.enums.ReservationSource.WEB);
                reservation.setStatus(SeatReservationStatus.HELD);
                reservation.setReservedAt(now);
                reservation.setExpiresAt(deadline);
                reservation.setBookingId(saved.getId());
                return reservation;
            }).toList();
            try {
                reservationRepository.saveAllAndFlush(reservations);
            } catch (DataIntegrityViolationException conflict) {
                throw new BusinessException("BOOKING_SEAT_CONFLICT",
                        "One or more selected seats are no longer available", HttpStatus.CONFLICT);
            }
            publishSeatAvailability(reservations);
            createBookingSnapshot(saved, context);
            bookingMetricsManager.incrementBookingCreated();
            return bookingMapper.toResponse(saved);
        } catch (RuntimeException failure) {
            if (redisLockService != null && !TransactionSynchronizationManager.isSynchronizationActive()) {
                redisLockService.releaseLocks(context.showtimeId(), seatIds, ownerToken);
            }
            throw failure;
        }
    }

    private void enforceSingleActiveBooking(Long userId, Long showtimeId, Instant now) {
        List<Booking> pendingBookings = bookingRepository.findPendingByUserAndShowtimeForUpdate(
                userId, showtimeId, BookingStatus.PENDING_PAYMENT);
        Optional<Booking> activeBooking = pendingBookings.stream()
                .filter(booking -> booking.getExpiresAt() == null
                        || booking.getExpiresAt().isAfter(now))
                .findFirst();
        if (activeBooking.isPresent()) {
            throw new BusinessException(
                    "BOOKING_ACTIVE_SHOWTIME_EXISTS",
                    "Bạn đã có một đơn đang giữ ghế cho suất chiếu này",
                    HttpStatus.CONFLICT);
        }

        for (Booking staleBooking : pendingBookings) {
            if (lifecycleService != null) {
                lifecycleService.transition(
                        staleBooking,
                        BookingStatus.EXPIRED,
                        "Expired before creating a replacement Booking for the same showtime",
                        "BOOKING_CREATION");
            } else {
                staleBooking.changeStatus(BookingStatus.EXPIRED, now);
                bookingRepository.save(staleBooking);
                reservationService.handleBookingStatusChange(
                        staleBooking.getId(),
                        BookingStatus.EXPIRED,
                        "Expired before creating a replacement Booking for the same showtime");
            }
        }
        if (!pendingBookings.isEmpty()) {
            bookingRepository.flush();
        }
    }

    private Booking saveNewPendingBooking(Booking booking) {
        try {
            return bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException conflict) {
            if (isActiveCustomerShowtimeConstraint(conflict)) {
                throw new BusinessException(
                        "BOOKING_ACTIVE_SHOWTIME_EXISTS",
                        "Bạn đã có một đơn đang giữ ghế cho suất chiếu này",
                        HttpStatus.CONFLICT);
            }
            throw conflict;
        }
    }

    private boolean isActiveCustomerShowtimeConstraint(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(java.util.Locale.ROOT);
                if (normalized.contains("uk_active_customer_showtime_booking")
                        || normalized.contains("active_customer_showtime_key")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void expireStaleReservationsBeforeInsert(Long showtimeId, List<Long> seatIds, Instant now) {
        List<SeatReservation> existing = reservationRepository.findReservationsForBookingUpdate(showtimeId, seatIds);
        List<SeatReservation> expiredUnlinked = new ArrayList<>();
        for (SeatReservation reservation : existing) {
            if (reservation.getStatus() == SeatReservationStatus.BOOKED
                    || (reservation.getStatus() == SeatReservationStatus.HELD
                    && reservation.getExpiresAt() != null
                    && reservation.getExpiresAt().isAfter(now))) {
                throw new BusinessException("BOOKING_SEAT_CONFLICT",
                        "One or more selected seats are no longer available", HttpStatus.CONFLICT);
            }
            if (reservation.getStatus() != SeatReservationStatus.HELD) {
                continue;
            }
            if (reservation.getBookingId() != null) {
                bookingRepository.findById(reservation.getBookingId()).ifPresent(staleBooking -> {
                    if (staleBooking.getBookingStatus() == BookingStatus.PENDING_PAYMENT
                            && staleBooking.getExpiresAt() != null
                            && !staleBooking.getExpiresAt().isAfter(now)) {
                        staleBooking.changeStatus(BookingStatus.EXPIRED, now);
                        bookingRepository.save(staleBooking);
                        reservationService.handleBookingStatusChange(
                                staleBooking.getId(), BookingStatus.EXPIRED,
                                "Expired while a new Booking acquired the database seat lock");
                    }
                });
            } else {
                reservation.setStatus(SeatReservationStatus.EXPIRED);
                reservation.setExpiredReason("Expired before Booking creation");
                reservationRepository.save(reservation);
                expiredUnlinked.add(reservation);
            }
        }
        reservationRepository.flush();
        publishSeatAvailability(expiredUnlinked);
    }

    private void publishSeatAvailability(List<SeatReservation> reservations) {
        if (seatAvailabilityEventService != null) {
            seatAvailabilityEventService.publish(reservations);
        }
    }

    private void createBookingSnapshot(Booking booking, ShowtimeBookingContext context) {
        CreateSnapshotRequest snapshotRequest = new CreateSnapshotRequest();
        snapshotRequest.setMovieId(context.movieId());
        snapshotRequest.setMovieTitle(context.movieTitle());
        snapshotRequest.setMoviePoster(context.moviePosterUrl());
        snapshotRequest.setShowtimeId(context.showtimeId());
        snapshotRequest.setShowtimeStart(context.startsAt());
        snapshotRequest.setShowtimeEnd(context.endsAt());
        snapshotRequest.setCinemaId(context.cinemaId());
        snapshotRequest.setCinemaName(context.cinemaName());
        snapshotRequest.setAuditoriumId(context.auditoriumId());
        snapshotRequest.setAuditoriumName(context.auditoriumName());
        snapshotRequest.setSeatCount(context.seats().size());
        try {
            snapshotRequest.setSnapshotJson(objectMapper.writeValueAsString(context.seats()));
        } catch (Exception e) {
            throw new IntegrationException("Failed to serialize seat snapshot", e);
        }
        bookingSnapshotService.createSnapshot(booking.getId(), snapshotRequest);
    }

    private Instant calculateDeadline(Instant now, Instant showtimeStart, Instant existingDeadline) {
        if (showtimeStart == null || !showtimeStart.isAfter(now)) {
            throw new BusinessException("BOOKING_SHOWTIME_STARTED", "Showtime start must be in the future", HttpStatus.CONFLICT);
        }
        Instant configured = now.plusSeconds(bookingPolicyProperties.getHoldDurationSeconds());
        Instant deadline = configured.isBefore(showtimeStart) ? configured : showtimeStart;
        return existingDeadline == null || existingDeadline.isBefore(deadline)
                ? existingDeadline == null ? deadline : existingDeadline
                : deadline;
    }

    private void registerRedisCleanup(Long showtimeId, List<Long> seatIds, String ownerToken) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (redisLockService != null) {
                        redisLockService.releaseLocks(showtimeId, seatIds, ownerToken);
                    }
                }
            });
        } else {
            if (redisLockService != null) {
                redisLockService.releaseLocks(showtimeId, seatIds, ownerToken);
            }
        }
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
        if (lifecycleService != null) {
            return bookingMapper.toResponse(lifecycleService.cancel(
                    booking, reasonCode, reasonDetail, "CUSTOMER"));
        }
        booking.cancel(reasonCode, reasonDetail, Instant.now());
        Booking saved = bookingRepository.save(booking);
        reservationService.handleBookingStatusChange(saved.getId(), BookingStatus.CANCELLED,
                reasonDetail != null ? reasonDetail : reasonCode);

        bookingMetricsManager.incrementBookingCancelled();

        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(String publicId) {
        throw new BusinessException("CONFIRM_VIA_PAYMENT_RESULT_REQUIRED",
                "Booking confirmation is performed only by a validated Payment result",
                HttpStatus.GONE);
    }

    @Override
    public Optional<BookingResponse> findActiveByShowtime(String showtimePublicId) {
        Long currentUserId = requireAuthenticatedUser();
        String normalizedShowtimePublicId = normalizeShowtimePublicId(showtimePublicId);
        return bookingRepository.findActiveByUserAndShowtimePublicId(
                        currentUserId,
                        normalizedShowtimePublicId,
                        BookingStatus.PENDING_PAYMENT,
                        Instant.now())
                .stream()
                .findFirst()
                .map(bookingMapper::toResponse);
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
        return toCustomerDetailResponse(booking);
    }

    @Override
    public BookingDetailResponse findByCode(String bookingCode) {
        Long currentUserId = requireAuthenticatedUser();
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .filter(this::isActive)
                .orElseThrow(() -> new BookingNotFoundException(bookingCode));
        MDC.put("bookingId", booking.getPublicId());
        requireOwnerOrAdmin(booking, currentUserId);
        return toCustomerDetailResponse(booking);
    }

    @Override
    public Page<BookingSummaryResponse> findAll(
            BookingStatus status, Instant fromDate, Instant toDate, Pageable pageable) {
        if (!securityContextService.isAdmin()) {
            throw new ForbiddenException("Only administrators can list all bookings");
        }
        validateDateRange(fromDate, toDate);
        return bookingRepository.findAll(buildSpecification(null, status, fromDate, toDate), pageable)
                .map(this::toCustomerSummaryResponse);
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
                .map(this::toCustomerSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSpendingSummaryResponse getMySpendingSummary(int year) {
        Long currentUserId = requireAuthenticatedUser();
        ZoneId businessZone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant periodStart = LocalDate.of(year, 1, 1)
                .atStartOfDay(businessZone)
                .toInstant();
        Instant periodEnd = LocalDate.of(year + 1, 1, 1)
                .atStartOfDay(businessZone)
                .toInstant();
        BigDecimal totalSpending = bookingRepository.sumPaidSpendingByUserAndPeriod(
                currentUserId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED),
                PaymentStatus.SUCCESS,
                periodStart,
                periodEnd);

        return new BookingSpendingSummaryResponse(
                year,
                totalSpending == null
                        ? BigDecimal.ZERO.setScale(2)
                        : totalSpending.setScale(2, RoundingMode.HALF_UP),
                "VND",
                periodStart,
                periodEnd);
    }

    private BookingSummaryResponse toCustomerSummaryResponse(Booking booking) {
        return bookingMapper.toSummaryResponse(
                booking,
                buildPresentation(booking),
                buildFoodPresentation(booking));
    }

    private BookingDetailResponse toCustomerDetailResponse(Booking booking) {
        return bookingMapper.toDetailResponse(
                booking,
                buildPresentation(booking),
                buildFoodPresentation(booking));
    }

    private BookingPresentationResponse buildPresentation(Booking booking) {
        Optional<BookingSnapshot> displaySnapshot = bookingSnapshotRepository.findByBookingId(booking.getId());
        List<BookingPresentationResponse.SeatLine> seats = readSeatPriceLines(booking.getId());

        if (seats.isEmpty()) {
            seats = reservationRepository.findAllByBookingId(booking.getId()).stream()
                    .map(reservation -> new BookingPresentationResponse.SeatLine(
                            reservation.getSeatPublicId(),
                            reservation.getSeatLabel(),
                            reservation.getSeatType(),
                            null))
                    .toList();
        }

        BookingSnapshot snapshot = displaySnapshot.orElse(null);
        return new BookingPresentationResponse(
                snapshot == null ? null : snapshot.getMovieTitle(),
                snapshot == null ? null : snapshot.getMoviePoster(),
                snapshot == null ? null : snapshot.getOriginalTitle(),
                snapshot == null ? null : snapshot.getDuration(),
                snapshot == null ? null : snapshot.getAgeRating(),
                snapshot == null ? null : snapshot.getShowtimeStart(),
                snapshot == null ? null : snapshot.getShowtimeEnd(),
                snapshot == null ? null : snapshot.getCinemaName(),
                snapshot == null ? null : snapshot.getAuditoriumName(),
                seats);
    }

    private List<BookingPresentationResponse.SeatLine> readSeatPriceLines(Long bookingId) {
        return priceSnapshotRepository.findByBookingId(bookingId)
                .map(BookingPriceSnapshot::getPricingBreakdownJson)
                .filter(json -> json != null && !json.isBlank())
                .map(json -> {
                    try {
                        BookingPriceSnapshotPayload payload = objectMapper.readValue(
                                json, BookingPriceSnapshotPayload.class);
                        if (payload.seats() == null) {
                            return List.<BookingPresentationResponse.SeatLine>of();
                        }
                        return payload.seats().stream()
                                .map(seat -> new BookingPresentationResponse.SeatLine(
                                        seat.seatPublicId(),
                                        seat.seatLabel(),
                                        seat.seatType(),
                                        seat.unitPrice()))
                                .toList();
                    } catch (JsonProcessingException exception) {
                        log.warn("Cannot read price snapshot for bookingId={}", bookingId, exception);
                        return List.<BookingPresentationResponse.SeatLine>of();
                    }
                })
                .orElseGet(List::of);
    }

    private BookingFoodResponse buildFoodPresentation(Booking booking) {
        if (booking.getFoodOrder() == null) {
            return new BookingFoodResponse(0, BigDecimal.ZERO, List.of());
        }
        var foodOrder = booking.getFoodOrder();
        List<BookingFoodResponse.Item> items = foodOrder.getItems().stream()
                .map(item -> new BookingFoodResponse.Item(
                        item.getProductName(),
                        item.getProductImage(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getFinalAmount()))
                .toList();
        return new BookingFoodResponse(
                foodOrder.getTotalQuantity(),
                foodOrder.getFinalAmount(),
                items);
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
        if (lifecycleService != null) {
            return bookingMapper.toResponse(lifecycleService.transition(
                    booking, targetStatus, "Booking status changed to " + targetStatus,
                    "BOOKING_SERVICE"));
        }
        booking.changeStatus(targetStatus, Instant.now());
        Booking saved = bookingRepository.save(booking);

        // Sync status with Food Order is handled automatically inside booking.changeStatus()

        if (targetStatus == BookingStatus.CONFIRMED) {
            bookingTicketService.generateTicketsForConfirmedBooking(saved.getId());
            if (saved.getFoodOrder() != null) {
                com.lorafilm.booking.food.event.FoodOrderConfirmedEvent foodEvent = new com.lorafilm.booking.food.event.FoodOrderConfirmedEvent(
                        saved.getId().toString(),
                        saved.getFoodOrder().getPublicId(),
                        saved.getFoodOrder().getFinalAmount()
                );
                outboxService.createOutboxEvent("FoodOrder", saved.getFoodOrder().getId(), "FOOD_ORDER_CONFIRMED", foodEvent);
            }
        }

        if (targetStatus == BookingStatus.CANCELLED || targetStatus == BookingStatus.EXPIRED
                || targetStatus == BookingStatus.REFUNDED) {
            reservationService.handleBookingStatusChange(saved.getId(), targetStatus,
                    "Booking status changed to " + targetStatus);
            // Preserve ticket rows and mark them with the appropriate terminal
            // state for audit/reconciliation.
            try {
                if (targetStatus == BookingStatus.REFUNDED) {
                    bookingTicketService.refundTickets(saved.getId());
                } else {
                    bookingTicketService.deleteTickets(saved.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to update tickets for bookingId: {}", saved.getId(), e);
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
        List<String> seatPublicIds = request.getSeatPublicIds();
        List<String> reservationPublicIds = request.getReservationPublicIds();
        if ((seatPublicIds == null || seatPublicIds.isEmpty())
                == (reservationPublicIds == null || reservationPublicIds.isEmpty())) {
            throw new BusinessException("BOOKING_SEAT_SELECTION_REQUIRED",
                    "Provide either seatPublicIds or deprecated reservationPublicIds");
        }
        List<String> selected = seatPublicIds != null && !seatPublicIds.isEmpty() ? seatPublicIds : reservationPublicIds;
        if (selected.size() > bookingPolicyProperties.getMaxSeatsPerBooking()) {
            throw new BusinessException("BOOKING_TOO_MANY_RESERVATIONS",
                    "A booking cannot contain more than " + bookingPolicyProperties.getMaxSeatsPerBooking() + " seats");
        }

        List<String> normalizedPublicIds;
        try {
            normalizedPublicIds = selected.stream()
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
        return new ValidatedCreateRequest(showtimePublicId,
                seatPublicIds != null && !seatPublicIds.isEmpty() ? normalizedPublicIds : null,
                reservationPublicIds != null && !reservationPublicIds.isEmpty() ? normalizedPublicIds : null);
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
        if (context.startsAt() == null || !context.startsAt().isAfter(now)) {
            throw new IntegrationException("Movie Service returned an invalid showtime start");
        }
        if (context.seats() == null) {
            throw new IntegrationException("Movie Service returned incomplete seat information");
        }
        if (context.seats().size() != requestedSeatIds.size()) {
            throw new IntegrationException("Movie Service returned duplicate or extra seat price lines");
        }
        Set<Long> returnedSeatIds = context.seats().stream()
                .map(ShowtimeBookingContext.SeatContext::seatId)
                .collect(java.util.stream.Collectors.toSet());
        if (returnedSeatIds.size() != requestedSeatIds.size()
                || !returnedSeatIds.containsAll(requestedSeatIds)) {
            throw new IntegrationException("Movie Service returned mismatched seat information");
        }
        validateCoupleSeatPairs(context.seats());
        validatePricing(context);
    }

    private void validateCoupleSeatPairs(List<ShowtimeBookingContext.SeatContext> seats) {
        Map<String, List<ShowtimeBookingContext.SeatContext>> coupleGroups = new HashMap<>();
        for (ShowtimeBookingContext.SeatContext seat : seats) {
            boolean couple = "COUPLE".equalsIgnoreCase(seat.seatType());
            String pairGroup = seat.pairGroup() == null ? null : seat.pairGroup().trim();
            if (couple && (pairGroup == null || pairGroup.isEmpty())) {
                throw new IntegrationException(
                        "Movie Service returned a couple seat without pairGroup");
            }
            if (!couple && pairGroup != null && !pairGroup.isEmpty()) {
                throw new IntegrationException(
                        "Movie Service returned pairGroup for a non-couple seat");
            }
            if (couple) {
                coupleGroups.computeIfAbsent(pairGroup, ignored -> new ArrayList<>()).add(seat);
            }
        }

        for (Map.Entry<String, List<ShowtimeBookingContext.SeatContext>> entry
                : coupleGroups.entrySet()) {
            int memberCount = entry.getValue().size();
            if (memberCount == 1) {
                throw new BusinessException(
                        "SEAT_COUPLE_PAIR_REQUIRED",
                        "Couple seats must be booked together",
                        HttpStatus.BAD_REQUEST);
            }
            if (memberCount != 2) {
                throw new IntegrationException(
                        "Movie Service returned an invalid couple pairGroup");
            }
        }
    }

    private void validateSingleSeatGap(Long showtimeId, List<Long> selectedSeatIds) {
        ShowtimeSeatLayoutResponse layout = showtimeClient.getSeatLayout(showtimeId);
        if (layout == null || layout.getSeats() == null
                || !Objects.equals(layout.getShowtimeId(), showtimeId)) {
            throw new IntegrationException("Movie Service returned an invalid seat layout");
        }

        Set<Long> selected = new HashSet<>(selectedSeatIds);
        Set<Long> layoutSeatIds = layout.getSeats().stream()
                .map(ShowtimeSeatLayoutResponse.SeatDetailDto::getSeatId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (!layoutSeatIds.containsAll(selected)) {
            throw new IntegrationException("Movie Service seat layout omitted selected seats");
        }

        Instant now = Instant.now();
        Set<Long> occupied = reservationRepository
                .findAllActiveReservationsByShowtimeId(showtimeId, now)
                .stream()
                .map(SeatReservation::getSeatId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        occupied.addAll(reservationRepository.findSoldSeatIdsFromBookingsByShowtimeId(showtimeId));
        occupied.removeAll(selected);

        if (SingleSeatGapPolicy.leavesSingleSeatGap(layout.getSeats(), selected, occupied)) {
            throw new BusinessException(
                    "SEAT_SINGLE_GAP_NOT_ALLOWED",
                    "Seat selection must not leave an isolated single seat",
                    HttpStatus.BAD_REQUEST);
        }
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
        if (context.ticketAmount().signum() <= 0) {
            throw new IntegrationException("Movie Service returned a non-positive ticket amount");
        }
        BigDecimal seatLineTotal = BigDecimal.ZERO;
        for (ShowtimeBookingContext.SeatContext seat : context.seats()) {
            if (seat.seatId() == null || seat.price() == null || seat.price().signum() <= 0) {
                throw new IntegrationException("Movie Service returned an invalid seat price line");
            }
            if (!context.currency().equals(seat.currency())) {
                throw new IntegrationException("Movie Service returned mixed seat-line currencies");
            }
            seatLineTotal = seatLineTotal.add(seat.price());
        }
        if (seatLineTotal.compareTo(context.ticketAmount()) != 0) {
            throw new IntegrationException("Seat price lines do not equal the authoritative ticket amount");
        }
    }

    private void persistAuthoritativePriceSnapshot(Booking booking, ShowtimeBookingContext context) {
        if (priceSnapshotRepository.existsByBookingId(booking.getId())) {
            throw new BusinessException(
                    "BOOKING_PRICE_SNAPSHOT_EXISTS",
                    "The Booking already has an authoritative price snapshot",
                    HttpStatus.CONFLICT);
        }
        BookingPriceSnapshotPayload payload = new BookingPriceSnapshotPayload(
                context.showtimeId(),
                context.showtimePublicId(),
                Instant.now(),
                context.currency(),
                context.movieId(),
                context.moviePublicId(),
                context.movieTitle(),
                context.cinemaPublicId(),
                context.ticketAmount(),
                context.seats().stream()
                        .map(seat -> new BookingPriceSnapshotPayload.SeatPriceLine(
                                seat.seatId(), seat.seatLabel(), seat.seatType(), seat.price(),
                                seat.seatPublicId()))
                        .toList());
        BookingPriceSnapshot snapshot = new BookingPriceSnapshot();
        snapshot.setBooking(booking);
        snapshot.setCurrency(context.currency());
        snapshot.setPricingEngineVersion("showtime-snapshot-v1");
        try {
            snapshot.setPricingBreakdownJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IntegrationException("Cannot persist authoritative price snapshot", exception);
        }
        priceSnapshotRepository.save(snapshot);
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

    private record ValidatedCreateRequest(String showtimePublicId,
                                          List<String> seatPublicIds,
                                          List<String> reservationPublicIds) {
    }

    @Override
    @Transactional
    public BookingResponse finalizeCheckout(String publicId, FinalizeCheckoutRequest request) {
        Long currentUserId = requireAuthenticatedUser();
        Booking booking = bookingRepository.findByPublicIdWithLock(publicId)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BookingNotFoundException(publicId));
        requireOwnerOrAdmin(booking, currentUserId);
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("BOOKING_NOT_PENDING", "Only pending Bookings can be finalized", HttpStatus.CONFLICT);
        }
        Instant now = Instant.now();
        if (booking.getExpiresAt() == null || !now.isBefore(booking.getExpiresAt())) {
            throw new BusinessException("BOOKING_EXPIRED", "The Booking payment deadline has passed", HttpStatus.CONFLICT);
        }
        int requestedPoints = request == null ? 0 : request.normalizedScorePoints();
        String scoreIdempotencyKey = request == null ? null : request.scoreIdempotencyKey();

        if (booking.getAmountLockedAt() != null) {
            if (!Objects.equals(booking.getScorePointsUsed(), requestedPoints)) {
                throw new BusinessException(
                        "BOOKING_AMOUNT_ALREADY_LOCKED",
                        "Checkout amount was already locked with another score selection",
                        HttpStatus.CONFLICT);
            }
            return bookingMapper.toResponse(booking);
        }
        if (requestedPoints == 0) {
            booking.lockAmount(now);
            return bookingMapper.toResponse(bookingRepository.saveAndFlush(booking));
        }
        if (scoreIdempotencyKey == null || scoreIdempotencyKey.isBlank()) {
            throw new BusinessException(
                    "SCORE_IDEMPOTENCY_KEY_REQUIRED",
                    "scoreIdempotencyKey is required when scorePoints is greater than zero",
                    HttpStatus.BAD_REQUEST);
        }
        if (scoreRedemptionClient == null) {
            throw new IntegrationException("Score Service integration is unavailable");
        }

        long ttlWithSettlementGrace = Duration.between(now, booking.getExpiresAt()).getSeconds() + 300;
        int ttlSeconds = (int) Math.min(Integer.MAX_VALUE, Math.max(60, ttlWithSettlementGrace));
        String holdEventId = stableScoreKey("hold-event", scoreIdempotencyKey);
        ScoreRedemptionClient.ScoreHoldResult hold = scoreRedemptionClient.hold(
                booking.getUserId(),
                booking.getId(),
                requestedPoints,
                ttlSeconds,
                booking.getFinalAmount(),
                holdEventId,
                scoreIdempotencyKey.trim());
        try {
            if (hold == null
                    || !"ACTIVE".equalsIgnoreCase(hold.status())
                    || hold.pointsHeld() != requestedPoints
                    || hold.discountAmount() == null
                    || hold.discountAmount().signum() <= 0) {
                throw new IntegrationException("Score Service did not create a valid active hold");
            }
            booking.applyScoreRedemption(
                    requestedPoints,
                    hold.discountAmount(),
                    hold.holdCode());
            booking.lockAmount(now);
            return bookingMapper.toResponse(bookingRepository.saveAndFlush(booking));
        } catch (RuntimeException exception) {
            try {
                scoreRedemptionClient.release(
                        booking.getId(),
                        hold == null ? null : hold.holdCode(),
                        "Booking checkout finalization failed",
                        stableScoreKey("release-event", scoreIdempotencyKey),
                        stableScoreKey("release", scoreIdempotencyKey));
            } catch (RuntimeException releaseException) {
                log.error("Failed to compensate Score hold for Booking {}", publicId, releaseException);
            }
            throw exception;
        }
    }

    private String stableScoreKey(String purpose, String source) {
        return purpose + "-" + UUID.nameUUIDFromBytes(
                (purpose + ":" + source).getBytes(StandardCharsets.UTF_8));
    }
}
