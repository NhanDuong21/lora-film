package com.project.userservice.service;

import com.project.userservice.dto.request.PositionRequest;
import com.project.userservice.dto.response.PositionResponse;
import com.project.userservice.entity.Position;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.mapper.PositionMapper;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.PositionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PositionService {
    private final PositionRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserAuditService auditService;
    private final PositionMapper positionMapper;

    public PositionService(PositionRepository repository, EmployeeRepository employeeRepository,
                           UserAuditService auditService, PositionMapper positionMapper) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
        this.positionMapper = positionMapper;
    }

    @Cacheable("positions")
    @Transactional(readOnly = true)
    public List<PositionResponse> list() {
        return repository.findByIsDeletedFalseOrderByTitleAsc().stream()
                .map(positionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<PositionResponse> search(String keyword, Pageable pageable) {
        return repository.findByIsDeletedFalseAndTitleContainingIgnoreCase(keyword == null ? "" : keyword,
                        com.project.userservice.util.PageableUtils.sanitize(pageable,
                                java.util.Set.of("id", "code", "title", "createdAt", "updatedAt"),
                                "title", org.springframework.data.domain.Sort.Direction.ASC))
                .map(positionMapper::toResponse);
    }

    @CacheEvict(value = "positions", allEntries = true)
    @Transactional
    public PositionResponse create(PositionRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessException("Position code already exists", "USER_POSITION_DUPLICATE");
        }
        Position value = new Position();
        apply(value, request);
        value = repository.save(value);
        auditService.log("POSITION_CREATED", "POSITION", value.getId(), null);
        return positionMapper.toResponse(value);
    }

    @CacheEvict(value = "positions", allEntries = true)
    @Transactional
    public PositionResponse update(Long id, PositionRequest request) {
        Position value = find(id);
        if (repository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new BusinessException("Position code already exists", "USER_POSITION_DUPLICATE");
        }
        apply(value, request);
        auditService.log("POSITION_UPDATED", "POSITION", id, null);
        return positionMapper.toResponse(repository.save(value));
    }

    @CacheEvict(value = "positions", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Position value = find(id);
        if (employeeRepository.existsByPositionIdAndIsDeletedFalse(id)) {
            throw new BusinessException("Position is assigned to active employees", "USER_POSITION_IN_USE");
        }
        value.setDeleted(true);
        repository.save(value);
        auditService.log("POSITION_DELETED", "POSITION", id, null);
    }

    private Position find(Long id) {
        Position value = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Position not found", "USER_005"));
        if (value.isDeleted()) {
            throw new BusinessException("Position not found", "USER_005");
        }
        return value;
    }

    private void apply(Position value, PositionRequest request) {
        value.setCode(request.code().trim().toUpperCase());
        value.setTitle(request.name().trim());
        value.setDescription(request.description());
    }

}
