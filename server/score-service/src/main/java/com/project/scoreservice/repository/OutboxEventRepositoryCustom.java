package com.project.scoreservice.repository;

import com.project.scoreservice.entity.OutboxEvent;
import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepositoryCustom {
    List<OutboxEvent> findAndClaimPendingEvents(LocalDateTime now, int batchSize);
}
