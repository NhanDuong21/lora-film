package com.project.authservice.repository;

import com.project.authservice.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
