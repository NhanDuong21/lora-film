package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.service.AutoScheduleShowtimeCreationService;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AutoScheduleShowtimeCreationServiceImpl implements AutoScheduleShowtimeCreationService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeStatusHistoryService showtimeStatusHistoryService;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final ShowtimePriceRepository showtimePriceRepository;

    public AutoScheduleShowtimeCreationServiceImpl(ShowtimeRepository showtimeRepository,
                                                   ShowtimeStatusHistoryService showtimeStatusHistoryService,
                                                   SeatRepository seatRepository,
                                                   SeatTypeRepository seatTypeRepository,
                                                   ShowtimePriceRepository showtimePriceRepository) {
        this.showtimeRepository = showtimeRepository;
        this.showtimeStatusHistoryService = showtimeStatusHistoryService;
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.showtimePriceRepository = showtimePriceRepository;
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

        List<ShowtimePrice> defaultPrices = new ArrayList<>();

        // Record history and associate back to preview items
        for (int i = 0; i < items.size(); i++) {
            Showtime savedShowtime = showtimes.get(i);
            ShowtimeSchedulePreviewItem item = items.get(i);
            
            showtimeStatusHistoryService.recordInitialHistory(savedShowtime, actorId);
            
            // Generate default prices
            List<String> requiredSeatTypeIds = seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(savedShowtime.getAuditorium().getId());
            List<SeatType> seatTypes = seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(requiredSeatTypeIds);
            
            for (SeatType seatType : seatTypes) {
                ShowtimePrice price = new ShowtimePrice();
                price.setShowtime(savedShowtime);
                price.setSeatType(seatType);
                price.setCurrency("VND");
                
                BigDecimal amount;
                if (seatType.getCode() == SeatTypeCode.VIP) {
                    amount = new BigDecimal("90000");
                } else if (seatType.getCode() == SeatTypeCode.COUPLE) {
                    amount = new BigDecimal("120000");
                } else {
                    amount = new BigDecimal("75000");
                }
                price.setPrice(amount);
                defaultPrices.add(price);
            }
        }

        if (!defaultPrices.isEmpty()) {
            showtimePriceRepository.saveAll(defaultPrices);
        }

        return showtimes;
    }
}
