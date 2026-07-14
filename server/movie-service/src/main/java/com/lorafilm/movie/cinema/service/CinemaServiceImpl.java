package com.lorafilm.movie.cinema.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaMedia;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.cinema.dto.CinemaMapper;
import com.lorafilm.movie.cinema.repository.CinemaMediaRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.cinema.repository.CinemaSpecification;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;

@Service
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaOperatingHourRepository cinemaOperatingHourRepository;
    private final CinemaMediaRepository cinemaMediaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final CinemaMapper cinemaMapper;

    public CinemaServiceImpl(CinemaRepository cinemaRepository,
                             CinemaOperatingHourRepository cinemaOperatingHourRepository,
                             CinemaMediaRepository cinemaMediaRepository,
                             AuditoriumRepository auditoriumRepository,
                             CinemaMapper cinemaMapper) {
        this.cinemaRepository = cinemaRepository;
        this.cinemaOperatingHourRepository = cinemaOperatingHourRepository;
        this.cinemaMediaRepository = cinemaMediaRepository;
        this.auditoriumRepository = auditoriumRepository;
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
    public CinemaDetailDto getCinemaBySlug(String slug) {
        Cinema cinema = cinemaRepository.findByActiveSlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        if (cinema.getStatus() != CinemaStatus.ACTIVE) {
            throw new ResourceNotFoundException("Cinema not found");
        }

        return mapToDetailDto(cinema);
    }

    private CinemaDetailDto mapToDetailDto(Cinema cinema) {
        CinemaDto baseDto = cinemaMapper.toDto(cinema);
        CinemaDetailDto detailDto = new CinemaDetailDto();
        detailDto.setPublicId(baseDto.getPublicId());
        detailDto.setName(baseDto.getName());
        detailDto.setSlug(baseDto.getSlug());
        detailDto.setCity(baseDto.getCity());
        detailDto.setDistrict(baseDto.getDistrict());
        detailDto.setAddress(baseDto.getAddress());
        detailDto.setHotline(baseDto.getHotline());
        detailDto.setLatitude(baseDto.getLatitude());
        detailDto.setLongitude(baseDto.getLongitude());
        detailDto.setTimezone(baseDto.getTimezone());

        List<CinemaOperatingHour> operatingHours = cinemaOperatingHourRepository.findByCinemaId(cinema.getId());
        detailDto.setOperatingHours(operatingHours.stream().map(h -> {
            CinemaDetailDto.OperatingHourDto dto = new CinemaDetailDto.OperatingHourDto();
            dto.setDayOfWeek(h.getDayOfWeek());
            dto.setOpenTime(h.getOpenTime() != null ? h.getOpenTime().toString() : null);
            dto.setCloseTime(h.getCloseTime() != null ? h.getCloseTime().toString() : null);
            dto.setIsClosed(h.getIsClosed());
            return dto;
        }).collect(Collectors.toList()));

        List<CinemaMedia> media = cinemaMediaRepository.findByCinemaIdAndStatusAndDeletedAtIsNullOrderByDisplayOrderAsc(cinema.getId(), ActiveStatus.ACTIVE);
        detailDto.setGallery(media.stream().map(m -> {
            CinemaDetailDto.CinemaMediaDto dto = new CinemaDetailDto.CinemaMediaDto();
            dto.setPublicId(m.getPublicId());
            dto.setMediaType(m.getMediaType().name());
            dto.setUrl(m.getUrl());
            dto.setTitle(m.getTitle());
            dto.setIsPrimary(m.getIsPrimary());
            return dto;
        }).collect(Collectors.toList()));

        List<Auditorium> auditoriums = auditoriumRepository.findByCinemaIdAndStatusAndDeletedAtIsNull(cinema.getId(), AuditoriumStatus.ACTIVE);
        detailDto.setActiveAuditoriums(auditoriums.stream().map(a -> {
            CinemaDetailDto.AuditoriumDto dto = new CinemaDetailDto.AuditoriumDto();
            dto.setPublicId(a.getPublicId());
            dto.setName(a.getName());
            dto.setScreenType(a.getScreenType() != null ? a.getScreenType().name() : null);
            dto.setSoundType(a.getSoundType() != null ? a.getSoundType().name() : null);
            dto.setCapacity(a.getCapacity());
            return dto;
        }).collect(Collectors.toList()));

        return detailDto;
    }
}
