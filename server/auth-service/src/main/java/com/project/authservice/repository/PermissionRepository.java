package com.project.authservice.repository;

import com.project.authservice.entity.Permission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

	Optional<Permission> findByPermissionCode(String permissionCode);
}
