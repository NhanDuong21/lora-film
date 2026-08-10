package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeMapper;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeSpecification;
import com.lorafilm.movie.showtime.service.AdminShowtimeQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminShowtimeQueryServiceImpl implements AdminShowtimeQueryService {

    private final ShowtimeRepository showtimeRepository;
    private final AdminShowtimeMapper adminShowtimeMapper;
    private final MovieMediaRepository movieMediaRepository;

    public AdminShowtimeQueryServiceImpl(
            ShowtimeRepository showtimeRepository,
            AdminShowtimeMapper adminShowtimeMapper,
            MovieMediaRepository movieMediaRepository) {
        this.showtimeRepository = showtimeRepository;
        this.adminShowtimeMapper = adminShowtimeMapper;
        this.movieMediaRepository = movieMediaRepository;
    }

    @Override
    public PageResponse<AdminShowtimeResponse> getAdminShowtimes(
            String cinemaSlug,
            String movieSlug,
            ShowtimeStatus status,
            LocalDate date,
            String batchId,
            ShowtimeSource source,
            int page,
            int size) {

        Specification<Showtime> spec = Specification.where(ShowtimeSpecification.isNotDeleted());

        if (cinemaSlug != null && !cinemaSlug.isEmpty()) {
            spec = spec.and(ShowtimeSpecification.hasCinemaSlug(cinemaSlug));
        }
        if (movieSlug != null && !movieSlug.isEmpty()) {
            spec = spec.and(ShowtimeSpecification.hasMovieSlug(movieSlug));
        }
        if (status != null) {
            spec = spec.and(ShowtimeSpecification.hasStatus(status));
        }
        
        if (date != null) {
            spec = spec.and(ShowtimeSpecification.hasDate(date));
        }
        
        if (batchId != null && !batchId.isEmpty()) {
            spec = spec.and(ShowtimeSpecification.hasBatchId(batchId));
        }
        if (source != null) {
            spec = spec.and(ShowtimeSpecification.hasSource(source));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<Showtime> showtimePage = showtimeRepository.findAll(spec, pageable);

        Map<Long, String> primaryPosters = loadPrimaryPosters(showtimePage.getContent());
        List<AdminShowtimeResponse> responses = showtimePage.getContent().stream()
                .map(showtime -> toResponse(showtime, primaryPosters))
                .collect(Collectors.toList());

        return new PageResponse<>(
                responses,
                showtimePage.getNumber(),
                showtimePage.getSize(),
                showtimePage.getTotalElements(),
                showtimePage.getTotalPages(),
                showtimePage.isLast()
        );
    }

    @Override
    public AdminShowtimeResponse getAdminShowtimeByPublicId(String publicId) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        return toResponse(showtime, loadPrimaryPosters(List.of(showtime)));
    }

    private Map<Long, String> loadPrimaryPosters(List<Showtime> showtimes) {
        List<Long> movieIds = showtimes.stream()
                .filter(showtime -> showtime.getMovie() != null && showtime.getMovie().getId() != null)
                .map(showtime -> showtime.getMovie().getId())
                .distinct()
                .toList();
        if (movieIds.isEmpty()) return Map.of();
        return movieMediaRepository
                .findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        movieIds, MovieMediaType.POSTER, ActiveStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        media -> media.getMovie().getId(),
                        media -> media.getUrl(),
                        (first, ignored) -> first));
    }

    private AdminShowtimeResponse toResponse(Showtime showtime, Map<Long, String> primaryPosters) {
        AdminShowtimeResponse response = adminShowtimeMapper.toAdminResponse(showtime);
        if (response != null && response.getMovie() != null && showtime.getMovie() != null) {
            response.getMovie().setPosterUrl(primaryPosters.get(showtime.getMovie().getId()));
        }
        return response;
    }
}
