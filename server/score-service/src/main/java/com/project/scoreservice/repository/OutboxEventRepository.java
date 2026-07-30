package com.project.scoreservice.repository;

import com.project.scoreservice.entity.OutboxEvent;
import com.project.scoreservice.enumtype.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>, OutboxEventRepositoryCustom {
    Optional<OutboxEvent> findByEventId(String eventId);
    List<OutboxEvent> findByStatus(OutboxStatus status);
}
