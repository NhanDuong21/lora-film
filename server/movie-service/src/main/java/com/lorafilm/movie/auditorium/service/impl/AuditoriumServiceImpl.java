package com.lorafilm.movie.auditorium.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest;

import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.AuditoriumService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditoriumServiceImpl implements AuditoriumService {

    private final AuditoriumRepository auditoriumRepository;
    private final CinemaRepository cinemaRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CurrentUserProvider currentUserProvider;

    public AuditoriumServiceImpl(AuditoriumRepository auditoriumRepository, CinemaRepository cinemaRepository, SeatRepository seatRepository, ShowtimeRepository showtimeRepository, CurrentUserProvider currentUserProvider) {
        this.auditoriumRepository = auditoriumRepository;
        this.cinemaRepository = cinemaRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public AuditoriumResponse createAuditorium(String cinemaPublicId, CreateAuditoriumRequest request) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CINEMA_NOT_FOUND));

        if (cinema.getStatus() == CinemaStatus.INACTIVE || cinema.getStatus() == CinemaStatus.PERMANENTLY_CLOSED) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_CONFIGURABLE);
        }

        if (auditoriumRepository.existsByCinemaIdAndNameIgnoreCaseAndDeletedAtIsNull(cinema.getId(), request.name().trim())) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NAME_DUPLICATED);
        }

        Auditorium auditorium = new Auditorium();
        auditorium.setPublicId(UUID.randomUUID().toString());
        auditorium.setCinema(cinema);
        auditorium.setName(request.name().trim());
        auditorium.setScreenType(request.screenType());
        auditorium.setSoundType(request.soundType());
        auditorium.setCapacity(request.capacity());
        auditorium.setCleaningBufferMinutes(request.cleaningBufferMinutes());
        auditorium.setStatus(AuditoriumStatus.DRAFT);
        
        auditorium = auditoriumRepository.save(auditorium);
        return mapToResponse(auditorium);
    }

    @Override
    @Transactional
    public AuditoriumResponse updateAuditorium(String auditoriumPublicId, UpdateAuditoriumRequest request) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        if (auditoriumRepository.existsByCinemaIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
                auditorium.getCinema().getId(), request.name().trim(), auditorium.getId())) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NAME_DUPLICATED);
        }
        
        long activeSeatCount = seatRepository.countByAuditoriumIdAndDeletedAtIsNull(auditorium.getId());
        if (request.capacity() < activeSeatCount) {
            throw new BusinessException(ErrorCode.AUDITORIUM_CAPACITY_BELOW_CURRENT_SEAT_COUNT);
        }
        validateStatusTransition(auditorium.getStatus(), request.status(), auditorium);

        auditorium.setName(request.name().trim());
        auditorium.setScreenType(request.screenType());
        auditorium.setSoundType(request.soundType());
        auditorium.setCapacity(request.capacity());
        auditorium.setCleaningBufferMinutes(request.cleaningBufferMinutes());
        auditorium.setStatus(request.status());
        
        return mapToResponse(auditorium);
    }



    @Override
    @Transactional
    public void deleteAuditorium(String auditoriumPublicId) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        if (showtimeRepository.existsByAuditoriumId(auditorium.getId())) {
            throw new BusinessException(ErrorCode.AUDITORIUM_CANNOT_BE_DELETED_HAS_SHOWTIME_HISTORY);
        }
        
        long activeSeatCount = seatRepository.countByAuditoriumIdAndDeletedAtIsNull(auditorium.getId());
        if (activeSeatCount > 0) {
            throw new BusinessException(ErrorCode.AUDITORIUM_HAS_ACTIVE_SEATS);
        }
        
        auditorium.performSoftDelete(currentUserProvider.getCurrentUserId());
    }
    
    private void validateStatusTransition(AuditoriumStatus current, AuditoriumStatus target, Auditorium auditorium) {
        if (current == target) return;
        
        boolean valid = switch(current) {
            case DRAFT -> target == AuditoriumStatus.ACTIVE || target == AuditoriumStatus.INACTIVE;
            case ACTIVE -> target == AuditoriumStatus.MAINTENANCE || target == AuditoriumStatus.INACTIVE;
            case MAINTENANCE -> target == AuditoriumStatus.ACTIVE || target == AuditoriumStatus.INACTIVE;
            case INACTIVE -> target == AuditoriumStatus.DRAFT || target == AuditoriumStatus.ACTIVE;
        };
        
        if (!valid) throw new BusinessException(ErrorCode.INVALID_AUDITORIUM_STATUS_TRANSITION);
        
        if (target == AuditoriumStatus.ACTIVE) {
            CinemaStatus cs = auditorium.getCinema().getStatus();
            if (cs == CinemaStatus.INACTIVE || cs == CinemaStatus.PERMANENTLY_CLOSED) {
                throw new BusinessException(ErrorCode.CINEMA_NOT_CONFIGURABLE);
            }
        }
    }

    private AuditoriumResponse mapToResponse(Auditorium a) {
        return new AuditoriumResponse(
            a.getPublicId(),
            a.getCinema().getPublicId(),
            a.getCinema().getName(),
            a.getName(),
            a.getScreenType(),
            a.getSoundType(),
            a.getCapacity(),
            a.getCleaningBufferMinutes(),
            a.getStatus(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }
}
