package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeBlockedSeat;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.response.CustomerBookingOptionResponse;
import com.lorafilm.movie.showtime.dto.response.CustomerSeatLayoutResponse;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CustomerShowtimeService {
    public static final int MAX_OPTION_RANGE_DAYS = 14;

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimePriceRepository priceRepository;
    private final ShowtimeBlockedSeatRepository blockedSeatRepository;
    private final SeatService seatService;

    public CustomerShowtimeService(ShowtimeRepository showtimeRepository,
                                   ShowtimePriceRepository priceRepository,
                                   ShowtimeBlockedSeatRepository blockedSeatRepository,
                                   SeatService seatService) {
        this.showtimeRepository = showtimeRepository;
        this.priceRepository = priceRepository;
        this.blockedSeatRepository = blockedSeatRepository;
        this.seatService = seatService;
    }

    public List<CustomerBookingOptionResponse> getBookingOptions(
            String movieIdentifier, LocalDate from, LocalDate to) {
        if (from == null) {
            from = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
        if (to == null) {
            to = from.plusDays(4);
        }
        if (to.isBefore(from) || to.isAfter(from.plusDays(MAX_OPTION_RANGE_DAYS - 1L))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Booking option range must contain between 1 and 14 service dates");
        }
        return showtimeRepository.findCustomerBookingOptions(movieIdentifier, from, to).stream()
                .collect(Collectors.toMap(
                        Showtime::getPublicId,
                        Function.identity(),
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new))
                .values().stream()
                .map(this::toBookingOption)
                .toList();
    }

    public CustomerSeatLayoutResponse getSeatLayout(String showtimePublicId) {
        Showtime showtime = customerVisibleShowtime(showtimePublicId);
        ZoneId zone = ZoneId.of(showtime.getCinema().getTimezone());
        Map<Long, ShowtimePrice> prices = priceRepository.findByShowtimeId(showtime.getId()).stream()
                .collect(Collectors.toMap(price -> price.getSeatType().getId(), Function.identity()));
        Set<Long> blockedIds = blockedSeatRepository
                .findByShowtimeIdAndStatus(showtime.getId(), ActionStatus.ACTIVE).stream()
                .map(ShowtimeBlockedSeat::getSeat)
                .map(Seat::getId)
                .collect(Collectors.toSet());

        List<CustomerSeatLayoutResponse.CustomerSeat> seats =
                seatService.getSeatsByAuditoriumId(showtime.getAuditorium().getId()).stream()
                        .sorted(Comparator.comparing(Seat::getPositionRow,
                                        Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(Seat::getPositionColumn,
                                        Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(Seat::getSeatCode,
                                        Comparator.nullsLast(String::compareTo)))
                        .map(seat -> toCustomerSeat(seat, prices.get(seat.getSeatType().getId()),
                                blockedIds.contains(seat.getId())))
                        .toList();

        return new CustomerSeatLayoutResponse(
                showtime.getPublicId(), showtime.getServiceDate(),
                showtime.getStartTime(), showtime.getEndTime(),
                showtime.getStartTime().atZone(zone).toLocalDateTime(),
                showtime.getEndTime().atZone(zone).toLocalDateTime(),
                new CustomerSeatLayoutResponse.MovieContext(
                        showtime.getMovie().getPublicId(), showtime.getMovie().getSlug(),
                        showtime.getMovie().getTitle()),
                new CustomerSeatLayoutResponse.VersionContext(
                        showtime.getMovieVersion().getPublicId(),
                        showtime.getMovieVersion().getVersionName(),
                        showtime.getMovieVersion().getFormat() == null ? null
                                : showtime.getMovieVersion().getFormat().getValue(),
                        showtime.getMovieVersion().getAudioLanguage(),
                        showtime.getMovieVersion().getSubtitleLanguage()),
                new CustomerSeatLayoutResponse.CinemaContext(
                        showtime.getCinema().getPublicId(), showtime.getCinema().getSlug(),
                        showtime.getCinema().getName(), showtime.getCinema().getTimezone()),
                new CustomerSeatLayoutResponse.AuditoriumContext(
                        showtime.getAuditorium().getPublicId(), showtime.getAuditorium().getName(),
                        showtime.getAuditorium().getScreenType() == null ? null
                                : showtime.getAuditorium().getScreenType().getValue(),
                        showtime.getAuditorium().getSoundType() == null ? null
                                : showtime.getAuditorium().getSoundType().name()),
                seats);
    }

    private CustomerBookingOptionResponse toBookingOption(Showtime showtime) {
        ZoneId zone = ZoneId.of(showtime.getCinema().getTimezone());
        return new CustomerBookingOptionResponse(
                showtime.getPublicId(), showtime.getServiceDate(),
                showtime.getStartTime(), showtime.getEndTime(),
                showtime.getStartTime().atZone(zone).toLocalDateTime(),
                showtime.getEndTime().atZone(zone).toLocalDateTime(),
                showtime.getCinema().getPublicId(), showtime.getCinema().getSlug(),
                showtime.getCinema().getName(), showtime.getCinema().getAddress(),
                showtime.getCinema().getCity(), showtime.getCinema().getTimezone(),
                showtime.getAuditorium().getPublicId(), showtime.getAuditorium().getName(),
                showtime.getAuditorium().getScreenType() == null ? null
                        : showtime.getAuditorium().getScreenType().getValue(),
                showtime.getAuditorium().getSoundType() == null ? null
                        : showtime.getAuditorium().getSoundType().name(),
                showtime.getMovieVersion().getPublicId(), showtime.getMovieVersion().getVersionName(),
                showtime.getMovieVersion().getFormat() == null ? null
                        : showtime.getMovieVersion().getFormat().getValue(),
                showtime.getMovieVersion().getAudioLanguage(),
                showtime.getMovieVersion().getSubtitleLanguage(),
                showtime.getStatus().name());
    }

    private CustomerSeatLayoutResponse.CustomerSeat toCustomerSeat(
            Seat seat, ShowtimePrice snapshot, boolean blocked) {
        BigDecimal price = snapshot == null ? null : snapshot.getPrice();
        boolean priced = price != null && price.signum() > 0;
        boolean sellable = seat.getStatus() == SeatStatus.ACTIVE && !blocked && priced;
        return new CustomerSeatLayoutResponse.CustomerSeat(
                seat.getPublicId(), seat.getSeatCode(), seat.getRowLabel(), seat.getSeatNumber(),
                seat.getPositionRow(), seat.getPositionColumn(),
                seat.getSeatType().getCode().name(), seat.getSeatType().getName(),
                priced ? price : null, snapshot == null ? null : snapshot.getCurrency(),
                seat.getStatus().name(), blocked, priced, sellable);
    }

    private Showtime customerVisibleShowtime(String publicId) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        if (showtime.getStatus() != ShowtimeStatus.OPEN_FOR_BOOKING) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND,
                    "Showtime is not open for booking");
        }
        return showtime;
    }
}
