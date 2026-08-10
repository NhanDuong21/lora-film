package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleAuditoriumLockService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AutoScheduleAuditoriumLockServiceImpl implements AutoScheduleAuditoriumLockService {

    private final AuditoriumRepository auditoriumRepository;

    public AutoScheduleAuditoriumLockServiceImpl(AuditoriumRepository auditoriumRepository) {
        this.auditoriumRepository = auditoriumRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Auditorium> lockAll(List<Long> auditoriumIds) {
        if (auditoriumIds == null || auditoriumIds.isEmpty()) {
            return List.of();
        }

        // Sort to avoid deadlocks
        List<Long> sortedIds = auditoriumIds.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        try {
            List<Auditorium> lockedAuditoriums = new java.util.ArrayList<>();
            for (Long id : sortedIds) {
                Auditorium auditorium = auditoriumRepository.findByIdForScheduling(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND, "Auditorium " + id + " not found or deleted"));
                lockedAuditoriums.add(auditorium);
            }
            
            return lockedAuditoriums;
        } catch (org.springframework.dao.CannotAcquireLockException | org.hibernate.exception.LockAcquisitionException e) {
            org.slf4j.LoggerFactory.getLogger(AutoScheduleAuditoriumLockServiceImpl.class)
                    .error("Failed to acquire auditorium scheduling locks", e);
            throw new BusinessException(ErrorCode.SHOWTIME_SCHEDULING_CONFLICT);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AutoScheduleAuditoriumLockServiceImpl.class)
                    .error("Unexpected error acquiring auditorium locks", e);
            throw new BusinessException(ErrorCode.SHOWTIME_SCHEDULING_CONFLICT);
        }
    }
}
