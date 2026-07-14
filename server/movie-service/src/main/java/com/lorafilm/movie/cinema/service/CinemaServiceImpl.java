package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.CinemaMapper;
import com.lorafilm.movie.cinema.dto.CreateCinemaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaRequest;
import com.lorafilm.movie.cinema.dto.CinemaResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaMediaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaMediaRequest;
import com.lorafilm.movie.cinema.dto.CinemaMediaResponse;
import com.lorafilm.movie.cinema.dto.OperatingHourUpdateRequest;
import com.lorafilm.movie.cinema.dto.OperatingHourResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaClosurePeriodRequest;
import com.lorafilm.movie.cinema.dto.CinemaClosurePeriodResponse;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.cinema.repository.CinemaSpecification;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.cinema.repository.CinemaMediaRepository;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.domain.entity.CinemaMedia;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import java.time.Instant;
import java.time.LocalTime;
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
    private final CinemaClosurePeriodRepository cinemaClosurePeriodRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditoriumRepository auditoriumRepository;
    private final CinemaMapper cinemaMapper;

    public CinemaServiceImpl(CinemaRepository cinemaRepository,
            CinemaOperatingHourRepository cinemaOperatingHourRepository,
            CinemaMediaRepository cinemaMediaRepository,
            CinemaClosurePeriodRepository cinemaClosurePeriodRepository,
            CurrentUserProvider currentUserProvider,
            AuditoriumRepository auditoriumRepository,
            CinemaMapper cinemaMapper) {
        this.cinemaRepository = cinemaRepository;
        this.cinemaOperatingHourRepository = cinemaOperatingHourRepository;
        this.cinemaMediaRepository = cinemaMediaRepository;
        this.cinemaClosurePeriodRepository = cinemaClosurePeriodRepository;
        this.currentUserProvider = currentUserProvider;
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
                cinemaPage.isLast());
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
            dto.setOpenTime(Boolean.TRUE.equals(h.getIsClosed()) ? null
                    : (h.getOpenTime() != null ? h.getOpenTime().toString() : null));
            dto.setCloseTime(Boolean.TRUE.equals(h.getIsClosed()) ? null
                    : (h.getCloseTime() != null ? h.getCloseTime().toString() : null));
            dto.setIsClosed(h.getIsClosed());
            return dto;
        }).collect(Collectors.toList()));

        List<CinemaMedia> media = cinemaMediaRepository
                .findByCinemaIdAndStatusAndDeletedAtIsNullOrderByDisplayOrderAsc(cinema.getId(), ActiveStatus.ACTIVE);
        detailDto.setGallery(media.stream().map(m -> {
            CinemaDetailDto.CinemaMediaDto dto = new CinemaDetailDto.CinemaMediaDto();
            dto.setPublicId(m.getPublicId());
            dto.setMediaType(m.getMediaType().name());
            dto.setUrl(m.getUrl());
            dto.setTitle(m.getTitle());
            dto.setIsPrimary(m.getIsPrimary());
            return dto;
        }).collect(Collectors.toList()));

        List<Auditorium> auditoriums = auditoriumRepository.findByCinemaIdAndStatusAndDeletedAtIsNull(cinema.getId(),
                com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE);
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

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CinemaDetailDto.CinemaMediaDto> getCinemaMedia(String cinemaPublicId) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        List<CinemaMedia> mediaList = cinemaMediaRepository
                .findByCinemaIdAndStatusAndDeletedAtIsNullOrderByDisplayOrderAsc(cinema.getId(), ActiveStatus.ACTIVE);
        return mediaList.stream().map(m -> {
            CinemaDetailDto.CinemaMediaDto dto = new CinemaDetailDto.CinemaMediaDto();
            dto.setPublicId(m.getPublicId());
            dto.setMediaType(m.getMediaType().name());
            dto.setUrl(m.getUrl());
            dto.setTitle(m.getTitle());
            dto.setIsPrimary(m.getIsPrimary());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CinemaDetailDto.OperatingHourDto> getCinemaOperatingHours(String cinemaPublicId) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        List<CinemaOperatingHour> hours = cinemaOperatingHourRepository
                .findByCinemaIdOrderByDayOfWeekAsc(cinema.getId());
        return hours.stream().map(h -> {
            CinemaDetailDto.OperatingHourDto dto = new CinemaDetailDto.OperatingHourDto();
            dto.setDayOfWeek(h.getDayOfWeek());
            dto.setOpenTime(Boolean.TRUE.equals(h.getIsClosed()) ? null
                    : (h.getOpenTime() != null ? h.getOpenTime().toString() : null));
            dto.setCloseTime(Boolean.TRUE.equals(h.getIsClosed()) ? null
                    : (h.getCloseTime() != null ? h.getCloseTime().toString() : null));
            dto.setIsClosed(h.getIsClosed());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CinemaMediaResponse addCinemaMedia(String cinemaPublicId, CreateCinemaMediaRequest request) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            List<CinemaMedia> existingPrimary = cinemaMediaRepository
                    .findByCinemaIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                            cinema.getId(), request.getMediaType(), ActiveStatus.ACTIVE);
            for (CinemaMedia m : existingPrimary) {
                m.setIsPrimary(false);
                cinemaMediaRepository.save(m);
            }
        }

        CinemaMedia media = new CinemaMedia();
        media.setPublicId(java.util.UUID.randomUUID().toString());
        media.setCinema(cinema);
        media.setMediaType(request.getMediaType());
        media.setUrl(request.getUrl());
        media.setTitle(request.getTitle());
        media.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        media.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()));
        media.setStatus(ActiveStatus.ACTIVE);

        CinemaMedia savedMedia = cinemaMediaRepository.save(media);
        return cinemaMapper.toMediaResponse(savedMedia);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CinemaMediaResponse updateCinemaMedia(String mediaPublicId, UpdateCinemaMediaRequest request) {
        CinemaMedia media = cinemaMediaRepository.findByPublicIdAndDeletedAtIsNull(mediaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema media not found"));

        if (Boolean.TRUE.equals(request.getIsPrimary()) && request.getStatus() == ActiveStatus.ACTIVE) {
            List<CinemaMedia> existingPrimary = cinemaMediaRepository
                    .findByCinemaIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                            media.getCinema().getId(), media.getMediaType(), ActiveStatus.ACTIVE);
            for (CinemaMedia m : existingPrimary) {
                if (!m.getId().equals(media.getId())) {
                    m.setIsPrimary(false);
                    cinemaMediaRepository.save(m);
                }
            }
        }

        media.setUrl(request.getUrl());
        media.setTitle(request.getTitle());
        media.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        media.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()));
        media.setStatus(request.getStatus());

        CinemaMedia savedMedia = cinemaMediaRepository.save(media);
        return cinemaMapper.toMediaResponse(savedMedia);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public List<OperatingHourResponse> updateOperatingHours(String cinemaPublicId,
            List<OperatingHourUpdateRequest> requests) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        if (requests == null || requests.size() != 7) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Must provide operating hours for exactly 7 days");
        }

        java.util.Set<Integer> days = new java.util.HashSet<>();
        for (OperatingHourUpdateRequest req : requests) {
            if (req.getDayOfWeek() < 1 || req.getDayOfWeek() > 7) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Day of week must be between 1 and 7");
            }
            if (!days.add(req.getDayOfWeek())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate day of week: " + req.getDayOfWeek());
            }
            if (Boolean.FALSE.equals(req.getIsClosed())) {
                if (req.getOpenTime() == null || req.getCloseTime() == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Open time and close time must not be null for open days");
                }
                if (!req.getOpenTime().isBefore(req.getCloseTime())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Open time must be before close time");
                }
            }
        }

        Long userId = currentUserProvider.getCurrentUserId();
        List<CinemaOperatingHour> existingHours = cinemaOperatingHourRepository.findByCinemaId(cinema.getId());
        java.util.Map<Integer, CinemaOperatingHour> hoursMap = existingHours.stream()
                .collect(Collectors.toMap(CinemaOperatingHour::getDayOfWeek, h -> h));

        java.util.List<CinemaOperatingHour> toSave = new java.util.ArrayList<>();
        for (OperatingHourUpdateRequest req : requests) {
            CinemaOperatingHour hour = hoursMap.get(req.getDayOfWeek());
            if (hour == null) {
                hour = new CinemaOperatingHour();
                hour.setCinema(cinema);
                hour.setDayOfWeek(req.getDayOfWeek());
                hour.setCreatedBy(userId);
            }
            hour.setOpenTime(Boolean.TRUE.equals(req.getIsClosed()) ? LocalTime.of(0, 0) : req.getOpenTime());
            hour.setCloseTime(Boolean.TRUE.equals(req.getIsClosed()) ? LocalTime.of(23, 59) : req.getCloseTime());
            hour.setIsClosed(req.getIsClosed());
            hour.setUpdatedBy(userId);
            toSave.add(hour);
        }

        List<CinemaOperatingHour> savedHours = cinemaOperatingHourRepository.saveAll(toSave);
        return savedHours.stream()
                .sorted(java.util.Comparator.comparing(CinemaOperatingHour::getDayOfWeek))
                .map(cinemaMapper::toOperatingHourResponse)
                .collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CinemaClosurePeriodResponse createClosurePeriod(String cinemaPublicId,
            CreateCinemaClosurePeriodRequest request) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "End time must be after start time");
        }
        if (request.getStartTime().isBefore(Instant.now().minusSeconds(10))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Start time must be in the future");
        }

        // Check overlapping active closures
        List<CinemaClosurePeriod> overlapping = cinemaClosurePeriodRepository.findOverlappingClosures(
                cinema.getId(), request.getStartTime(), request.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new BusinessException(ErrorCode.CINEMA_CLOSURE_CONFLICT);
        }

        Long userId = currentUserProvider.getCurrentUserId();
        CinemaClosurePeriod period = new CinemaClosurePeriod();
        period.setCinema(cinema);
        period.setStartTime(request.getStartTime());
        period.setEndTime(request.getEndTime());
        period.setReason(request.getReason());
        period.setStatus(ActionStatus.ACTIVE);
        period.setCreatedBy(userId);
        period.setUpdatedBy(userId);

        CinemaClosurePeriod savedPeriod = cinemaClosurePeriodRepository.save(period);
        return cinemaMapper.toClosurePeriodResponse(savedPeriod);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CinemaClosurePeriodResponse cancelClosurePeriod(Long closurePeriodId) {
        CinemaClosurePeriod period = cinemaClosurePeriodRepository.findById(closurePeriodId)
                .orElseThrow(() -> new ResourceNotFoundException("Closure period not found"));

        if (period.getStatus() == ActionStatus.CANCELLED) {
            throw new ResourceNotFoundException("Closure period not found or already cancelled");
        }

        Long userId = currentUserProvider.getCurrentUserId();
        period.setStatus(ActionStatus.CANCELLED);
        period.setUpdatedBy(userId);
        CinemaClosurePeriod savedPeriod = cinemaClosurePeriodRepository.save(period);
        return cinemaMapper.toClosurePeriodResponse(savedPeriod);
    }
}
