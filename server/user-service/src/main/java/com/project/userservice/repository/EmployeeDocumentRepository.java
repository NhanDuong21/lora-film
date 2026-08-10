package com.project.userservice.repository;

import com.project.userservice.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    @EntityGraph(attributePaths = "employee")
    List<EmployeeDocument> findByEmployeeAccountIdAndDeletedAtIsNullOrderByUploadedAtDesc(Long accountId);

    @EntityGraph(attributePaths = "employee")
    List<EmployeeDocument> findByEmployeeAccountIdOrderByUploadedAtDesc(Long accountId);

    @EntityGraph(attributePaths = "employee")
    Optional<EmployeeDocument> findByIdAndEmployeeAccountIdAndDeletedAtIsNull(Long id, Long accountId);
}
