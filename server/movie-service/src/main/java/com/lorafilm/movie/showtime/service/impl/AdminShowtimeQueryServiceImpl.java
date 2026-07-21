package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminShowtimeQueryServiceImpl implements AdminShowtimeQueryService {

    private final ShowtimeRepository showtimeRepository;
    private final AdminShowtimeMapper adminShowtimeMapper;

    public AdminShowtimeQueryServiceImpl(ShowtimeRepository showtimeRepository, AdminShowtimeMapper adminShowtimeMapper) {
        this.showtimeRepository = showtimeRepository;
        this.adminShowtimeMapper = adminShowtimeMapper;
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

        List<AdminShowtimeResponse> responses = showtimePage.getContent().stream()
                .map(adminShowtimeMapper::toAdminResponse)
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
        return adminShowtimeMapper.toAdminResponse(showtime);
    }
}
