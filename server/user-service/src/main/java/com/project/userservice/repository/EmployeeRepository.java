package com.project.userservice.repository;

import com.project.userservice.entity.Employee;
import com.project.userservice.enumtype.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import com.project.userservice.security.PiiCrypto;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    boolean existsByAccountIdAndIsDeletedFalse(Long accountId);
    boolean existsByDepartmentIdAndIsDeletedFalse(Long departmentId);
    boolean existsByPositionIdAndIsDeletedFalse(Long positionId);
    Page<Employee> findByIsDeletedFalse(Pageable pageable);
    long countByIsDeletedFalse();
    long countByStatusAndIsDeletedFalse(EmployeeStatus status);
    long countByIsDeletedFalseAndAccountIdNot(Long accountId);
    long countByStatusAndIsDeletedFalseAndAccountIdNot(EmployeeStatus status, Long accountId);
    long countByDepartmentIdAndIsDeletedFalse(Long departmentId);
    long countByPositionIdAndIsDeletedFalse(Long positionId);
    List<Employee> findByStatusAndIsDeletedFalse(EmployeeStatus status);
    List<Employee> findByCinemaPublicIdAndIsDeletedFalseOrderByEmployeeCodeAsc(String cinemaPublicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.accountId = :accountId")
    Optional<Employee> findByAccountIdForScheduling(@Param("accountId") Long accountId);

    @EntityGraph(attributePaths = {"department", "position"})
    @Query("""
            select e from Employee e, User u
            where e.accountId = u.accountId
              and e.isDeleted = false
              and (:status is null or e.status = :status)
              and (:departmentId is null or e.department.id = :departmentId)
              and (:positionId is null or e.position.id = :positionId)
               and (:cinemaPublicId is null
                   or (:cinemaPublicId = '__unassigned__' and e.cinemaPublicId is null)
                   or e.cinemaPublicId = :cinemaPublicId)
              and (:excludeAccountId is null or e.accountId <> :excludeAccountId)
              and (:keyword is null or :keyword = ''
                   or lower(e.employeeCode) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                   or (:keywordHash is not null and u.phoneHash = :keywordHash))
            """)
    Page<Employee> searchSecure(@Param("keyword") String keyword,
                                @Param("keywordHash") String keywordHash,
                                @Param("status") EmployeeStatus status,
                                @Param("departmentId") Long departmentId,
                                @Param("positionId") Long positionId,
                                @Param("cinemaPublicId") String cinemaPublicId,
                                @Param("excludeAccountId") Long excludeAccountId,
                                Pageable pageable);

    default Page<Employee> search(String keyword, EmployeeStatus status, Long departmentId,
                                  Long positionId, String cinemaPublicId, Long excludeAccountId, Pageable pageable) {
        String normalizedCinema = cinemaPublicId == null || cinemaPublicId.isBlank()
                ? null : cinemaPublicId.trim().toLowerCase(java.util.Locale.ROOT);
        return searchSecure(keyword, PiiCrypto.searchHash(keyword), status, departmentId, positionId,
                normalizedCinema, excludeAccountId, pageable);
    }
}
