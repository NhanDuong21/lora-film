package com.lorafilm.movie.cinema.scheduler;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class CinemaStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(CinemaStatusScheduler.class);

    private final CinemaRepository cinemaRepository;

    public CinemaStatusScheduler(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkAndTransitionCinemaStatuses() {
        Instant now = Instant.now();
        log.debug("Running scheduled task to update cinema statuses at {}", now);

        // 1. Transition ACTIVE cinemas with active closure periods to TEMPORARILY_CLOSED
        List<Cinema> toClose = cinemaRepository.findCinemasToClose(now);
        if (!toClose.isEmpty()) {
            for (Cinema cinema : toClose) {
                cinema.setStatus(CinemaStatus.TEMPORARILY_CLOSED);
                log.info("Transitioned cinema {} (ID: {}) to TEMPORARILY_CLOSED due to active closure period", cinema.getName(), cinema.getId());
            }
            cinemaRepository.saveAll(toClose);
        }

        // 2. Transition TEMPORARILY_CLOSED cinemas with no active closure periods back to ACTIVE
        List<Cinema> toOpen = cinemaRepository.findCinemasToOpen(now);
        if (!toOpen.isEmpty()) {
            for (Cinema cinema : toOpen) {
                cinema.setStatus(CinemaStatus.ACTIVE);
                log.info("Transitioned cinema {} (ID: {}) back to ACTIVE as closure period ended", cinema.getName(), cinema.getId());
            }
            cinemaRepository.saveAll(toOpen);
        }
    }
}
