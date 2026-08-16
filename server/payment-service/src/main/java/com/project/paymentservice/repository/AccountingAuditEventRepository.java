package com.project.paymentservice.repository;

import com.project.paymentservice.entity.AccountingAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingAuditEventRepository extends JpaRepository<AccountingAuditEvent, Long> {
    Page<AccountingAuditEvent> findByAggregateType(String aggregateType, Pageable pageable);
}
