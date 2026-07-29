package com.project.userservice.repository;

import com.project.userservice.entity.Employee;
import com.project.userservice.enumtype.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    boolean existsByDepartmentIdAndIsDeletedFalse(Long departmentId);
    boolean existsByPositionIdAndIsDeletedFalse(Long positionId);
    Page<Employee> findByIsDeletedFalse(Pageable pageable);
    long countByIsDeletedFalse();
    long countByStatusAndIsDeletedFalse(EmployeeStatus status);

    @EntityGraph(attributePaths = {"department", "position"})
    @Query("""
            select e from Employee e, User u
            where e.accountId = u.accountId
              and e.isDeleted = false
              and (:status is null or e.status = :status)
              and (:departmentId is null or e.department.id = :departmentId)
              and (:positionId is null or e.position.id = :positionId)
              and (:keyword is null or :keyword = ''
                   or lower(e.employeeCode) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or u.phoneNumber like concat('%', :keyword, '%'))
            """)
    Page<Employee> search(@Param("keyword") String keyword,
                          @Param("status") EmployeeStatus status,
                          @Param("departmentId") Long departmentId,
                          @Param("positionId") Long positionId,
                          Pageable pageable);
}
