package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class MovieLifecycleScheduler {
    private static final Logger log = LoggerFactory.getLogger(MovieLifecycleScheduler.class);

    private final MovieRepository movieRepository;
    private final MovieApprovalPolicy approvalPolicy;
    private final MovieStatusHistoryService historyService;
    private final Clock clock;

    public MovieLifecycleScheduler(MovieRepository movieRepository,
                                   MovieApprovalPolicy approvalPolicy,
                                   MovieStatusHistoryService historyService,
                                   Clock clock) {
        this.movieRepository = movieRepository;
        this.approvalPolicy = approvalPolicy;
        this.historyService = historyService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${movie.lifecycle.reconcile-delay-ms:60000}")
    @Transactional
    public void reconcile() {
        LocalDate today = LocalDate.now(clock);
        List<Movie> released = movieRepository.findReleasedByStatusForUpdate(
                MovieStatus.UPCOMING, today);
        int started = 0;
        for (Movie movie : released) {
            if (!approvalPolicy.hasPublishedShowtime(movie.getId())) {
                continue;
            }
            movie.setStatus(MovieStatus.NOW_SHOWING);
            Movie saved = movieRepository.save(movie);
            historyService.record(saved, MovieStatus.UPCOMING, MovieStatus.NOW_SHOWING,
                    "Tự động bắt đầu khi đến ngày khai thác tại rạp", null);
            started++;
        }

        List<Movie> ended = movieRepository.findEndedByStatusForUpdate(
                MovieStatus.NOW_SHOWING, today);
        for (Movie movie : ended) {
            movie.setStatus(MovieStatus.ENDED);
            Movie saved = movieRepository.save(movie);
            historyService.record(saved, MovieStatus.NOW_SHOWING, MovieStatus.ENDED,
                    "Tự động kết thúc sau đợt khai thác tại rạp", null);
        }

        if (started > 0 || !ended.isEmpty()) {
            log.info("Movie lifecycle reconciled: nowShowing={}, ended={}", started, ended.size());
        }
    }
}
