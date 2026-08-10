package com.project.paymentservice.repository;

import com.project.paymentservice.entity.CounterCashSession;
import com.project.paymentservice.enumtype.CounterCashSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CounterCashSessionRepository extends JpaRepository<CounterCashSession, Long> {
    Optional<CounterCashSession> findFirstByEmployeeAccountIdAndStatusOrderByOpenedAtDesc(
            Long employeeAccountId, CounterCashSessionStatus status);

    List<CounterCashSession> findTop10ByEmployeeAccountIdOrderByOpenedAtDesc(Long employeeAccountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CounterCashSession s where s.publicId = :publicId")
    Optional<CounterCashSession> findByPublicIdForUpdate(@Param("publicId") String publicId);
}
