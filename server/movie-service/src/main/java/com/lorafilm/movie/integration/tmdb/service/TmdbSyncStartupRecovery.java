package com.lorafilm.movie.integration.tmdb.service;

import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TmdbSyncStartupRecovery implements ApplicationRunner {

    private static final String SYNC_TYPE_BULK = "TMDB_BULK_EXPORT";
    private static final Set<String> INTERRUPTED_STATUSES = Set.of("IN_PROGRESS", "STOPPING");

    private final TmdbSyncStateRepository syncStateRepository;

    public TmdbSyncStartupRecovery(TmdbSyncStateRepository syncStateRepository) {
        this.syncStateRepository = syncStateRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncStateRepository.findBySyncType(SYNC_TYPE_BULK).ifPresent(syncState -> {
            if (!INTERRUPTED_STATUSES.contains(syncState.getStatus())) {
                return;
            }
            syncState.setStatus("IDLE");
            syncState.setStatusMessage(
                    "Phiên nhập trước đã kết thúc khi Movie Service khởi động lại. Hệ thống đang sẵn sàng.");
            syncStateRepository.save(syncState);
        });
    }
}
