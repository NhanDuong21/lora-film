package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeStatusHistory;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.response.ShowtimeStatusHistoryResponse;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeStatusHistoryRepository;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShowtimeStatusHistoryServiceImpl implements ShowtimeStatusHistoryService {

    private final ShowtimeStatusHistoryRepository statusHistoryRepository;
    private final ShowtimeRepository showtimeRepository;

    public ShowtimeStatusHistoryServiceImpl(ShowtimeStatusHistoryRepository statusHistoryRepository, ShowtimeRepository showtimeRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordInitialHistory(Showtime showtime, Long changedBy) {
        ShowtimeStatusHistory history = new ShowtimeStatusHistory();
        history.setShowtime(showtime);
        history.setPreviousStatus(null);
        history.setNewStatus(ShowtimeStatus.DRAFT);
        history.setReason("Showtime created");
        history.setChangedBy(changedBy);
        
        statusHistoryRepository.save(history);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordTransitionHistory(Showtime showtime, ShowtimeStatus previousStatus, ShowtimeStatus newStatus, String reason, Long changedBy, Instant changedAt) {
        ShowtimeStatusHistory history = new ShowtimeStatusHistory();
        history.setShowtime(showtime);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        history.setChangedBy(changedBy);
        history.setChangedAt(changedAt);
        
        statusHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeStatusHistoryResponse> getShowtimeStatusHistory(String showtimePublicId) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        List<ShowtimeStatusHistory> histories = statusHistoryRepository.findByShowtimeIdOrderByChangedAtAscIdAsc(showtime.getId());

        return histories.stream().map(history -> {
            ShowtimeStatusHistoryResponse response = new ShowtimeStatusHistoryResponse();
            response.setPreviousStatus(history.getPreviousStatus() != null ? history.getPreviousStatus().name() : null);
            response.setNewStatus(history.getNewStatus() != null ? history.getNewStatus().name() : null);
            response.setReason(history.getReason());
            response.setChangedAt(history.getChangedAt());
            response.setChangedBy(history.getChangedBy());
            return response;
        }).collect(Collectors.toList());
    }
}
