package com.project.authservice.repository;

import com.project.authservice.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByAccountIdOrderByLoginTimeDesc(Long accountId);
    Page<LoginHistory> findByAccountIdOrderByLoginTimeDesc(Long accountId, Pageable pageable);
}
