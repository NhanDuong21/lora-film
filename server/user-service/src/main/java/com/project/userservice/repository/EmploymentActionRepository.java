package com.project.userservice.repository;

import com.project.userservice.entity.EmploymentAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentActionRepository extends JpaRepository<EmploymentAction, Long> {
    Page<EmploymentAction> findByEmployeeAccountId(Long employeeAccountId, Pageable pageable);
}
