package com.project.authservice.repository;

import com.project.authservice.entity.AccessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessProfileRepository extends JpaRepository<AccessProfile, Long> {
    Optional<AccessProfile> findByCode(String code);
    List<AccessProfile> findAllByActiveTrueOrderByNameAsc();
}
