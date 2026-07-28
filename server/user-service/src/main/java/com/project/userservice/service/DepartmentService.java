package com.project.userservice.service;

import com.project.userservice.dto.request.DepartmentRequest;
import com.project.userservice.dto.response.DepartmentResponse;
import com.project.userservice.entity.Department;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.DepartmentRepository;
import com.project.userservice.repository.EmployeeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserAuditService auditService;

    public DepartmentService(DepartmentRepository repository, EmployeeRepository employeeRepository,
                             UserAuditService auditService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Cacheable("departments")
    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        return repository.findByIsDeletedFalseOrderByNameAsc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> search(String keyword, Pageable pageable) {
        return repository.findByIsDeletedFalseAndNameContainingIgnoreCase(keyword == null ? "" : keyword,
                        com.project.userservice.util.PageableUtils.sanitize(pageable,
                                java.util.Set.of("id", "code", "name", "createdAt", "updatedAt"),
                                "name", org.springframework.data.domain.Sort.Direction.ASC))
                .map(this::map);
    }

    @CacheEvict(value = "departments", allEntries = true)
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessException("Department code already exists", "USER_DEPARTMENT_DUPLICATE");
        }
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessException("Department name already exists", "USER_DEPARTMENT_DUPLICATE");
        }
        Department value = new Department();
        apply(value, request);
        value = repository.save(value);
        auditService.log("DEPARTMENT_CREATED", "DEPARTMENT", value.getId(), null);
        return map(value);
    }

    @CacheEvict(value = "departments", allEntries = true)
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department value = find(id);
        if (repository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new BusinessException("Department code already exists", "USER_DEPARTMENT_DUPLICATE");
        }
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new BusinessException("Department name already exists", "USER_DEPARTMENT_DUPLICATE");
        }
        apply(value, request);
        auditService.log("DEPARTMENT_UPDATED", "DEPARTMENT", id, null);
        return map(repository.save(value));
    }

    @CacheEvict(value = "departments", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Department value = find(id);
        if (employeeRepository.existsByDepartmentIdAndIsDeletedFalse(id)) {
            throw new BusinessException("Department is assigned to active employees", "USER_DEPARTMENT_IN_USE");
        }
        value.setDeleted(true);
        repository.save(value);
        auditService.log("DEPARTMENT_DELETED", "DEPARTMENT", id, null);
    }

    private Department find(Long id) {
        Department value = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Department not found", "USER_004"));
        if (value.isDeleted()) {
            throw new BusinessException("Department not found", "USER_004");
        }
        return value;
    }

    private void apply(Department value, DepartmentRequest request) {
        value.setCode(request.code().trim().toUpperCase());
        value.setName(request.name().trim());
        value.setDescription(request.description());
    }

    private DepartmentResponse map(Department value) {
        return new DepartmentResponse(value.getId(), value.getCode(), value.getName(), value.getDescription());
    }
}
