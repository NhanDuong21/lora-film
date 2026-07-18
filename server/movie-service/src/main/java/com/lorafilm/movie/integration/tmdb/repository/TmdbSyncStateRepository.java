package com.lorafilm.movie.integration.tmdb.repository;

import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TmdbSyncStateRepository extends JpaRepository<TmdbSyncState, Long> {
    Optional<TmdbSyncState> findBySyncType(String syncType);
}
