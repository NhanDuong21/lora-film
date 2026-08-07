package com.project.userservice.repository;

import com.project.userservice.entity.LeaveRequest;
import com.project.userservice.enumtype.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    @Query("""
            select l from LeaveRequest l
            where (:employeeId is null or l.employee.accountId = :employeeId)
              and (:status is null or l.status = :status)
              and l.startDate <= :to
              and l.endDate >= :from
            """)
    Page<LeaveRequest> search(@Param("employeeId") Long employeeId,
                              @Param("status") LeaveStatus status,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to,
                              Pageable pageable);

    @Query("""
            select l from LeaveRequest l
            where l.employee.accountId = :employeeId
              and l.status in :statuses
              and l.startDate <= :to
              and l.endDate >= :from
            """)
    List<LeaveRequest> findOverlaps(@Param("employeeId") Long employeeId,
                                    @Param("statuses") Collection<LeaveStatus> statuses,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    List<LeaveRequest> findByEmployeeAccountIdInAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Collection<Long> employeeIds, LeaveStatus status, LocalDate to, LocalDate from);
}
