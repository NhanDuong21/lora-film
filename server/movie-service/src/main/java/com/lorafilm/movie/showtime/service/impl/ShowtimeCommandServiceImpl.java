package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.pricing.dto.response.ShowtimePricesResponse;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.CreateShowtimeRequest;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeMapper;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.service.ShowtimeCommandService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationContext;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ShowtimeCommandServiceImpl implements ShowtimeCommandService {

    private final MovieRepository movieRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeValidationService showtimeValidationService;
    private final ShowtimeStatusHistoryService showtimeStatusHistoryService;
    private final AdminShowtimeMapper adminShowtimeMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ShowtimePricingService showtimePricingService;

    public ShowtimeCommandServiceImpl(MovieRepository movieRepository,
                                      MovieVersionRepository movieVersionRepository,
                                      CinemaRepository cinemaRepository,
                                      AuditoriumRepository auditoriumRepository,
                                      ShowtimeRepository showtimeRepository,
                                      ShowtimeValidationService showtimeValidationService,
                                      ShowtimeStatusHistoryService showtimeStatusHistoryService,
                                      AdminShowtimeMapper adminShowtimeMapper,
                                      CurrentUserProvider currentUserProvider,
                                      ShowtimePricingService showtimePricingService) {
        this.movieRepository = movieRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.cinemaRepository = cinemaRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.showtimeRepository = showtimeRepository;
        this.showtimeValidationService = showtimeValidationService;
        this.showtimeStatusHistoryService = showtimeStatusHistoryService;
        this.adminShowtimeMapper = adminShowtimeMapper;
        this.currentUserProvider = currentUserProvider;
        this.showtimePricingService = showtimePricingService;
    }

    @Override
    @Transactional
    public AdminShowtimeResponse createShowtime(CreateShowtimeRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Current user not available");
        }

        Movie movie = resolveMovie(request.getMoviePublicId());
        MovieVersion movieVersion = resolveMovieVersion(request.getMovieVersionPublicId());
        Cinema cinema = resolveCinema(request.getCinemaPublicId());
        Auditorium auditoriumSnapshot = resolveAuditorium(request.getAuditoriumPublicId());

        Auditorium auditorium = auditoriumRepository.findByIdForScheduling(auditoriumSnapshot.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_SCHEDULING_CONFLICT, "Scheduling lock conflict"));

        Instant endTime = calculateEndTime(request.getStartTime(), movie);

        ShowtimeValidationContext context = buildContext(movie, movieVersion, cinema, auditorium, request.getStartTime(), endTime, null);
        showtimeValidationService.validateScheduling(context);

        Showtime showtime = new Showtime();
        showtime.setPublicId(UUID.randomUUID().toString());
        showtime.setMovie(movie);
        showtime.setMovieVersion(movieVersion);
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setStatus(ShowtimeStatus.DRAFT);
        showtime.setCancellationReason(null);
        showtime.setBookingOpenTime(null);
        showtime.setBookingCloseTime(null);

        showtime = showtimeRepository.saveAndFlush(showtime);

        showtimeStatusHistoryService.recordInitialHistory(showtime, currentUserId);

        PriceResolutionResult pricing = showtimePricingService.resolveAndReplace(showtime);
        return withPricing(adminShowtimeMapper.toAdminResponse(showtime), pricing);
    }

    @Override
    @Transactional
    public AdminShowtimeResponse updateShowtime(String showtimePublicId, UpdateShowtimeRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Current user not available");
        }

        Showtime snapshot = showtimeRepository.findByPublicIdAndDeletedAtIsNull(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        
        if (snapshot.getStatus() != ShowtimeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_SHOWTIME_STATUS_TRANSITION, "Only draft showtimes can be updated");
        }
        Long previousCinemaId = snapshot.getCinema().getId();
        Long previousAuditoriumId = snapshot.getAuditorium().getId();
        Instant previousStartTime = snapshot.getStartTime();

        Auditorium targetAuditoriumSnapshot = resolveAuditorium(request.getAuditoriumPublicId());
        
        List<Long> auditoriumIds = Stream.of(snapshot.getAuditorium().getId(), targetAuditoriumSnapshot.getId())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        for (Long audId : auditoriumIds) {
            auditoriumRepository.findByIdForScheduling(audId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_SCHEDULING_CONFLICT, "Scheduling lock conflict"));
        }

        Showtime lockedShowtime = showtimeRepository.findByPublicIdForUpdate(showtimePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_SCHEDULING_CONFLICT, "Scheduling lock conflict"));

        if (lockedShowtime.getStatus() != ShowtimeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_SHOWTIME_STATUS_TRANSITION, "Only draft showtimes can be updated");
        }

        Movie movie = resolveMovie(request.getMoviePublicId());
        MovieVersion movieVersion = resolveMovieVersion(request.getMovieVersionPublicId());
        Cinema cinema = resolveCinema(request.getCinemaPublicId());
        Auditorium targetAuditorium = auditoriumRepository.findByIdForScheduling(targetAuditoriumSnapshot.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_SCHEDULING_CONFLICT, "Scheduling lock conflict"));

        Instant endTime = calculateEndTime(request.getStartTime(), movie);

        ShowtimeValidationContext context = buildContext(movie, movieVersion, cinema, targetAuditorium, request.getStartTime(), endTime, lockedShowtime.getId());
        showtimeValidationService.validateScheduling(context);

        lockedShowtime.setMovie(movie);
        lockedShowtime.setMovieVersion(movieVersion);
        lockedShowtime.setCinema(cinema);
        lockedShowtime.setAuditorium(targetAuditorium);
        lockedShowtime.setStartTime(request.getStartTime());
        lockedShowtime.setEndTime(endTime);

        showtimeRepository.flush();

        boolean pricingDimensionsChanged =
                !Objects.equals(previousCinemaId, cinema.getId())
                || !Objects.equals(previousAuditoriumId, targetAuditorium.getId())
                || !Objects.equals(previousStartTime, request.getStartTime());
        if (pricingDimensionsChanged) {
            PriceResolutionResult pricing = showtimePricingService.resolveAndReplace(lockedShowtime);
            return withPricing(adminShowtimeMapper.toAdminResponse(lockedShowtime), pricing);
        }
        ShowtimePricesResponse pricing = showtimePricingService.getPrices(showtimePublicId);
        return withPricing(adminShowtimeMapper.toAdminResponse(lockedShowtime), pricing);
    }

    private Movie resolveMovie(String publicId) {
        return movieRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    }

    private MovieVersion resolveMovieVersion(String publicId) {
        return movieVersionRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie version not found"));
    }

    private Cinema resolveCinema(String publicId) {
        return cinemaRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));
    }

    private Auditorium resolveAuditorium(String publicId) {
        return auditoriumRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditorium not found"));
    }

    private Instant calculateEndTime(Instant startTime, Movie movie) {
        Integer duration = movie.getDurationMinutes();
        if (duration == null || duration <= 0) {
            throw new BusinessException(ErrorCode.INVALID_MOVIE_DURATION, "Invalid movie duration");
        }
        return startTime.plus(duration, ChronoUnit.MINUTES);
    }

    private ShowtimeValidationContext buildContext(Movie movie, MovieVersion movieVersion, Cinema cinema, Auditorium auditorium, Instant startTime, Instant endTime, Long excludedShowtimeId) {
        return ShowtimeValidationContext.builder()
                .movie(movie)
                .movieVersion(movieVersion)
                .cinema(cinema)
                .auditorium(auditorium)
                .startTime(startTime)
                .endTime(endTime)
                .excludeShowtimeId(excludedShowtimeId)
                .build();
    }

    @Override
    @Transactional
    public void deleteBatch(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Batch ID is required");
        }

        throw new BusinessException(
                ErrorCode.SHOWTIME_BATCH_CANCELLATION_SAFETY_UNAVAILABLE,
                "Batch cancellation is disabled until booking, refund, notification, and compensation safety can be verified");
    }

    private AdminShowtimeResponse withPricing(AdminShowtimeResponse response, PriceResolutionResult pricing) {
        response.setPricingStatus(pricing.isComplete() ? "COMPLETE" : "INCOMPLETE");
        response.setMissingPriceSeatTypeIds(pricing.missingSeatTypes().stream()
                .map(PriceResolutionResult.SeatTypeDiagnostic::seatTypeId)
                .toList());
        response.setAmbiguousPriceSeatTypeIds(pricing.ambiguousSeatTypes().stream()
                .map(PriceResolutionResult.SeatTypeDiagnostic::seatTypeId)
                .toList());
        return response;
    }

    private AdminShowtimeResponse withPricing(AdminShowtimeResponse response, ShowtimePricesResponse pricing) {
        response.setPricingStatus(pricing.isComplete() ? "COMPLETE" : "INCOMPLETE");
        response.setMissingPriceSeatTypeIds(pricing.getMissingSeatTypes().stream()
                .map(item -> item.seatTypeId())
                .toList());
        response.setAmbiguousPriceSeatTypeIds(pricing.getAmbiguousSeatTypes().stream()
                .map(item -> item.seatTypeId())
                .toList());
        return response;
    }
}
