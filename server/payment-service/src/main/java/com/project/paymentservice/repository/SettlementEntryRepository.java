package com.project.paymentservice.repository;

import com.project.paymentservice.entity.SettlementEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementEntryRepository extends JpaRepository<SettlementEntry, Long> {
    List<SettlementEntry> findByBatchIdOrderByIdAsc(Long batchId);
}
