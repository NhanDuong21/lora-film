package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeStatusHistory;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeStatusHistoryRepository;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class ShowtimeStatusHistoryServiceImpl implements ShowtimeStatusHistoryService {

    private final ShowtimeStatusHistoryRepository statusHistoryRepository;

    public ShowtimeStatusHistoryServiceImpl(ShowtimeStatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
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
}
