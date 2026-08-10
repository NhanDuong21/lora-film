package com.project.userservice.repository;

import com.project.userservice.entity.WorkShift;
import com.project.userservice.enumtype.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
    @Query("""
            select s from WorkShift s
            where (:employeeId is null or s.employee.accountId = :employeeId)
              and s.scheduledStart < :to
              and s.scheduledEnd > :from
            """)
    Page<WorkShift> search(@Param("employeeId") Long employeeId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           Pageable pageable);

    @Query("""
            select s from WorkShift s
            where s.employee.accountId = :employeeId
              and s.status <> com.project.userservice.enumtype.ShiftStatus.CANCELLED
              and s.scheduledStart < :end
              and s.scheduledEnd > :start
            """)
    List<WorkShift> findOverlaps(@Param("employeeId") Long employeeId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    List<WorkShift> findByEmployeeAccountIdInAndScheduledStartGreaterThanEqualAndScheduledStartLessThanAndStatusIn(
            Collection<Long> employeeIds, LocalDateTime from, LocalDateTime to,
            Collection<ShiftStatus> statuses);

    List<WorkShift> findByEmployeeAccountIdAndScheduledStartGreaterThanEqualAndScheduledStartLessThanAndStatusNot(
            Long employeeId, LocalDateTime from, LocalDateTime to, ShiftStatus status);

    @Query("""
            select s from WorkShift s
            where s.employee.accountId in :employeeIds
              and s.scheduledStart < :to
              and s.scheduledEnd > :from
            order by s.scheduledStart asc
            """)
    List<WorkShift> findForEmployees(@Param("employeeIds") Collection<Long> employeeIds,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);
}
