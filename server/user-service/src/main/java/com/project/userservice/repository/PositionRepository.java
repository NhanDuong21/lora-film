package com.project.userservice.repository;

import com.project.userservice.entity.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Page<Position> findByIsDeletedFalseAndTitleContainingIgnoreCase(String keyword, Pageable pageable);
    List<Position> findByIsDeletedFalseOrderByTitleAsc();
    boolean existsByDepartmentIdAndIsDeletedFalse(Long departmentId);
    long countByDepartmentIdAndIsDeletedFalse(Long departmentId);
}
