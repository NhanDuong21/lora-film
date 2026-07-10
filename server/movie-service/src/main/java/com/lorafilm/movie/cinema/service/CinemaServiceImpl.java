package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.cinema.dto.CinemaMapper;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.cinema.repository.CinemaSpecification;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaMapper cinemaMapper;

    public CinemaServiceImpl(CinemaRepository cinemaRepository, CinemaMapper cinemaMapper) {
        this.cinemaRepository = cinemaRepository;
        this.cinemaMapper = cinemaMapper;
    }

    @Override
    public PageResponse<CinemaDto> getCinemas(String city, String district, String keyword, int page, int size) {
        Specification<Cinema> spec = Specification.where(CinemaSpecification.isNotDeleted())
                .and(CinemaSpecification.hasStatus(CinemaStatus.ACTIVE));

        if (city != null && !city.isEmpty()) {
            spec = spec.and(CinemaSpecification.hasCity(city));
        }
        if (district != null && !district.isEmpty()) {
            spec = spec.and(CinemaSpecification.hasDistrict(district));
        }
        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and(CinemaSpecification.hasKeyword(keyword));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Cinema> cinemaPage = cinemaRepository.findAll(spec, pageable);

        List<CinemaDto> cinemaDtos = cinemaPage.getContent().stream()
                .map(cinemaMapper::toDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                cinemaDtos,
                cinemaPage.getNumber(),
                cinemaPage.getSize(),
                cinemaPage.getTotalElements(),
                cinemaPage.getTotalPages(),
                cinemaPage.isLast()
        );
    }

    @Override
    public CinemaDto getCinemaBySlug(String slug) {
        Cinema cinema = cinemaRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        if (cinema.getStatus() != CinemaStatus.ACTIVE) {
            throw new ResourceNotFoundException("Cinema not found");
        }

        return cinemaMapper.toDto(cinema);
    }
}
