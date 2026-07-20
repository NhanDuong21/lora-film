package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.service.AutoScheduleShowtimeCreationService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AutoScheduleShowtimeCreationServiceImpl implements AutoScheduleShowtimeCreationService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeStatusHistoryService showtimeStatusHistoryService;

    public AutoScheduleShowtimeCreationServiceImpl(ShowtimeRepository showtimeRepository,
                                                   ShowtimeStatusHistoryService showtimeStatusHistoryService) {
        this.showtimeRepository = showtimeRepository;
        this.showtimeStatusHistoryService = showtimeStatusHistoryService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Showtime> createAll(List<ShowtimeSchedulePreviewItem> items, Long actorId, String batchId) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<Showtime> showtimes = new ArrayList<>();
        
        for (ShowtimeSchedulePreviewItem item : items) {
            Showtime showtime = new Showtime();
            showtime.setPublicId(UUID.randomUUID().toString());
            showtime.setMovie(item.getMovie());
            showtime.setMovieVersion(item.getMovieVersion());
            showtime.setCinema(item.getCinema());
            showtime.setAuditorium(item.getAuditorium());
            showtime.setStartTime(item.getStartTime());
            showtime.setEndTime(item.getEndTime());
            showtime.setStatus(ShowtimeStatus.DRAFT);
            showtime.setCancellationReason(null);
            showtime.setBookingOpenTime(null);
            showtime.setBookingCloseTime(null);
            showtime.setBatchId(batchId);
            showtime.setSource(ShowtimeSource.AUTO);
            
            showtimes.add(showtime);
        }

        // Batch save
        showtimes = showtimeRepository.saveAll(showtimes);
        showtimeRepository.flush(); // Ensure IDs are generated

        // Record history and associate back to preview items
        for (int i = 0; i < items.size(); i++) {
            Showtime savedShowtime = showtimes.get(i);
            ShowtimeSchedulePreviewItem item = items.get(i);
            
            showtimeStatusHistoryService.recordInitialHistory(savedShowtime, actorId);
            
            // Note: If recordInitialHistory doesn't allow custom reason, we just use the default.
            // In the codebase it uses "Created" as reason for initial draft.
            
            // Map created showtime back to item (in-memory, handled by JPA later)
            // But we shouldn't use a setter directly if we are not modifying the item here,
            // actually we MUST set it so the orchestration can update the item status!
            // We will let orchestration handle the preview item update, we just return the showtimes.
        }

        return showtimes;
    }
}
