package com.project.scoreservice.repository;

import com.project.scoreservice.entity.MembershipTierHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipTierHistoryRepository extends JpaRepository<MembershipTierHistory, Long> {

    List<MembershipTierHistory> findByUserScore_UserIdOrderByCreatedAtDesc(Long userId);

    Page<MembershipTierHistory> findByUserScore_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<MembershipTierHistory> findByUserScore_UserIdOrderByCreatedAtAsc(Long userId);
}
