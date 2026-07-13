package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.CinemaMapper;
import com.lorafilm.movie.cinema.dto.CreateCinemaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaRequest;
import com.lorafilm.movie.cinema.dto.CinemaResponse;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.cinema.repository.CinemaSpecification;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.cinema.repository.CinemaMediaRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.domain.entity.CinemaMedia;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
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
        return getCinemaByIdentifier(slug);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CinemaDetailDto getCinemaByIdentifier(String identifier) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(identifier)
                .orElseGet(() -> cinemaRepository.findBySlugAndDeletedAtIsNull(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found")));

        if (cinema.getStatus() != CinemaStatus.ACTIVE) {
            throw new ResourceNotFoundException("Cinema not found");
        }

        return mapToDetailDto(cinema);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CinemaResponse createCinema(CreateCinemaRequest request) {
        // Validate timezone
        String tz = request.getTimezone();
        if (tz == null || tz.trim().isEmpty()) {
            tz = "Asia/Ho_Chi_Minh";
        } else {
            if (!java.time.ZoneId.getAvailableZoneIds().contains(tz)) {
                throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE);
            }
        }

        // Validate opened and closed date
        if (request.getOpenedDate() != null && request.getClosedDate() != null) {
            if (request.getClosedDate().isBefore(request.getOpenedDate())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Closed date must be on or after opened date");
            }
        }

        // Generate and check unique slug
        String slug = com.lorafilm.movie.cinema.util.SlugUtils.toSlug(request.getName());
        if (cinemaRepository.existsBySlugAndDeletedAtIsNull(slug)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cinema slug already exists");
        }

        Cinema cinema = new Cinema();
        cinema.setPublicId(java.util.UUID.randomUUID().toString());
        cinema.setName(request.getName());
        cinema.setSlug(slug);
        cinema.setCity(request.getCity());
        cinema.setDistrict(request.getDistrict());
        cinema.setAddress(request.getAddress());
        cinema.setLatitude(request.getLatitude());
        cinema.setLongitude(request.getLongitude());
        cinema.setTimezone(tz);
        cinema.setHotline(request.getHotline());
        cinema.setDescription(request.getDescription());
        cinema.setStatus(CinemaStatus.DRAFT);
        cinema.setOpenedDate(request.getOpenedDate());
        cinema.setClosedDate(request.getClosedDate());

        Cinema savedCinema = cinemaRepository.save(cinema);
        return cinemaMapper.toResponse(savedCinema);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CinemaResponse updateCinema(String publicId, UpdateCinemaRequest request) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        // Validate timezone
        String tz = request.getTimezone();
        if (tz == null || tz.trim().isEmpty()) {
            tz = "Asia/Ho_Chi_Minh";
        } else {
            if (!java.time.ZoneId.getAvailableZoneIds().contains(tz)) {
                throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE);
            }
        }

        // Validate opened and closed date
        if (request.getOpenedDate() != null && request.getClosedDate() != null) {
            if (request.getClosedDate().isBefore(request.getOpenedDate())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Closed date must be on or after opened date");
            }
        }

        // Generate and check unique slug
        String slug = com.lorafilm.movie.cinema.util.SlugUtils.toSlug(request.getName());
        if (cinemaRepository.existsBySlugAndPublicIdNotAndDeletedAtIsNull(slug, publicId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cinema slug already exists");
        }

        cinema.setName(request.getName());
        cinema.setSlug(slug);
        cinema.setCity(request.getCity());
        cinema.setDistrict(request.getDistrict());
        cinema.setAddress(request.getAddress());
        cinema.setLatitude(request.getLatitude());
        cinema.setLongitude(request.getLongitude());
        cinema.setTimezone(tz);
        cinema.setHotline(request.getHotline());
        cinema.setDescription(request.getDescription());
        cinema.setOpenedDate(request.getOpenedDate());
        cinema.setClosedDate(request.getClosedDate());

        Cinema savedCinema = cinemaRepository.save(cinema);
        return cinemaMapper.toResponse(savedCinema);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CinemaResponse updateCinemaStatus(String publicId, CinemaStatus targetStatus) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        CinemaStatus currentStatus = cinema.getStatus();
        if (currentStatus == targetStatus) {
            return cinemaMapper.toResponse(cinema);
        }

        boolean isValid = false;
        switch (currentStatus) {
            case DRAFT:
                isValid = (targetStatus == CinemaStatus.ACTIVE || targetStatus == CinemaStatus.INACTIVE);
                break;
            case ACTIVE:
                isValid = (targetStatus == CinemaStatus.MAINTENANCE 
                        || targetStatus == CinemaStatus.TEMPORARILY_CLOSED 
                        || targetStatus == CinemaStatus.INACTIVE 
                        || targetStatus == CinemaStatus.PERMANENTLY_CLOSED);
                break;
            case MAINTENANCE:
                isValid = (targetStatus == CinemaStatus.ACTIVE 
                        || targetStatus == CinemaStatus.TEMPORARILY_CLOSED 
                        || targetStatus == CinemaStatus.INACTIVE 
                        || targetStatus == CinemaStatus.PERMANENTLY_CLOSED);
                break;
            case TEMPORARILY_CLOSED:
                isValid = (targetStatus == CinemaStatus.ACTIVE 
                        || targetStatus == CinemaStatus.MAINTENANCE 
                        || targetStatus == CinemaStatus.INACTIVE 
                        || targetStatus == CinemaStatus.PERMANENTLY_CLOSED);
                break;
            case INACTIVE:
                isValid = (targetStatus == CinemaStatus.ACTIVE || targetStatus == CinemaStatus.PERMANENTLY_CLOSED);
                break;
            case PERMANENTLY_CLOSED:
                isValid = false; // Terminal state
                break;
        }

        if (!isValid) {
            throw new BusinessException(ErrorCode.INVALID_AUDITORIUM_STATUS_TRANSITION,
                    "Invalid cinema status transition from " + currentStatus + " to " + targetStatus);
        }

        cinema.setStatus(targetStatus);
        Cinema savedCinema = cinemaRepository.save(cinema);
        return cinemaMapper.toResponse(savedCinema);
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

        List<Auditorium> auditoriums = auditoriumRepository.findByCinemaIdAndStatusAndDeletedAtIsNull(cinema.getId(), com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE);
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
