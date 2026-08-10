package com.project.userservice.repository;

import com.project.userservice.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByShiftId(Long shiftId);

    @Query("""
            select a from AttendanceRecord a
            where (:employeeId is null or a.employee.accountId = :employeeId)
              and a.shift.scheduledStart >= :from
              and a.shift.scheduledStart < :to
            """)
    Page<AttendanceRecord> search(@Param("employeeId") Long employeeId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  Pageable pageable);

    List<AttendanceRecord> findByEmployeeAccountIdInAndShiftScheduledStartGreaterThanEqualAndShiftScheduledStartLessThan(
            Collection<Long> employeeIds, LocalDateTime from, LocalDateTime to);
}
