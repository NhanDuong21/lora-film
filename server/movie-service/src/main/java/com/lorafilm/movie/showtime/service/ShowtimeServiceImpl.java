package com.lorafilm.movie.showtime.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.SeatLayoutDto;
import com.lorafilm.movie.showtime.dto.ShowtimeDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieVersionDto;
import com.lorafilm.movie.showtime.dto.ShowtimeCinemaDto;
import com.lorafilm.movie.showtime.dto.ShowtimeAuditoriumDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMapper;
import com.lorafilm.movie.showtime.repository.ShowtimePriceRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeSpecification;

@Service
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimePriceRepository showtimePriceRepository;
    private final SeatService seatService;
    private final ShowtimeMapper showtimeMapper;

    public ShowtimeServiceImpl(ShowtimeRepository showtimeRepository,
                               ShowtimePriceRepository showtimePriceRepository,
                               SeatService seatService,
                               ShowtimeMapper showtimeMapper) {
        this.showtimeRepository = showtimeRepository;
        this.showtimePriceRepository = showtimePriceRepository;
        this.seatService = seatService;
        this.showtimeMapper = showtimeMapper;
    }

    @Override
    public PageResponse<ShowtimeDto> getShowtimes(String movieSlug, String cinemaSlug, String city, LocalDate date, int page, int size) {
        Specification<Showtime> spec = Specification.where(ShowtimeSpecification.isNotDeleted())
                .and(ShowtimeSpecification.hasStatus(ShowtimeStatus.OPEN_FOR_BOOKING));

        if (movieSlug != null && !movieSlug.isEmpty()) {
            spec = spec.and(ShowtimeSpecification.hasMovieSlug(movieSlug));
        }
        if (cinemaSlug != null && !cinemaSlug.isEmpty()) {
            spec = spec.and(ShowtimeSpecification.hasCinemaSlug(cinemaSlug));
        }
        if (city != null && !city.isEmpty()) {
            spec = spec.and(ShowtimeSpecification.hasCity(city));
        }
        if (date != null) {
            spec = spec.and(ShowtimeSpecification.hasDate(date));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Showtime> showtimePage = showtimeRepository.findAll(spec, pageable);

        List<ShowtimeDto> showtimeDtos = showtimePage.getContent().stream()
                .map(showtimeMapper::toDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                showtimeDtos,
                showtimePage.getNumber(),
                showtimePage.getSize(),
                showtimePage.getTotalElements(),
                showtimePage.getTotalPages(),
                showtimePage.isLast()
        );
    }

    @Override
    public ShowtimeDto getShowtimeByPublicId(String publicId) {
        Showtime showtime = getActiveShowtime(publicId);
        return showtimeMapper.toDto(showtime);
    }

    @Override
    public SeatLayoutDto getSeatLayout(String publicId) {
        Showtime showtime = getActiveShowtime(publicId);

        List<Seat> seats = seatService.getSeatsByAuditoriumId(showtime.getAuditorium().getId());
        List<ShowtimePrice> prices = showtimePriceRepository.findByShowtimeId(showtime.getId());

        Map<Long, ShowtimePrice> priceMap = prices.stream()
                .collect(Collectors.toMap(p -> p.getSeatType().getId(), p -> p));

        List<SeatLayoutDto.SeatPriceDto> seatPriceDtos = seats.stream().map(seat -> {
            ShowtimePrice showtimePrice = priceMap.get(seat.getSeatType().getId());
            BigDecimal price = showtimePrice != null ? showtimePrice.getPrice() : BigDecimal.ZERO;
            String currency = showtimePrice != null ? showtimePrice.getCurrency() : "VND";

            SeatLayoutDto.SeatPriceDto dto = new SeatLayoutDto.SeatPriceDto();
            dto.setPublicId(seat.getPublicId());
            dto.setSeatCode(seat.getSeatCode());
            dto.setRowLabel(seat.getRowLabel());
            dto.setSeatNumber(seat.getSeatNumber());
            dto.setPositionRow(seat.getPositionRow());
            dto.setPositionColumn(seat.getPositionColumn());
            dto.setSeatType(seat.getSeatType().getCode().name());
            dto.setPrice(price);
            dto.setCurrency(currency);
            dto.setStatus(seat.getStatus().name());
            dto.setBlockedForShowtime(false); // Logic to check blocked seats can be added here
            return dto;
        }).collect(Collectors.toList());

        SeatLayoutDto layout = new SeatLayoutDto();
        layout.setShowtimePublicId(showtime.getPublicId());

        ShowtimeMovieDto movieDto = new ShowtimeMovieDto();
        movieDto.setPublicId(showtime.getMovie().getPublicId());
        movieDto.setSlug(showtime.getMovie().getSlug());
        movieDto.setTitle(showtime.getMovie().getTitle());
        layout.setMovie(movieDto);

        ShowtimeMovieVersionDto versionDto = new ShowtimeMovieVersionDto();
        versionDto.setPublicId(showtime.getMovieVersion().getPublicId());
        versionDto.setVersionName(showtime.getMovieVersion().getVersionName());
        versionDto.setFormat(showtime.getMovieVersion().getFormat() != null ? showtime.getMovieVersion().getFormat().name() : null);
        versionDto.setAudioLanguage(showtime.getMovieVersion().getAudioLanguage());
        versionDto.setSubtitleLanguage(showtime.getMovieVersion().getSubtitleLanguage());
        layout.setMovieVersion(versionDto);

        ShowtimeCinemaDto cinemaDto = new ShowtimeCinemaDto();
        cinemaDto.setPublicId(showtime.getCinema().getPublicId());
        cinemaDto.setSlug(showtime.getCinema().getSlug());
        cinemaDto.setName(showtime.getCinema().getName());
        cinemaDto.setTimezone(showtime.getCinema().getTimezone());
        layout.setCinema(cinemaDto);

        ShowtimeAuditoriumDto auditoriumDto = new ShowtimeAuditoriumDto();
        auditoriumDto.setPublicId(showtime.getAuditorium().getPublicId());
        auditoriumDto.setName(showtime.getAuditorium().getName());
        auditoriumDto.setScreenType(showtime.getAuditorium().getScreenType() != null ? showtime.getAuditorium().getScreenType().name() : null);
        auditoriumDto.setSoundType(showtime.getAuditorium().getSoundType() != null ? showtime.getAuditorium().getSoundType().name() : null);
        layout.setAuditorium(auditoriumDto);

        layout.setStartTime(showtime.getStartTime());
        layout.setEndTime(showtime.getEndTime());
        layout.setSeats(seatPriceDtos);
        return layout;
    }

    private Showtime getActiveShowtime(String publicId) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        if (showtime.getStatus() != ShowtimeStatus.OPEN_FOR_BOOKING) {
            throw new ResourceNotFoundException("Showtime not found or not open for booking");
        }
        return showtime;
    }
}
