package com.project.paymentservice.repository;

import com.project.paymentservice.entity.AccountingPeriod;
import com.project.paymentservice.enumtype.AccountingPeriodStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {
    Optional<AccountingPeriod> findByPublicId(String publicId);
    boolean existsByPeriodCodeAndScopeKey(String periodCode, String scopeKey);
    Page<AccountingPeriod> findByScopeKey(String scopeKey, Pageable pageable);
    long countByStatus(AccountingPeriodStatus status);
    long countByStatusAndScopeKey(AccountingPeriodStatus status, String scopeKey);
}
