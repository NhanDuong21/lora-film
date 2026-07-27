package com.project.userservice.repository;

import com.project.userservice.entity.Payroll;
import com.project.userservice.enumtype.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long>, JpaSpecificationExecutor<Payroll> {
    boolean existsByEmployeeAccountIdAndSalaryMonth(Long employeeId, LocalDate salaryMonth);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.position", "details"})
    @Query("select p from Payroll p where p.id = :id")
    Optional<Payroll> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.position"})
    Page<Payroll> findByEmployeeAccountId(Long employeeId, Pageable pageable);

    long countByStatus(PayrollStatus status);

    @Query("select coalesce(sum(p.totalSalary), 0) from Payroll p where p.status in :statuses")
    BigDecimal sumTotalByStatuses(@Param("statuses") java.util.Collection<PayrollStatus> statuses);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.position"})
    @Query("""
            select p from Payroll p
            where (:employeeId is null or p.employee.accountId = :employeeId)
              and (:status is null or p.status = :status)
              and (:month is null or p.salaryMonth = :month)
            """)
    Page<Payroll> search(@Param("employeeId") Long employeeId,
                         @Param("status") PayrollStatus status,
                         @Param("month") LocalDate month,
                         Pageable pageable);
}
