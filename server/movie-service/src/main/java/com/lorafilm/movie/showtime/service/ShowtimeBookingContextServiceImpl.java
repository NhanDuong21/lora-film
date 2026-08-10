package com.lorafilm.movie.showtime.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.util.AccessibleSeatPricing;
import com.lorafilm.movie.pricing.util.SeatPriceAllocation;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeBlockedSeat;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.ShowtimeAuditoriumDto;
import com.lorafilm.movie.showtime.dto.ShowtimeCinemaDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMapper;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieVersionDto;
import com.lorafilm.movie.showtime.dto.request.BookingContextRequest;
import com.lorafilm.movie.showtime.dto.response.BookingContextPricingDto;
import com.lorafilm.movie.showtime.dto.response.BookingContextResponse;
import com.lorafilm.movie.showtime.dto.response.BookingContextSeatDto;
import com.lorafilm.movie.showtime.dto.response.BookingContextShowtimeDto;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;

@Service
public class ShowtimeBookingContextServiceImpl implements ShowtimeBookingContextService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimePriceRepository showtimePriceRepository;
    private final ShowtimeBlockedSeatRepository showtimeBlockedSeatRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final SeatService seatService;
    private final ShowtimeMapper showtimeMapper;

    public ShowtimeBookingContextServiceImpl(ShowtimeRepository showtimeRepository,
                               ShowtimePriceRepository showtimePriceRepository,
                               ShowtimeBlockedSeatRepository showtimeBlockedSeatRepository,
                               MovieMediaRepository movieMediaRepository,
                               SeatService seatService,
                               ShowtimeMapper showtimeMapper) {
        this.showtimeRepository = showtimeRepository;
        this.showtimePriceRepository = showtimePriceRepository;
        this.showtimeBlockedSeatRepository = showtimeBlockedSeatRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.seatService = seatService;
        this.showtimeMapper = showtimeMapper;
    }

    // removed query methods

    @Override
    public BookingContextResponse getBookingContext(Long showtimeId, BookingContextRequest request) {
        Showtime showtime = showtimeRepository.findByIdAndDeletedAtIsNull(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        if (showtime.getStatus() != ShowtimeStatus.OPEN_FOR_BOOKING) {
            throw new BusinessException(ErrorCode.INVALID_SHOWTIME_STATUS_TRANSITION, "Showtime is not open for booking");
        }

        Set<Long> uniqueSeatIds = new java.util.HashSet<>(request.getSeatIds());
        if (uniqueSeatIds.size() != request.getSeatIds().size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate seat IDs are not allowed");
        }

        List<Seat> seats = seatService.getSeatsByIds(request.getSeatIds());
        
        if (seats == null || seats.isEmpty() || seats.size() != request.getSeatIds().size()) {
            throw new ResourceNotFoundException("One or more seats not found");
        }

        List<ShowtimePrice> prices = showtimePriceRepository.findByShowtimeId(showtime.getId());
        List<ShowtimeBlockedSeat> blockedSeats = showtimeBlockedSeatRepository.findByShowtimeIdAndStatus(showtime.getId(), ActionStatus.ACTIVE);
        Set<Long> blockedSeatIds = blockedSeats.stream().map(b -> b.getSeat().getId()).collect(Collectors.toSet());

        for (Seat seat : seats) {
            if (!seat.getAuditorium().getId().equals(showtime.getAuditorium().getId())) {
                throw new BusinessException(ErrorCode.SEAT_BELONGS_TO_ANOTHER_AUDITORIUM, "Seat " + seat.getId() + " belongs to another auditorium");
            }
            if (seat.getStatus() != SeatStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.SEAT_INACTIVE, "Seat " + seat.getId() + " is inactive");
            }
            if (blockedSeatIds.contains(seat.getId())) {
                throw new BusinessException(ErrorCode.SEAT_BLOCKED_FOR_SHOWTIME, "Seat " + seat.getId() + " is blocked for this showtime");
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        String currency = "VND";

        List<BookingContextSeatDto> seatDtos = new java.util.ArrayList<>();
        for (Seat seat : seats) {
            ShowtimePrice showtimePrice = AccessibleSeatPricing.findPrice(prices, seat.getSeatType());
            if (showtimePrice == null) {
                throw new BusinessException(ErrorCode.SHOWTIME_PRICE_MISSING, "Missing price for seat type: " + seat.getSeatType().getCode());
            }
            if (showtimePrice.getPrice() == null || showtimePrice.getPrice().signum() <= 0) {
                throw new BusinessException(ErrorCode.PRICING_INCOMPLETE,
                        "Invalid price for seat type: " + seat.getSeatType().getCode());
            }
            if (!"VND".equals(showtimePrice.getCurrency())) {
                throw new BusinessException(ErrorCode.PRICING_INCOMPLETE,
                        "Unsupported Showtime currency: " + showtimePrice.getCurrency());
            }

            BookingContextSeatDto seatDto = new BookingContextSeatDto();
            seatDto.setSeatId(seat.getId());
            seatDto.setSeatPublicId(seat.getPublicId());
            seatDto.setSeatCode(seat.getSeatCode());
            seatDto.setSeatType(seat.getSeatType().getCode().name());
            seatDto.setPairGroup(seat.getPairGroup());
            BigDecimal allocatedPrice = SeatPriceAllocation.perPhysicalSeat(
                    seat.getSeatType().getCode(), showtimePrice.getPrice());
            seatDto.setPrice(allocatedPrice);
            seatDto.setCurrency(showtimePrice.getCurrency());
            seatDtos.add(seatDto);

            totalAmount = totalAmount.add(allocatedPrice);
            currency = showtimePrice.getCurrency();
        }

        BookingContextResponse response = new BookingContextResponse();
        response.setMovieId(showtime.getMovie().getId());
        response.setCinemaId(showtime.getCinema().getId());
        response.setAuditoriumId(showtime.getAuditorium().getId());

        BookingContextShowtimeDto showtimeDto = new BookingContextShowtimeDto();
        showtimeDto.setId(showtime.getId());
        showtimeDto.setPublicId(showtime.getPublicId());
        showtimeDto.setStatus(showtime.getStatus().name());
        showtimeDto.setServiceDate(showtime.getServiceDate());
        
        java.time.ZoneId zoneId = java.time.ZoneId.of(showtime.getCinema().getTimezone());
        showtimeDto.setStartAt(OffsetDateTime.ofInstant(showtime.getStartTime(), zoneId));
        showtimeDto.setEndAt(OffsetDateTime.ofInstant(showtime.getEndTime(), zoneId));
        response.setShowtime(showtimeDto);

        ShowtimeMovieDto movieDto = new ShowtimeMovieDto();
        movieDto.setPublicId(showtime.getMovie().getPublicId());
        movieDto.setSlug(showtime.getMovie().getSlug());
        movieDto.setTitle(showtime.getMovie().getTitle());
        movieDto.setDurationMinutes(showtime.getMovie().getDurationMinutes());
        movieDto.setAgeRating(showtime.getMovie().getAgeRating() == null
                ? null : showtime.getMovie().getAgeRating().name());
        movieMediaRepository.findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        showtime.getMovie().getId(), MovieMediaType.POSTER, ActiveStatus.ACTIVE)
                .ifPresent(media -> movieDto.setPosterUrl(media.getUrl()));
        response.setMovie(movieDto);

        ShowtimeMovieVersionDto versionDto = new ShowtimeMovieVersionDto();
        versionDto.setPublicId(showtime.getMovieVersion().getPublicId());
        versionDto.setVersionName(showtime.getMovieVersion().getVersionName());
        versionDto.setFormat(showtime.getMovieVersion().getFormat() != null ? showtime.getMovieVersion().getFormat().getValue() : null);
        versionDto.setAudioLanguage(showtime.getMovieVersion().getAudioLanguage());
        versionDto.setSubtitleLanguage(showtime.getMovieVersion().getSubtitleLanguage());
        response.setMovieVersion(versionDto);

        ShowtimeCinemaDto cinemaDto = new ShowtimeCinemaDto();
        cinemaDto.setPublicId(showtime.getCinema().getPublicId());
        cinemaDto.setSlug(showtime.getCinema().getSlug());
        cinemaDto.setName(showtime.getCinema().getName());
        cinemaDto.setTimezone(showtime.getCinema().getTimezone());
        response.setCinema(cinemaDto);

        ShowtimeAuditoriumDto auditoriumDto = new ShowtimeAuditoriumDto();
        auditoriumDto.setPublicId(showtime.getAuditorium().getPublicId());
        auditoriumDto.setName(showtime.getAuditorium().getName());
        auditoriumDto.setScreenType(showtime.getAuditorium().getScreenType() != null ? showtime.getAuditorium().getScreenType().getValue() : null);
        auditoriumDto.setSoundType(showtime.getAuditorium().getSoundType() != null ? showtime.getAuditorium().getSoundType().name() : null);
        auditoriumDto.setCapacity(showtime.getAuditorium().getCapacity());
        response.setAuditorium(auditoriumDto);

        response.setSelectedSeats(seatDtos);

        BookingContextPricingDto pricingDto = new BookingContextPricingDto();
        pricingDto.setSeatAmount(totalAmount);
        pricingDto.setTotalAmount(totalAmount);
        pricingDto.setCurrency(currency);
        response.setPricing(pricingDto);

        response.setBookingExpiredAt(OffsetDateTime.now().plusMinutes(15));

        return response;
    }

    @Override
    public ShowtimeMovieDto getPresentationByPublicId(String showtimePublicId) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        ShowtimeMovieDto movieDto = new ShowtimeMovieDto();
        movieDto.setPublicId(showtime.getMovie().getPublicId());
        movieDto.setSlug(showtime.getMovie().getSlug());
        movieDto.setTitle(showtime.getMovie().getTitle());
        movieDto.setDurationMinutes(showtime.getMovie().getDurationMinutes());
        movieDto.setAgeRating(showtime.getMovie().getAgeRating() == null
                ? null : showtime.getMovie().getAgeRating().name());
        movieMediaRepository.findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        showtime.getMovie().getId(), MovieMediaType.POSTER, ActiveStatus.ACTIVE)
                .ifPresent(media -> movieDto.setPosterUrl(media.getUrl()));
        return movieDto;
    }

    @Override
    public BookingContextResponse getBookingContextByPublicId(String showtimePublicId, List<String> seatPublicIds) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        if (seatPublicIds == null || seatPublicIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Seat public IDs cannot be empty");
        }
        Set<String> unique = new java.util.HashSet<>(seatPublicIds);
        if (unique.size() != seatPublicIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate seat public IDs are not allowed");
        }
        List<Seat> seats = seatService.getSeatsByPublicIds(seatPublicIds);
        if (seats.size() != seatPublicIds.size()) {
            throw new ResourceNotFoundException("One or more seats not found");
        }
        return getBookingContext(showtime.getId(),
                new BookingContextRequest(seats.stream().map(Seat::getId).toList()));
    }

    // removed getActiveShowtime
}
