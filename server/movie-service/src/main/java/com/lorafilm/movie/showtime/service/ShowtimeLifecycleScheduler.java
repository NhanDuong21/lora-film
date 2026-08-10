package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class ShowtimeLifecycleScheduler {
    private static final Logger log = LoggerFactory.getLogger(ShowtimeLifecycleScheduler.class);
    private static final String AUTO_CLOSE_REASON = "Automatically closed when the showtime started";
    private static final String AUTO_FINISH_REASON = "Automatically finished when the showtime ended";

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeStatusHistoryService historyService;
    private final Clock clock;

    public ShowtimeLifecycleScheduler(ShowtimeRepository showtimeRepository,
                                      ShowtimeStatusHistoryService historyService,
                                      Clock clock) {
        this.showtimeRepository = showtimeRepository;
        this.historyService = historyService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${showtime.lifecycle.reconcile-delay-ms:60000}")
    @Transactional
    public void reconcile() {
        Instant now = Instant.now(clock);
        List<Showtime> toClose = showtimeRepository
                .findByStatusAndStartTimeLessThanEqualAndDeletedAtIsNullOrderByIdAsc(
                        ShowtimeStatus.OPEN_FOR_BOOKING, now);
        for (Showtime showtime : toClose) {
            if (showtime.getBookingCloseTime() == null) {
                showtime.setBookingCloseTime(now);
            }
            showtime.setStatus(ShowtimeStatus.CLOSED);
            Showtime saved = showtimeRepository.save(showtime);
            historyService.recordTransitionHistory(
                    saved, ShowtimeStatus.OPEN_FOR_BOOKING, ShowtimeStatus.CLOSED,
                    AUTO_CLOSE_REASON, null, now);
        }

        List<Showtime> toFinish = showtimeRepository
                .findByStatusAndEndTimeLessThanEqualAndDeletedAtIsNullOrderByIdAsc(
                        ShowtimeStatus.CLOSED, now);
        for (Showtime showtime : toFinish) {
            showtime.setStatus(ShowtimeStatus.FINISHED);
            Showtime saved = showtimeRepository.save(showtime);
            historyService.recordTransitionHistory(
                    saved, ShowtimeStatus.CLOSED, ShowtimeStatus.FINISHED,
                    AUTO_FINISH_REASON, null, now);
        }

        if (!toClose.isEmpty() || !toFinish.isEmpty()) {
            log.info("Showtime lifecycle reconciled: closed={}, finished={}",
                    toClose.size(), toFinish.size());
        }
    }
}
