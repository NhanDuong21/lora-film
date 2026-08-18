package com.project.authservice.repository;

import java.util.Optional;

import com.project.authservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.project.authservice.enums.AccountStatus;

public interface AccountRepository extends JpaRepository<Account, Long> {

	boolean existsByEmail(String email);

	Optional<Account> findByEmail(String email);

	@Query(value = """
			SELECT DISTINCT a FROM Account a
			LEFT JOIN a.roles r
			WHERE (:keyword IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
			  AND (:status IS NULL OR a.status = :status)
			  AND (:roleId IS NULL OR r.id = :roleId)
			  AND (:accountScope IS NULL
			       OR (:accountScope = 'INTERNAL' AND r.code IN ('ADMIN', 'MANAGER', 'EMPLOYEE', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'))
			       OR (:accountScope = 'CUSTOMER' AND r.code IN ('CUSTOMER', 'ROLE_CUSTOMER')))
			""",
			countQuery = """
			SELECT COUNT(DISTINCT a.id) FROM Account a
			LEFT JOIN a.roles r
			WHERE (:keyword IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
			  AND (:status IS NULL OR a.status = :status)
			  AND (:roleId IS NULL OR r.id = :roleId)
			  AND (:accountScope IS NULL
			       OR (:accountScope = 'INTERNAL' AND r.code IN ('ADMIN', 'MANAGER', 'EMPLOYEE', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'))
			       OR (:accountScope = 'CUSTOMER' AND r.code IN ('CUSTOMER', 'ROLE_CUSTOMER')))
			""")
	Page<Account> search(@Param("keyword") String keyword,
			@Param("status") AccountStatus status,
			@Param("roleId") Long roleId,
			@Param("accountScope") String accountScope,
			Pageable pageable);

	java.util.List<Account> findAllByRolesId(Long roleId);
	java.util.List<Account> findAllByAccessProfileId(Long accessProfileId);
	long countByAccessProfileId(Long accessProfileId);
}
