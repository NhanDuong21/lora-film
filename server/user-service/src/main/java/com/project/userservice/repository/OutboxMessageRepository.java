package com.project.userservice.repository;

import com.project.userservice.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    @Query("""
            select o from OutboxMessage o
            where o.processed = false
              and (o.nextAttemptAt is null or o.nextAttemptAt <= :now)
            order by o.createdAt
            """)
    List<OutboxMessage> findReady(LocalDateTime now, org.springframework.data.domain.Pageable pageable);
}
