package com.project.authservice.repository;

import com.project.authservice.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByRoleName(String roleName);

	Optional<Role> findByRoleNameIgnoreCase(String roleName);

	Optional<Role> findByCode(String code);

	boolean existsByCode(String code);

	boolean existsByRoleNameIgnoreCase(String roleName);
}
