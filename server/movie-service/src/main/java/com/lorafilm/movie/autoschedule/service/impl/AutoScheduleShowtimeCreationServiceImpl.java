package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.service.AutoScheduleShowtimeCreationService;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
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
    private final ShowtimePricingService showtimePricingService;

    public AutoScheduleShowtimeCreationServiceImpl(ShowtimeRepository showtimeRepository,
                                                   ShowtimeStatusHistoryService showtimeStatusHistoryService,
                                                   ShowtimePricingService showtimePricingService) {
        this.showtimeRepository = showtimeRepository;
        this.showtimeStatusHistoryService = showtimeStatusHistoryService;
        this.showtimePricingService = showtimePricingService;
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
            showtimeStatusHistoryService.recordInitialHistory(savedShowtime, actorId);
        }
        showtimePricingService.resolveAndReplaceAll(showtimes);

        return showtimes;
    }
}
