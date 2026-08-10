package com.project.authservice.repository;

import com.project.authservice.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    @Query("""
            select message from OutboxMessage message
            where message.processed = false
              and (message.nextAttemptAt is null or message.nextAttemptAt <= :now)
            order by message.createdAt asc
            """)
    List<OutboxMessage> findReady(@Param("now") LocalDateTime now, Pageable pageable);
}
